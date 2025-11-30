package com.kaushal.app.livenotoficationupdate

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Foreground service to handle Live Update notifications
 * This service updates notifications periodically to reflect the current state
 */
class LiveUpdateService : Service() {

    private lateinit var notificationManager: LiveUpdateNotificationManager
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentUpdateType: UpdateType = UpdateType.NONE
    private var isRunning = false
    private var isPaused = false
    
    // Timer state
    private var timerStartTime = 0L
    private var timerElapsedTime = 0L
    
    // Progress state
    private var currentProgress = 0
    
    // Navigation state
    private var navigationSteps = listOf(
        "Head north on Main St",
        "Turn right onto Oak Ave",
        "Continue for 2 miles",
        "Turn left onto Elm St",
        "Destination will be on your right"
    )
    private var currentNavigationStep = 0
    private var navigationEta = 15
    

    enum class UpdateType {
        NONE, TIMER, PROGRESS, NAVIGATION, WORKOUT
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = LiveUpdateNotificationManager(this)
        Log.d(TAG, "LiveUpdateService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TIMER -> startTimer()
            ACTION_START_PROGRESS -> startProgress()
            ACTION_START_NAVIGATION -> startNavigation()
            ACTION_START_WORKOUT -> startWorkout()
            LiveUpdateNotificationManager.ACTION_STOP -> stopUpdates()
            LiveUpdateNotificationManager.ACTION_PAUSE -> togglePause()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }

        return START_STICKY
    }

    private fun startTimer() {
        Log.d(TAG, "Starting timer")
        currentUpdateType = UpdateType.TIMER
        isRunning = true
        isPaused = false
        timerStartTime = System.currentTimeMillis()
        timerElapsedTime = 0L

        // Start as foreground service
        val notification = notificationManager.createTimerNotification(
            "Workout Timer",
            timerElapsedTime,
            isPaused
        )
        startForeground(LiveUpdateNotificationManager.NOTIFICATION_ID_TIMER, notification)

        // Update every second
        scheduleTimerUpdate()
    }

    private fun scheduleTimerUpdate() {
        if (!isRunning || currentUpdateType != UpdateType.TIMER) return

        handler.postDelayed(@androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS) {
            if (!isPaused) {
                timerElapsedTime = System.currentTimeMillis() - timerStartTime
            }

            val notification = notificationManager.createTimerNotification(
                "Workout Timer",
                timerElapsedTime,
                isPaused
            )
            notificationManager.notify(
                LiveUpdateNotificationManager.NOTIFICATION_ID_TIMER,
                notification
            )

            scheduleTimerUpdate()
        }, 1000) // Update every second
    }

    private fun startProgress() {
        Log.d(TAG, "Starting progress")
        currentUpdateType = UpdateType.PROGRESS
        isRunning = true
        currentProgress = 0

        // Start as foreground service
        val notification = notificationManager.createProgressNotification(
            "Downloading File",
            "Preparing download...",
            currentProgress,
            100,
            "$currentProgress%"
        )
        startForeground(LiveUpdateNotificationManager.NOTIFICATION_ID_PROGRESS, notification)

        // Update progress
        scheduleProgressUpdate()
    }

    private fun scheduleProgressUpdate() {
        if (!isRunning || currentUpdateType != UpdateType.PROGRESS) return

        handler.postDelayed({
            currentProgress += 5
            
            if (currentProgress > 100) {
                // Complete
                val notification = notificationManager.createProgressNotification(
                    "Download Complete",
                    "File downloaded successfully",
                    100,
                    100,
                    "Done"
                )
                notificationManager.notify(
                    LiveUpdateNotificationManager.NOTIFICATION_ID_PROGRESS,
                    notification
                )
                
                // Auto-dismiss after 3 seconds
                handler.postDelayed({
                    stopUpdates()
                }, 3000)
            } else {
                val notification = notificationManager.createProgressNotification(
                    "Downloading File",
                    "Progress: $currentProgress%",
                    currentProgress,
                    100,
                    "$currentProgress%"
                )
                notificationManager.notify(
                    LiveUpdateNotificationManager.NOTIFICATION_ID_PROGRESS,
                    notification
                )
                scheduleProgressUpdate()
            }
        }, 500) // Update every 500ms
    }

    private fun startNavigation() {
        Log.d(TAG, "Starting navigation")
        currentUpdateType = UpdateType.NAVIGATION
        isRunning = true
        currentNavigationStep = 0
        navigationEta = 15

        // Start as foreground service
        val notification = notificationManager.createNavigationNotification(
            "Home",
            navigationSteps[currentNavigationStep],
            navigationEta
        )
        startForeground(LiveUpdateNotificationManager.NOTIFICATION_ID_NAVIGATION, notification)

        // Update navigation
        scheduleNavigationUpdate()
    }

    private fun scheduleNavigationUpdate() {
        if (!isRunning || currentUpdateType != UpdateType.NAVIGATION) return

        handler.postDelayed({
            // Decrease ETA
            navigationEta--
            
            // Move to next step every 5 ETA minutes
            if (navigationEta % 3 == 0 && currentNavigationStep < navigationSteps.size - 1) {
                currentNavigationStep++
            }

            if (navigationEta <= 0 || currentNavigationStep >= navigationSteps.size) {
                // Arrived
                notificationManager.cancel(LiveUpdateNotificationManager.NOTIFICATION_ID_NAVIGATION)
                stopUpdates()
            } else {
                val notification = notificationManager.createNavigationNotification(
                    "Home",
                    navigationSteps[currentNavigationStep],
                    navigationEta
                )
                notificationManager.notify(
                    LiveUpdateNotificationManager.NOTIFICATION_ID_NAVIGATION,
                    notification
                )
                scheduleNavigationUpdate()
            }
        }, 2000) // Update every 2 seconds for demo purposes
    }

    private fun startWorkout() {
        Log.d(TAG, "Starting workout")
        currentUpdateType = UpdateType.WORKOUT
        isRunning = true
        isPaused = false
        timerStartTime = System.currentTimeMillis()
        timerElapsedTime = 0L

        // Start as foreground service
        val notification = notificationManager.createWorkoutNotification(
            "Running",
            formatTime(timerElapsedTime),
            "Distance: 0.0 km • Pace: 0:00/km"
        )
        startForeground(LiveUpdateNotificationManager.NOTIFICATION_ID_WORKOUT, notification)

        // Update workout
        scheduleWorkoutUpdate()
    }

    private fun scheduleWorkoutUpdate() {
        if (!isRunning || currentUpdateType != UpdateType.WORKOUT) return

        handler.postDelayed({
            if (!isPaused) {
                timerElapsedTime = System.currentTimeMillis() - timerStartTime
            }

            // Simulate workout stats
            val distanceKm = (timerElapsedTime / 1000.0) / 60.0 * 0.2 // ~12 km/h pace
            val paceMinPerKm = if (distanceKm > 0) (timerElapsedTime / 1000.0 / 60.0) / distanceKm else 0.0

            val stats = String.format(
                "Distance: %.2f km • Pace: %d:%02d/km",
                distanceKm,
                paceMinPerKm.toInt(),
                ((paceMinPerKm - paceMinPerKm.toInt()) * 60).toInt()
            )

            val notification = notificationManager.createWorkoutNotification(
                "Running",
                formatTime(timerElapsedTime),
                stats
            )
            notificationManager.notify(
                LiveUpdateNotificationManager.NOTIFICATION_ID_WORKOUT,
                notification
            )

            scheduleWorkoutUpdate()
        }, 1000) // Update every second
    }

    private fun togglePause() {
        isPaused = !isPaused
        
        if (!isPaused && (currentUpdateType == UpdateType.TIMER || currentUpdateType == UpdateType.WORKOUT)) {
            // Reset start time when resuming
            timerStartTime = System.currentTimeMillis() - timerElapsedTime
        }

        // Update notification immediately
        when (currentUpdateType) {
            UpdateType.TIMER -> {
                val notification = notificationManager.createTimerNotification(
                    "Workout Timer",
                    timerElapsedTime,
                    isPaused
                )
                notificationManager.notify(
                    LiveUpdateNotificationManager.NOTIFICATION_ID_TIMER,
                    notification
                )
            }
            UpdateType.WORKOUT -> {
                val distanceKm = (timerElapsedTime / 1000.0) / 60.0 * 0.2
                val paceMinPerKm = if (distanceKm > 0) (timerElapsedTime / 1000.0 / 60.0) / distanceKm else 0.0
                val stats = String.format(
                    "Distance: %.2f km • Pace: %d:%02d/km",
                    distanceKm,
                    paceMinPerKm.toInt(),
                    ((paceMinPerKm - paceMinPerKm.toInt()) * 60).toInt()
                )

                val notification = notificationManager.createWorkoutNotification(
                    "Running",
                    formatTime(timerElapsedTime),
                    stats
                )
                notificationManager.notify(
                    LiveUpdateNotificationManager.NOTIFICATION_ID_WORKOUT,
                    notification
                )
            }
            else -> {}
        }

        Log.d(TAG, "Paused: $isPaused")
    }

    private fun stopUpdates() {
        Log.d(TAG, "Stopping updates")
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        
        // Cancel all notifications
        notificationManager.cancelAll()
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "LiveUpdateService destroyed")
    }

    companion object {
        private const val TAG = "LiveUpdateService"
        
        const val ACTION_START_TIMER = "com.kaushal.app.livenotoficationupdate.START_TIMER"
        const val ACTION_START_PROGRESS = "com.kaushal.app.livenotoficationupdate.START_PROGRESS"
        const val ACTION_START_NAVIGATION = "com.kaushal.app.livenotoficationupdate.START_NAVIGATION"
        const val ACTION_START_WORKOUT = "com.kaushal.app.livenotoficationupdate.START_WORKOUT"
    }
}

