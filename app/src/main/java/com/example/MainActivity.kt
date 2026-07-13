package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_TRIGGER_ATTENDANCE = "com.example.ACTION_TRIGGER_ATTENDANCE"
        const val EXTRA_TRIGGER_ATTENDANCE = "EXTRA_TRIGGER_ATTENDANCE"
        private const val TAG = "MainActivity"
    }

    private val viewModel: TimekeeperViewModel by viewModels()
    private var isForcedAlarmMode by mutableStateOf(false)

    // Screen navigation state
    private var currentScreen by mutableStateOf(Screen.DASHBOARD)

    // Request Notification permission for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted.")
        } else {
            Log.w(TAG, "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Check & Request necessary permissions on startup
        checkAndRequestPermissions()

        // 2. Schedule or verify that the 7:30 AM alarm is set
        viewModel.ensureAlarmScheduled()

        // 3. Process Intent that launched the activity
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoxWithNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val isAlarmTrigger = intent.getBooleanExtra(EXTRA_TRIGGER_ATTENDANCE, false) || 
                             intent.action == ACTION_TRIGGER_ATTENDANCE
        
        Log.d(TAG, "Handling intent: action=${intent.action}, isAlarmTrigger=$isAlarmTrigger")
        
        if (isAlarmTrigger) {
            isForcedAlarmMode = true
            viewModel.showOverlay()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch SCHEDULE_EXACT_ALARM settings", e)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch SYSTEM_ALERT_WINDOW settings", e)
                }
            }
        }
    }

    @Composable
    fun BoxWithNavigation() {
        if (viewModel.isOverlayShowing) {
            // Force the daily/manual attendance clock-in overlay
            AttendanceOverlay(
                viewModel = viewModel,
                isForcedAlarmMode = isForcedAlarmMode,
                dateString = viewModel.overlayDateString,
                onDismiss = {
                    viewModel.dismissOverlay()
                    isForcedAlarmMode = false
                }
            )
        } else {
            // Standard screen navigation
            Crossfade(
                targetState = currentScreen,
                label = "ScreenNavigation"
            ) { screen ->
                when (screen) {
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                            onNavigateToHistory = { currentScreen = Screen.HISTORY },
                            onOpenManualAttendance = { dateString ->
                                isForcedAlarmMode = false
                                viewModel.showOverlay(dateString)
                            }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                    Screen.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    DASHBOARD,
    SETTINGS,
    HISTORY
}
