package com.example.ui

import android.app.NotificationManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceOverlay(
    viewModel: TimekeeperViewModel,
    isForcedAlarmMode: Boolean, // True if opened via 7:30 AM alarm, False if opened via FAB
    dateString: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Date generation for today or custom date
    val todayDateString = remember(dateString) {
        dateString ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val todayDisplayString = remember(todayDateString) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOutput = SimpleDateFormat("dd/MM/yyyy (EEEE)", Locale("vi", "VN"))
            val d = sdfInput.parse(todayDateString)
            if (d != null) sdfOutput.format(d) else todayDateString
        } catch (e: Exception) {
            todayDateString
        }
    }

    // Collect records to find if a record already exists for this date
    val records by viewModel.records.collectAsState()
    val existingRecordForDate = remember(todayDateString, records) {
        records.find { it.dateString == todayDateString }
    }

    // State form (pre-populate with existing record if found)
    var selectedStatus by remember(existingRecordForDate) { 
        mutableStateOf(existingRecordForDate?.status ?: "DI_LAM") 
    }
    var actualHoursStr by remember(existingRecordForDate) { 
        mutableStateOf(existingRecordForDate?.actualHours?.toString() ?: "8.0") 
    }
    var note by remember(existingRecordForDate) { 
        mutableStateOf(existingRecordForDate?.note ?: "") 
    }

    // Lock Back button if in forced alarm mode!
    BackHandler(enabled = isForcedAlarmMode) {
        // Do absolutely nothing - Back is disabled!
    }

    val statusOptions = listOf(
        Triple("DI_LAM", "Đi làm hành chính (Mặc định)", "Sáng: 7h30-11h30, Chiều: 13h-17h (8 tiếng)"),
        Triple("NGHI_TUAN", "Ngày nghỉ (Tuần/Chủ Nhật)", "Nghỉ tuần có kế hoạch không tính lương"),
        Triple("NGHI_CHO_VIEC", "Nghỉ chờ việc", "Do công ty không có việc, không bị trừ chuyên cần"),
        Triple("NGHI_PHEP", "Nghỉ phép (Được hưởng lương)", "Hưởng 100% lương ngày đó, không trừ chuyên cần"),
        Triple("NGHI_VIEC_RIENG", "Nghỉ việc riêng", "Tự động trừ 100% tiền chuyên cần tháng này")
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_overlay_root"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Spacer(modifier = Modifier.height(24.dp))

            // Icon Reminder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AlarmOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Title
            Text(
                text = if (isForcedAlarmMode) "CHẤM CÔNG" else "ĐIỂM DANH CHẤM CÔNG",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (dateString != null) "Ngày chấm công: $todayDisplayString" else "Hôm nay: $todayDisplayString",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isForcedAlarmMode) {
                Text(
                    text = "🔒 Bạn cần chọn chế độ chấm công để có thể sử dụng điện thoại.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Radio Choices
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chọn trạng thái chấm công:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                statusOptions.forEach { (option, label, subtitle) ->
                    val isSelected = selectedStatus == option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = option }
                            .testTag("radio_option_$option"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedStatus = option }
                            )

                            Column {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Input working hours
            if (selectedStatus == "DI_LAM") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Thời gian làm việc hôm nay:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tổng giờ dôi ra sau 17h00 sẽ tự động tính tăng ca ngày thường (x1.5). " +
                                    "Nếu là Chủ Nhật, toàn bộ giờ được tính tăng ca Chủ Nhật (x2.0).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = actualHoursStr,
                            onValueChange = { actualHoursStr = it },
                            label = { Text("Tổng số giờ làm thực tế (mặc định: 8.0)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_actual_hours"),
                            singleLine = true
                        )
                    }
                }
            }

            // Optional comments/notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú ngày hôm nay (nếu có)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_note"),
                singleLine = true,
                maxLines = 2
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button only if NOT forced alarm mode!
                if (!isForcedAlarmMode) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_attendance_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy bỏ", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        val hours = actualHoursStr.toDoubleOrNull() ?: 8.0
                        
                        // 1. Save to Room database
                        viewModel.saveAttendance(
                            dateString = todayDateString,
                            status = selectedStatus,
                            actualHours = hours,
                            note = note
                        )

                        // 2. Mark evening submitted & cancel pending snooze checks or alarms
                        try {
                            val prefs = context.getSharedPreferences("timekeeper_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("evening_submitted_$todayDateString", true).apply()
                            com.example.alarm.AlarmHelper.cancelSnoozeAndCheck(context)
                        } catch (e: Exception) {
                            // ignore
                        }

                        // 3. Clear notification if launched from alarm
                        try {
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(4848)
                            notificationManager.cancel(4849)
                        } catch (e: Exception) {
                            // ignore
                        }

                        // 4. Callback dismissal
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("confirm_attendance_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Xác Nhận", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
