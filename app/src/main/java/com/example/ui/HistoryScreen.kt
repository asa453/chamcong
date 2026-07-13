package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimekeeperViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val records by viewModel.records.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()

    var selectedRecordForEdit by remember { mutableStateOf<AttendanceRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch Sử Chấm Công", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_btn")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            // Month Selector Card
            MonthSelectorCard(
                currentMonth = currentMonth,
                onMonthChanged = { viewModel.setMonth(it) }
            )

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventNote,
                            contentDescription = "No data",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "Chưa có dữ liệu chấm công tháng này",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Hãy bấm nút '+' hoặc đợi 7h30 sáng để chấm công.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(records, key = { it.dateString }) { record ->
                        RecordItemRow(
                            record = record,
                            isSunday = viewModel.isSunday(record.dateString),
                            onEditClick = { selectedRecordForEdit = record },
                            onDeleteClick = {
                                viewModel.deleteAttendance(record.dateString)
                                Toast.makeText(context, "Đã xóa bản ghi ${record.dateString}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    selectedRecordForEdit?.let { record ->
        EditAttendanceDialog(
            record = record,
            onDismiss = { selectedRecordForEdit = null },
            onConfirm = { status, hours, note ->
                viewModel.saveAttendance(record.dateString, status, hours, note)
                selectedRecordForEdit = null
                Toast.makeText(context, "Đã cập nhật bản ghi thành công!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun RecordItemRow(
    record: AttendanceRecord,
    isSunday: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val displayDate = remember(record.dateString) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOutput = SimpleDateFormat("dd/MM (EEEE)", Locale("vi", "VN"))
            val d = sdfInput.parse(record.dateString)
            if (d != null) sdfOutput.format(d) else record.dateString
        } catch (e: Exception) {
            record.dateString
        }
    }

    val (statusLabel, statusColor) = getStatusDetails(record.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayDate,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSunday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Badge(
                        containerColor = statusColor,
                        contentColor = Color.White
                    ) {
                        Text(statusLabel, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Render work hours / overtime hours
                if (record.status == "DI_LAM") {
                    Text(
                        text = "Số giờ làm: ${record.actualHours}h" +
                                (if (record.overtimeWeekday > 0) " (Tăng ca thường: ${record.overtimeWeekday}h)" else "") +
                                (if (record.overtimeSunday > 0) " (Tăng ca CN: ${record.overtimeSunday}h)" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (record.status == "NGHI_PHEP") {
                    Text(
                        text = "Hưởng nguyên lương ngày (LCB / 26)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Không tính công",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (record.note.isNotEmpty()) {
                    Text(
                        text = "Ghi chú: ${record.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, "Sửa", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun getStatusDetails(status: String): Pair<String, Color> {
    return when (status) {
        "DI_LAM" -> "Đi làm" to Color(0xFF1976D2) // standard blue
        "NGHI_TUAN" -> "Nghỉ tuần" to Color(0xFF757575) // gray
        "NGHI_CHO_VIEC" -> "Chờ việc" to Color(0xFFF57C00) // orange
        "NGHI_PHEP" -> "Nghỉ phép" to Color(0xFF388E3C) // green
        "NGHI_VIEC_RIENG" -> "Việc riêng (Trừ CC)" to Color(0xFFD32F2F) // red
        else -> status to Color.DarkGray
    }
}

@Composable
fun EditAttendanceDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var status by remember { mutableStateOf(record.status) }
    var actualHoursStr by remember { mutableStateOf(record.actualHours.toString()) }
    var note by remember { mutableStateOf(record.note) }

    val statusOptions = listOf(
        Triple("DI_LAM", "Đi làm hành chính", "Tính công chuẩn và overtime nếu >8h"),
        Triple("NGHI_TUAN", "Nghỉ tuần/Chủ Nhật", "Nghỉ có kế hoạch không lương"),
        Triple("NGHI_CHO_VIEC", "Nghỉ chờ việc", "Nghỉ do cty không có việc, không trừ CC"),
        Triple("NGHI_PHEP", "Nghỉ phép", "Hưởng 100% lương ngày, không trừ CC"),
        Triple("NGHI_VIEC_RIENG", "Nghỉ việc riêng", "Trừ 100% chuyên cần của tháng")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chỉnh sửa ngày ${record.dateString}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Radio buttons
                Text("Chọn Chế Độ Chấm Công:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                
                statusOptions.forEach { (option, label, sub) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { status = option }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = (status == option),
                            onClick = { status = option }
                        )
                        Column {
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // If "Đi làm", allow inputting hours
                if (status == "DI_LAM") {
                    OutlinedTextField(
                        value = actualHoursStr,
                        onValueChange = { actualHoursStr = it },
                        label = { Text("Tổng số giờ làm việc thực tế") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = actualHoursStr.toDoubleOrNull() ?: 8.0
                    onConfirm(status, hours, note)
                }
            ) {
                Text("Xác Nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
