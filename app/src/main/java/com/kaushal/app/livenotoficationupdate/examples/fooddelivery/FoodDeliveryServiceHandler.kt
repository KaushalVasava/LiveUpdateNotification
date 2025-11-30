package com.kaushal.app.livenotoficationupdate.examples.fooddelivery

import android.app.Notification
import android.app.Service
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kaushal.app.livenotoficationupdate.LiveUpdateNotificationManager

/**
 * Service handler for food delivery tracking
 * Manages the lifecycle of food delivery Live Update notifications
 * 
 * This class encapsulates all food delivery service logic, making it
 * fully modular and reusable in other projects.
 */
class FoodDeliveryServiceHandler(
    private val service: Service,
    private val notificationManager: LiveUpdateNotificationManager,
    private val onComplete: () -> Unit = {}  // Callback when delivery completes
) {
    private var foodDeliveryTracker: FoodDeliveryTracker? = null
    private var isActive = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "FoodDeliveryHandler"
        private const val COMPLETION_DISPLAY_DURATION = 3000L // Show completion for 3 seconds
    }

    /**
     * Start food delivery tracking
     * @return Initial notification to start foreground service with
     */
    fun start(): Notification {
        Log.d(TAG, "Starting food delivery tracking")
        isActive = true

        // Initialize the tracker with callbacks
        foodDeliveryTracker = FoodDeliveryTracker(
            onStateChanged = { state -> updateNotification(state) },
            onCompleted = { complete() }
        )

        // Create initial notification
        val firstState = OrderState.entries[0]
        val notification = createNotification(firstState)

        // Start the tracker
        foodDeliveryTracker?.startTracking()

        return notification
    }

    /**
     * Stop food delivery tracking
     */
    fun stop() {
        Log.d(TAG, "Stopping food delivery tracking")
        isActive = false
        foodDeliveryTracker?.stopTracking()
        foodDeliveryTracker = null
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Update notification with new state
     */
    private fun updateNotification(state: OrderState) {
        if (!isActive) return

        val notification = createNotification(state)
        notificationManager.notify(
            LiveUpdateNotificationManager.NOTIFICATION_ID_FOOD_DELIVERY,
            notification
        )
    }

    /**
     * Create notification for given order state
     */
    private fun createNotification(state: OrderState): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            FoodDeliveryNotificationHelper.createProgressStyleNotification(
                service,
                LiveUpdateNotificationManager.CHANNEL_ID,
                state.orderStatus,
                state.statusText,
                state.progress
            )
        } else {
            FoodDeliveryNotificationHelper.createCompatNotification(
                service,
                LiveUpdateNotificationManager.CHANNEL_ID,
                state.orderStatus,
                state.statusText,
                state.progress
            )
        }
    }

    /**
     * Handle completion callback
     * Shows the completion state briefly, then dismisses notification and stops service
     */
    private fun complete() {
        Log.d(TAG, "Food delivery tracking completed - will dismiss notification after ${COMPLETION_DISPLAY_DURATION}ms")
        
        // Stop tracker but keep notification visible for a moment
        isActive = false
        foodDeliveryTracker?.stopTracking()
        foodDeliveryTracker = null
        
        // Schedule notification dismissal and service stop
        handler.postDelayed({
            Log.d(TAG, "Dismissing food delivery notification")
            // Cancel the notification
            notificationManager.cancel(LiveUpdateNotificationManager.NOTIFICATION_ID_FOOD_DELIVERY)
            // Notify service to stop
            onComplete()
        }, COMPLETION_DISPLAY_DURATION)
    }
}

