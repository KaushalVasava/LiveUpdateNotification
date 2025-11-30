package com.kaushal.app.livenotoficationupdate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kaushal.app.livenotoficationupdate.examples.fooddelivery.FoodDeliveryService
import com.kaushal.app.livenotoficationupdate.ui.theme.LiveNotoficationUpdateTheme

class MainActivity : ComponentActivity() {

    private lateinit var notificationManager: LiveUpdateNotificationManager
    private var hasNotificationPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager = LiveUpdateNotificationManager(this)
        checkNotificationPermission()
        
        enableEdgeToEdge()
        setContent {
            LiveNotoficationUpdateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiveUpdateDemoScreen(
                        hasPermission = hasNotificationPermission,
                        onRequestPermission = { requestNotificationPermission() },
                        onStartTimer = { startLiveUpdate(LiveUpdateService.ACTION_START_TIMER) },
                        onStartProgress = { startLiveUpdate(LiveUpdateService.ACTION_START_PROGRESS) },
                        onStartNavigation = { startLiveUpdate(LiveUpdateService.ACTION_START_NAVIGATION) },
                        onStartWorkout = { startLiveUpdate(LiveUpdateService.ACTION_START_WORKOUT) },
                        onStartFoodDelivery = { startFoodDelivery() },  // Use dedicated service
                        onOpenSettings = { openNotificationSettings() },
                        canPostPromoted = notificationManager.canPostPromotedNotifications()
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions automatically granted on older Android versions
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startLiveUpdate(action: String) {
        if (!hasNotificationPermission) {
            Toast.makeText(this, "Please grant notification permission first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, LiveUpdateService::class.java).apply {
            this.action = action
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val updateType = when (action) {
            LiveUpdateService.ACTION_START_TIMER -> "Timer"
            LiveUpdateService.ACTION_START_PROGRESS -> "Progress"
            LiveUpdateService.ACTION_START_NAVIGATION -> "Navigation"
            LiveUpdateService.ACTION_START_WORKOUT -> "Workout"
            else -> "Live Update"
        }
        
        Toast.makeText(this, "$updateType started", Toast.LENGTH_SHORT).show()
    }

    private fun startFoodDelivery() {
        if (!hasNotificationPermission) {
            Toast.makeText(this, "Please grant notification permission first", Toast.LENGTH_SHORT).show()
            return
        }

        // Start dedicated FoodDeliveryService
        val intent = Intent(this, FoodDeliveryService::class.java).apply {
            action = FoodDeliveryService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        Toast.makeText(this, "Food Delivery started", Toast.LENGTH_SHORT).show()
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        }
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdateDemoScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartTimer: () -> Unit,
    onStartProgress: () -> Unit,
    onStartNavigation: () -> Unit,
    onStartWorkout: () -> Unit,
    onStartFoodDelivery: () -> Unit,
    onOpenSettings: () -> Unit,
    canPostPromoted: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Updates Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Android Live Update Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Live Updates keep users informed about ongoing activities like timers, navigation, workouts, and downloads.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Status Card
            PermissionStatusCard(
                hasPermission = hasPermission,
                canPostPromoted = canPostPromoted,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Update Examples
            Text(
                text = "Try These Examples",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Food Delivery Example (NEW - ProgressStyle)
            LiveUpdateCard(
                title = "🍕 Food Delivery Tracking",
                description = "Track your order with ProgressStyle milestones (Android 16+)",
                icon = Icons.Default.ShoppingCart,
                onClick = onStartFoodDelivery,
                enabled = hasPermission,
                color = MaterialTheme.colorScheme.primaryContainer,
                isNew = true
            )

            // Timer Example
            LiveUpdateCard(
                title = "Timer / Stopwatch",
                description = "Perfect for workout timers, cooking, or any time-tracking activity",
                icon = Icons.Default.Star,
                onClick = onStartTimer,
                enabled = hasPermission,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            // Progress Example
            LiveUpdateCard(
                title = "Progress Tracker",
                description = "Track downloads, file transfers, or delivery progress",
                icon = Icons.Default.Refresh,
                onClick = onStartProgress,
                enabled = hasPermission,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )

            // Navigation Example
            LiveUpdateCard(
                title = "Navigation",
                description = "Turn-by-turn directions with ETA updates",
                icon = Icons.Default.Place,
                onClick = onStartNavigation,
                enabled = hasPermission,
                color = MaterialTheme.colorScheme.errorContainer
            )

            // Workout Example
            LiveUpdateCard(
                title = "Workout Tracking",
                description = "Monitor exercise duration, distance, and pace",
                icon = Icons.Default.FavoriteBorder,
                onClick = onStartWorkout,
                enabled = hasPermission,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "About Live Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Ongoing: Must have a clear start and end\n" +
                                "• User-initiated: Started by user actions\n" +
                                "• Time-sensitive: Requires active monitoring\n" +
                                "• Status chips: Show critical info at a glance",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    hasPermission: Boolean,
    canPostPromoted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasPermission) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasPermission) "Permission Granted" else "Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPermission) 
                            "You can now create Live Update notifications" 
                        else 
                            "Grant notification permission to continue",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Permission")
                }
            }

            if (hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HorizontalDivider()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (canPostPromoted) Icons.Default.Star else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
    Text(
                        text = if (canPostPromoted) 
                            "Live Updates enabled in settings" 
                        else 
                            "Enable Live Updates in settings for best experience",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!canPostPromoted) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdateCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    color: androidx.compose.ui.graphics.Color,
    isNew: Boolean = false
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (enabled) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isNew) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "NEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = if (enabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}