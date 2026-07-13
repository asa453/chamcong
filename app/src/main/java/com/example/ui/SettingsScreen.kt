package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TimekeeperViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()

    // Form inputs initialized with current values
    var lcbStr by remember(currentSettings) { mutableStateOf(currentSettings.basicSalary.toLong().toString()) }
    var ccStr by remember(currentSettings) { mutableStateOf(currentSettings.attendanceBonus.toLong().toString()) }
    var standardDaysStr by remember(currentSettings) { mutableStateOf(currentSettings.standardDays.toString()) }
    var alarmHour by remember(currentSettings) { mutableStateOf(currentSettings.alarmHour) }
    var alarmMinute by remember(currentSettings) { mutableStateOf(currentSettings.alarmMinute) }

    // Overlay overlay authorization status
    var isOverlayPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    val scrollState = rememberScrollState()

    // Periodic check on overlay permission when screen resumes
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOverlayPermissionGranted = Settings.canDrawOverlays(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cấu Hình Hệ Thống", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_btn")
                    ) {
                        Icon(Icons.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. INPUT CONFIGS SECTION
            Text(
                text = "Thông số Lương & Chuyên cần",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    
                    // Basic salary
                    OutlinedTextField(
                        value = lcbStr,
                        onValueChange = { lcbStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Lương cơ bản (LCB) VND") },
                        leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_basic_salary"),
                        singleLine = true
                    )

                    // Attendance bonus
                    OutlinedTextField(
                        value = ccStr,
                        onValueChange = { ccStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Tiền chuyên cần (CC) VND") },
                        leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_attendance_bonus"),
                        singleLine = true
                    )

                    // Standard working days
                    OutlinedTextField(
                        value = standardDaysStr,
                        onValueChange = { standardDaysStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Số công quy chuẩn (mặc định: 26)") },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_standard_days"),
                        singleLine = true
                    )
                }
            }

            // 2. CONSTANTS INFO SECTION
            Text(
                text = "Hệ số tăng ca quy định (Cố định)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tăng ca ngày thường:", fontWeight = FontWeight.Medium)
                        Text("150% (Hệ số 1.5)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tăng ca Chủ Nhật:", fontWeight = FontWeight.Medium)
                        Text("200% (Hệ số 2.0)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 3. ALARM SETTINGS SECTION
            Text(
                text = "Báo Thức Chấm Công Tự Động (7h30)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Thiết lập giờ báo thức để ứng dụng tự động mở đè lên màn hình nhằm chấm công trực tiếp lúc ${alarmHour.toString().padStart(2, '0')}:${alarmMinute.toString().padStart(2, '0')}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Giờ: $alarmHour giờ", fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = alarmHour.toFloat(),
                                onValueChange = { alarmHour = it.toInt() },
                                valueRange = 0f..23f,
                                steps = 23,
                                modifier = Modifier.testTag("slider_hour")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phút: $alarmMinute phút", fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = alarmMinute.toFloat(),
                                onValueChange = { alarmMinute = it.toInt() },
                                valueRange = 0f..59f,
                                steps = 59,
                                modifier = Modifier.testTag("slider_minute")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Button(
                        onClick = {
                            viewModel.triggerTestAlarm()
                            Toast.makeText(context, "Báo thức thử nghiệm sẽ phát ra sau 5 giây! Vui lòng khóa màn hình hoặc tắt ứng dụng để chạy thử.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_alarm"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chạy Thử Báo Thức Chấm Công (5 giây)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. KIOSK / SYSTEM OVERLAY PERMISSION HELP
            Text(
                text = "Cấp Quyền Vẽ Trên Ứng Dụng Khác (Overlay)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverlayPermissionGranted) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Để ứng dụng có thể Tự động bật lên đè màn hình (Force Foreground) đúng giờ báo thức, Android yêu cầu bạn cấp quyền 'Hiển thị trên các ứng dụng khác'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isOverlayPermissionGranted) Icons.Filled.Verified else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (isOverlayPermissionGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isOverlayPermissionGranted) "Đã cấp quyền thành công" else "Chưa có quyền vẽ overlay",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (!isOverlayPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("grant_permission_btn")
                            ) {
                                Text("Cấp Quyền")
                            }
                        }
                    }
                }
            }

            // 5. SAVE ACTION BUTTON
            Button(
                onClick = {
                    val basicVal = lcbStr.toDoubleOrNull() ?: 6000000.0
                    val bonusVal = ccStr.toDoubleOrNull() ?: 500000.0
                    val standardVal = standardDaysStr.toIntOrNull() ?: 26

                    viewModel.saveSettings(
                        basicSalary = basicVal,
                        attendanceBonus = bonusVal,
                        standardDays = standardVal,
                        alarmHour = alarmHour,
                        alarmMinute = alarmMinute
                    )

                    Toast.makeText(context, "Đã lưu cài đặt và cập nhật báo thức!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu Cấu Hình", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
