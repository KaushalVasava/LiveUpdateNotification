package com.kaushal.app.livenotoficationupdate.examples.fooddelivery

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.kaushal.app.livenotoficationupdate.LiveUpdateNotificationManager
import com.kaushal.app.livenotoficationupdate.LiveUpdateService
import com.kaushal.app.livenotoficationupdate.MainActivity

/**
 * Helper class to create food delivery specific notifications
 * Demonstrates ProgressStyle API (Android 16+) and backward compatibility
 * 
 * Status Chips:
 * - Allow users to track Live Updates when notification is not in view
 * - Show critical information at a glance in status bar
 * - Best practice: Keep text under 7 characters for full display
 * - Source: https://developer.android.com/develop/ui/views/notifications/live-update#status-chips
 * 
 * IMPORTANT: If status chips don't appear on Android 14+:
 * 1. Check if Live Updates are enabled: Settings → Apps → [App] → Notifications → Live Updates
 * 2. Notification must be COLLAPSED (swiped up) to see chip in status bar
 * 3. Check logs for debug output about promotion status
 */
object FoodDeliveryNotificationHelper {

    /**
     * Check if the app can post promoted (Live Update) notifications
     * Call this to diagnose why status chips might not appear
     */
    fun canPostPromotedNotifications(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val canPost = notificationManager.canPostPromotedNotifications()
            android.util.Log.d("FoodDelivery", "Can post promoted notifications: $canPost")
            if (!canPost) {
                android.util.Log.w("FoodDelivery", "Live Updates are DISABLED in settings!")
                android.util.Log.w("FoodDelivery", "Enable: Settings → Apps → Notifications → Live Updates")
            }
            canPost
        } else {
            android.util.Log.w("FoodDelivery", "Android version ${Build.VERSION.SDK_INT} doesn't support canPostPromotedNotifications()")
            true // Assume supported on older versions
        }
    }

    /**
     * Create a food delivery Live Update notification with ProgressStyle (Android 16+)
     * Uses new ProgressStyle API with segments and milestone points
     * Includes status chip with current order state
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    fun createProgressStyleNotification(
        context: Context,
        channelId: String,
        orderStatus: String,
        statusText: String,
        progressValue: Int
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = LiveUpdateNotificationManager.ACTION_STOP
        }
        val cancelPendingIntent = PendingIntent.getService(
            context, 0, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create ProgressStyle with colored segments and milestone points
        val pointColor = android.graphics.Color.parseColor("#ECB7FF") // Light purple
        val segmentColor = android.graphics.Color.parseColor("#86F7FA") // Light cyan
        
        val progressStyle = Notification.ProgressStyle()
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(25).setColor(segmentColor),
                    Notification.ProgressStyle.Segment(25).setColor(segmentColor),
                    Notification.ProgressStyle.Segment(25).setColor(segmentColor),
                    Notification.ProgressStyle.Segment(25).setColor(segmentColor)
                )
            )

        // Add progress points based on current progress (milestones)
        val points = mutableListOf<Notification.ProgressStyle.Point>()
        if (progressValue >= 25) points.add(Notification.ProgressStyle.Point(25).setColor(pointColor))
        if (progressValue >= 50) points.add(Notification.ProgressStyle.Point(50).setColor(pointColor))
        if (progressValue >= 75) points.add(Notification.ProgressStyle.Point(75).setColor(pointColor))
        if (points.isNotEmpty()) {
            progressStyle.setProgressPoints(points)
        }

        progressStyle.setProgress(progressValue)

        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentTitle("🍕 Food Delivery Order")
            .setContentText(orderStatus)
            .setContentIntent(mainPendingIntent)
            .setStyle(progressStyle)
            // Live Update requirements
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Status chip: Shows critical info at a glance
            // Appears in status bar when notification is collapsed
            // Best practice: Keep under 7 characters for full visibility
            .setShortCriticalText(statusText) // E.g., "Placing", "On way", "Arrived"
            // Request promotion using extras (alternative method)
            .setExtras(android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
            })
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel Order",
                cancelPendingIntent
            )
            .build()
        
        // Set FLAG_ONGOING_EVENT to request Live Update promotion
        // This is REQUIRED for status chips to appear
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        
        // Debug logging (remove in production)
        android.util.Log.d("FoodDelivery", "Status chip text: '$statusText' (${statusText.length} chars)")
        android.util.Log.d("FoodDelivery", "Notification flags: ${notification.flags}")
        android.util.Log.d("FoodDelivery", "Is promoted: ${(notification.flags and Notification.FLAG_PROMOTED_ONGOING) != 0}")
        
        return notification
    }

    /**
     * Create a food delivery notification with standard progress bar
     * For devices running Android 15 and below
     * Includes status chip support for Android 14+ (API 34+)
     */
    fun createCompatNotification(
        context: Context,
        channelId: String,
        orderStatus: String,
        statusText: String,
        progress: Int
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = LiveUpdateNotificationManager.ACTION_STOP
        }
        val cancelPendingIntent = PendingIntent.getService(
            context, 0, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentTitle("🍕 Food Delivery Order")
            .setContentText(orderStatus)
            .setSubText(statusText)  // Fallback for older devices
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, false)
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel Order",
                cancelPendingIntent
            )

        // Add status chip for Android 14+ (API 34+)
        // Status chip shows critical info at a glance in the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setShortcutId(statusText)  // Status chip text (max 7 chars looks best)
        }

        val notification = builder.build()
        
        // Request promotion to Live Update (required for status chips on Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT
        }
        
        return notification
    }
}

