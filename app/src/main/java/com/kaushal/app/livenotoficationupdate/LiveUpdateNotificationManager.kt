package com.kaushal.app.livenotoficationupdate

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Manager class to handle Live Update notifications
 * Following Android's Live Update guidelines:
 * https://developer.android.com/develop/ui/views/notifications/live-update
 */
class LiveUpdateNotificationManager(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "live_updates_channel"
        const val CHANNEL_NAME = "Live Updates"
        
        // Notification IDs
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_PROGRESS = 1002
        const val NOTIFICATION_ID_NAVIGATION = 1003
        const val NOTIFICATION_ID_WORKOUT = 1004
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
     * Create a timer/stopwatch Live Update notification
     * Use case: Workout timer, cooking timer, etc.
     */
    fun createTimerNotification(
        title: String,
        elapsedTimeMillis: Long,
        isPaused: Boolean = false
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            context, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Calculate the base time for chronometer
        val baseTime = SystemClock.elapsedRealtime() - elapsedTimeMillis

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(if (isPaused) "Paused" else "Running")
            .setContentIntent(mainPendingIntent)
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true) // Required for Live Updates
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(!isPaused)
            .setWhen(baseTime)
            .setChronometerCountDown(false)
            // Request promotion to Live Update
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play 
                else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build().apply {
                // Request promotion using the flag
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    flags = flags or Notification.FLAG_ONGOING_EVENT
                }
            }
    }

    /**
     * Create a progress-based Live Update notification
     * Use case: File download, food delivery tracking, etc.
     */
    fun createProgressNotification(
        title: String,
        contentText: String,
        progress: Int,
        maxProgress: Int = 100,
        statusText: String? = null
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_STOP
        }
        val cancelPendingIntent = PendingIntent.getService(
            context, 0, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(mainPendingIntent)
            .setProgress(maxProgress, progress, false)
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )

        // Add status chip text if provided
        if (statusText != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setShortcutId(statusText)
        }

        return builder.build().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                flags = flags or Notification.FLAG_ONGOING_EVENT
            }
        }
    }

    /**
     * Create a navigation Live Update notification
     * Use case: Turn-by-turn navigation
     */
    fun createNavigationNotification(
        destination: String,
        currentInstruction: String,
        etaMinutes: Int
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("To $destination")
            .setContentText(currentInstruction)
            .setContentIntent(mainPendingIntent)
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Navigation",
                stopPendingIntent
            )

        // Set ETA in status chip
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use setShortCutId for status chip
            builder.setShortcutId("$etaMinutes min")
        }

        return builder.build().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                flags = flags or Notification.FLAG_ONGOING_EVENT
            }
        }
    }

    /**
     * Create a workout Live Update notification
     * Use case: Exercise tracking, running timer
     */
    fun createWorkoutNotification(
        workoutType: String,
        duration: String,
        stats: String
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(context, LiveUpdateService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            context, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("$workoutType Workout")
            .setContentText(stats)
            .setSubText(duration)
            .setContentIntent(mainPendingIntent)
            // Live Update requirements
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Workout",
                stopPendingIntent
            )
            .build().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    flags = flags or Notification.FLAG_ONGOING_EVENT
                }
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
     * Cancel all notifications
     */
    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

