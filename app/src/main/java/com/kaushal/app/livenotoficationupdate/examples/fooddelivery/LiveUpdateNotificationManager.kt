package com.kaushal.app.livenotoficationupdate.examples.fooddelivery

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.kaushal.app.livenotoficationupdate.MainActivity
import androidx.core.graphics.toColorInt

/**
 * Manager class to handle Live Update notifications
 * Following Android's Live Update guidelines:
 * https://developer.android.com/develop/ui/views/notifications/live-update
 */
class LiveUpdateNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "LiveUpdateNotifMgr"
        
        const val CHANNEL_ID = "live_updates_channel"
        const val CHANNEL_NAME = "Live Updates"

        const val NOTIFICATION_ID_FOOD_DELIVERY = 1005

        // Actions
        const val ACTION_STOP = "com.kaushal.app.livenotoficationupdate.STOP"
        const val ACTION_PAUSE = "com.kaushal.app.livenotoficationupdate.PAUSE"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    /**
     * Create notification channel with appropriate importance level
     * Note: Channel must NOT have IMPORTANCE_MIN for Live Updates
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // Live Updates require HIGH or DEFAULT importance
        ).apply {
            description = "Notifications for ongoing activities with live updates"
            setShowBadge(false)
            enableLights(true)
            enableVibration(false) // Usually don't vibrate for updates
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Check if the app can post promoted notifications
     */
    fun canPostPromotedNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            notificationManager.canPostPromotedNotifications()
        } else {
            true // Assume true for older versions
        }
    }

    /**
     * Post or update a notification
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notify(notificationId: Int, notification: Notification) {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Cancel a notification
     */
    fun cancel(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }


    /**
     * Create a food delivery Live Update notification with ProgressStyle (Android 16+)
     * Uses new ProgressStyle API with segments and milestone points
     * Includes status chip with current order state
     * 
     * Status Chips:
     * - Show critical information at a glance in status bar
     * - Best practice: Keep text under 7 characters for full display
     * - Source: https://developer.android.com/develop/ui/views/notifications/live-update#status-chips
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun createFoodDeliveryNotification(
        orderStatus: String,
        statusText: String,
        progressValue: Int
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create ProgressStyle with colored segments and milestone points
        val pointColor = "#ECB7FF".toColorInt() // Light purple
        val segmentColor = "#86F7FA".toColorInt() // Light cyan
        
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(
                listOf(
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor),
                    NotificationCompat.ProgressStyle.Segment(25).setColor(segmentColor)
                )
            )

        // Add progress points based on current progress (milestones)
        val points = mutableListOf<NotificationCompat.ProgressStyle.Point>()
        if (progressValue >= 25) points.add(NotificationCompat.ProgressStyle.Point(25).setColor(pointColor))
        if (progressValue >= 50) points.add(NotificationCompat.ProgressStyle.Point(50).setColor(pointColor))
        if (progressValue >= 75) points.add(NotificationCompat.ProgressStyle.Point(75).setColor(pointColor))
        if (points.isNotEmpty()) {
            progressStyle.setProgressPoints(points)
        }

        // Handle indeterminate progress for initial state
        if (progressValue == 0) {
            progressStyle.setProgressIndeterminate(true)
        } else {
            progressStyle.setProgress(progressValue)
        }

        // Use IconCompat for better compatibility
        val smallIcon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_sort_by_size)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("🍕 Food Delivery Order")
            .setContentText(orderStatus)
            .setContentIntent(mainPendingIntent)
            .setStyle(progressStyle)
            .setRequestPromotedOngoing(true)
            // CRITICAL: Request promotion to Live Update
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Status chip: Shows critical info at a glance in status bar
            // FIXED: Use dynamic statusText instead of hardcoded "Food"
            .setShortCriticalText(statusText)

        // Add conditional actions based on delivery progress (like AOSP example)
        when {
            progressValue >= 90 && progressValue < 100 -> {
                // Food arriving - add acknowledgment actions
                builder.addAction(
                    NotificationCompat.Action.Builder(null, "Got it", null).build()
                )
            }
            progressValue >= 100 -> {
                // Order complete - add rating action
                builder.addAction(
                    NotificationCompat.Action.Builder(null, "Rate delivery", null).build()
                )
            }
        }

        val notification = builder.build()
        
        // Mark as ongoing event for promotion to Live Update
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        return notification
    }

    /**
     * Create a food delivery notification with standard progress bar
     * For devices running Android 15 and below
     * Includes status chip support for Android 14+ (API 34+)
     */
    fun createFoodDeliveryNotificationCompat(
        orderStatus: String,
        statusText: String,
        progress: Int
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, FoodDeliveryService::class.java).apply {
            action = ACTION_STOP
        }
        val cancelPendingIntent = PendingIntent.getService(
            context, 0, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use IconCompat for better compatibility
        val smallIcon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_sort_by_size)
        val cancelIcon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("🍕 Food Delivery Order")
            .setContentText(orderStatus)
            .setSubText(statusText)
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, progress == 0)  // Indeterminate if progress is 0
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // Add conditional actions based on delivery progress
        when {
            progress < 90 -> {
                // Show cancel option before delivery arrives
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        cancelIcon,
                        "Cancel Order",
                        cancelPendingIntent
                    ).build()
                )
            }
            progress >= 90 && progress < 100 -> {
                // Food arriving - add acknowledgment action
                builder.addAction(
                    NotificationCompat.Action.Builder(null, "Got it", null).build()
                )
            }
            progress >= 100 -> {
                // Order complete - add rating action
                builder.addAction(
                    NotificationCompat.Action.Builder(null, "Rate delivery", null).build()
                )
            }
        }

        val notification = builder.build()
        
        // Request promotion to Live Update
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        }

        return notification
    }
}

