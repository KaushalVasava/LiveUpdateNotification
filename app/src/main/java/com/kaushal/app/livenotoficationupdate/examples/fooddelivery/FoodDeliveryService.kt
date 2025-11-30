package com.kaushal.app.livenotoficationupdate.examples.fooddelivery

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Dedicated foreground service for food delivery tracking
 * 
 * This is a completely standalone service for the food delivery module.
 * It can be copied to other projects along with the rest of the fooddelivery package.
 * 
 * Features:
 * - Self-contained (no dependencies on other services)
 * - Manages food delivery Live Update notifications
 * - Handles foreground service lifecycle
 * - Auto-stops after delivery completion
 */
class FoodDeliveryService : Service() {

    private lateinit var notificationManager: LiveUpdateNotificationManager
    private lateinit var deliveryHandler: FoodDeliveryServiceHandler
    private var isRunning = false

    companion object {
        private const val TAG = "FoodDeliveryService"
        // Action to start food delivery tracking
        const val ACTION_START = "com.kaushal.app.livenotoficationupdate.fooddelivery.START"
        const val ACTION_STOP = "com.kaushal.app.livenotoficationupdate.fooddelivery.STOP"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize notification manager
        notificationManager = LiveUpdateNotificationManager(this)
        
        // Initialize delivery handler with completion callback
        deliveryHandler = FoodDeliveryServiceHandler(
            service = this,
            notificationManager = notificationManager,
            onComplete = {
                stopDelivery()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> startDelivery()
            ACTION_STOP -> stopDelivery()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }

        return START_NOT_STICKY  // Don't restart if killed
    }

    /**
     * Start food delivery tracking
     */
    private fun startDelivery() {
        if (isRunning) {
            return
        }

        // Check if Live Updates are enabled (for debugging status chips)
        val canPostPromoted = notificationManager.canPostPromotedNotifications()
        Log.d(TAG, "Can post promoted notifications: $canPostPromoted")
        
        isRunning = true

        // Get initial notification from handler
        val notification = deliveryHandler.start()

        // Check if notification has promotable characteristics (Android 16+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val hasPromotable = notification.hasPromotableCharacteristics()
            Log.d(TAG, "Initial notification hasPromotableCharacteristics: $hasPromotable")
        }

        // Start as foreground service (REQUIRED for continuous tracking)
        startForeground(
            LiveUpdateNotificationManager.NOTIFICATION_ID_FOOD_DELIVERY,
            notification
        )

    }

    /**
     * Stop food delivery tracking
     */
    private fun stopDelivery() {
        if (!isRunning) {
            return
        }

        isRunning = false

        // Stop the handler
        deliveryHandler.stop()

        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // Stop the service
        stopSelf()

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        // Ensure handler is stopped
        if (isRunning) {
            deliveryHandler.stop()
        }
    }
}

