package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecord
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TimekeeperViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenManualAttendance: (String?) -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val summary by viewModel.salaryCalculation.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Generate list of all days in the current selected month paired with their database record (if any)
    val daysInMonth = remember(currentMonth, records) {
        val list = mutableListOf<Pair<String, AttendanceRecord?>>()
        try {
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdfMonth.parse(currentMonth)
            if (date != null) {
                val calendar = Calendar.getInstance().apply {
                    time = date
                }
                val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                for (i in 1..maxDays) {
                    calendar.set(Calendar.DAY_OF_MONTH, i)
                    val dateStr = sdfDay.format(calendar.time)
                    val record = records.find { it.dateString == dateStr }
                    list.add(dateStr to record)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        list
    }

    // Formatter for Currency
    val vndFormat = remember { DecimalFormat("#,###đ") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Chấm Công",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("action_history")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Lịch sử chấm công",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Cài đặt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenManualAttendance(null) },
                icon = { Icon(Icons.Filled.Add, "Chấm công") },
                text = { Text("Chấm Công Hôm Nay", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("fab_quick_attendance")
                    .padding(8.dp)
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
            
            // 1. Month Navigation Controller
            MonthSelectorCard(
                currentMonth = currentMonth,
                onMonthChanged = { viewModel.setMonth(it) }
            )

            // 2. HERO CARD: Accrued Real-Time Salary Summary
            SalaryHeroCard(
                totalSalary = summary.totalSalary,
                lcb = settings.basicSalary,
                formatter = vndFormat
            )

            // 3. STATS GRIDS: Days Breakdown
            Text(
                text = "Thống Kê Công Việc",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Ngày Đi Làm",
                    value = "${summary.daysWorked.toInt()} ngày",
                    subValue = "Thực tế hành chính",
                    icon = Icons.Filled.Work,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Nghỉ Phép (Hưởng Lương)",
                    value = "${summary.paidLeaveDays.toInt()} ngày",
                    subValue = vndFormat.format(summary.paidLeaveDays * (settings.basicSalary / settings.standardDays)),
                    icon = Icons.Filled.BeachAccess,
                    color = Color(0xFF2E7D32), // Custom green
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. OVERTIME STATS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueryBuilder,
                            contentDescription = "Tăng ca",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tăng Ca Lũy Kế",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Weekday OT
                    OvertimeRow(
                        label = "Ngày thường (x1.5)",
                        hours = summary.totalWeekdayOvertimeHours,
                        earnings = summary.weekdayOvertimePay,
                        formatter = vndFormat
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Sunday OT
                    OvertimeRow(
                        label = "Chủ Nhật (x2.0)",
                        hours = summary.totalSundayOvertimeHours,
                        earnings = summary.sundayOvertimePay,
                        formatter = vndFormat
                    )
                }
            }

            // 5. ATTENDANCE BONUS (CHUYÊN CẦN) BADGE & SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.isAttendanceBonusActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (summary.isAttendanceBonusActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (summary.isAttendanceBonusActive) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.Warning
                            },
                            contentDescription = "Chuyên cần status",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tiền Chuyên Cần",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (summary.isAttendanceBonusActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Text(
                            text = if (summary.isAttendanceBonusActive) {
                                "Đang đủ điều kiện nhận chuyên cần"
                            } else {
                                "Đã bị trừ do có nghỉ việc riêng trong tháng"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = vndFormat.format(summary.attendanceBonusValue),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (summary.isAttendanceBonusActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // 5b. WORK ALLOWANCES (PHỤ CẤP ĐI LÀM) CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = "Phụ cấp",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Phụ Cấp Đi Làm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Gasoline Allowance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tiền xăng xe (200k / 26 ngày)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ngày đi làm: ${summary.physicalDaysWorked.toInt()} ngày",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = vndFormat.format(summary.gasolineAllowance),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Meal Allowance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tiền ăn mỗi ngày (20k / ngày)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ngày đi làm: ${summary.physicalDaysWorked.toInt()} ngày",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = vndFormat.format(summary.mealAllowance),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 6. DETAILED CALENDAR / WORKDAYS SHEET
            Text(
                text = "Bảng Chấm Công Chi Tiết (Tháng ${currentMonth.split("-").let { if (it.size == 2) "${it[1]}/${it[0]}" else currentMonth }})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daysInMonth.forEach { (dateStr, record) ->
                    val isSunday = viewModel.isSunday(dateStr)
                    MonthlyDayRow(
                        dateString = dateStr,
                        record = record,
                        isSunday = isSunday,
                        onDeclareClick = { onOpenManualAttendance(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Extra padding for FAB
        }
    }
}

@Composable
fun MonthSelectorCard(
    currentMonth: String,
    onMonthChanged: (String) -> Unit
) {
    val sdfInput = remember { SimpleDateFormat("yyyy-MM", Locale.US) }
    val sdfOutput = remember { SimpleDateFormat("'Tháng' MM / yyyy", Locale( "vi", "VN")) }

    val formattedMonth = remember(currentMonth) {
        try {
            val date = sdfInput.parse(currentMonth)
            if (date != null) sdfOutput.format(date) else currentMonth
        } catch (e: Exception) {
            currentMonth
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    onMonthChanged(shiftMonth(currentMonth, -1))
                },
                modifier = Modifier.testTag("prev_month_btn")
            ) {
                Icon(Icons.Filled.ChevronLeft, "Tháng trước")
            }

            Text(
                text = formattedMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    onMonthChanged(shiftMonth(currentMonth, 1))
                },
                modifier = Modifier.testTag("next_month_btn")
            ) {
                Icon(Icons.Filled.ChevronRight, "Tháng sau")
            }
        }
    }
}

private fun shiftMonth(current: String, amount: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(current) ?: return current
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.MONTH, amount)
        }
        sdf.format(cal.time)
    } catch (e: Exception) {
        current
    }
}

@Composable
fun SalaryHeroCard(
    totalSalary: Double,
    lcb: Double,
    formatter: DecimalFormat
) {
    // Elegant gradient background card representing professional finance
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LƯƠNG THÁNG TÍCH LŨY",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = formatter.format(totalSalary),
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("hero_total_salary")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lương cơ bản: ${formatter.format(lcb)}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tính thời gian thực",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OvertimeRow(
    label: String,
    hours: Double,
    earnings: Double,
    formatter: DecimalFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Tổng số giờ: $hours giờ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatter.format(earnings),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MonthlyDayRow(
    dateString: String,
    record: AttendanceRecord?,
    isSunday: Boolean,
    onDeclareClick: (String) -> Unit
) {
    val displayDate = remember(dateString) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOutput = SimpleDateFormat("dd/MM (EEEE)", Locale("vi", "VN"))
            val d = sdfInput.parse(dateString)
            if (d != null) {
                var formatted = sdfOutput.format(d)
                formatted = formatted.replace("Thứ hai", "Thứ Hai")
                    .replace("Thứ ba", "Thứ Ba")
                    .replace("Thứ tư", "Thứ Tư")
                    .replace("Thứ năm", "Thứ Năm")
                    .replace("Thứ sáu", "Thứ Sáu")
                    .replace("Thứ bảy", "Thứ Bảy")
                formatted
            } else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    val isRegistered = record != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeclareClick(dateString) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRegistered) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        border = if (!isRegistered) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        fontWeight = if (isRegistered) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSunday) {
                            MaterialTheme.colorScheme.error
                        } else if (isRegistered) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    
                    if (isRegistered) {
                        val (statusLabel, statusColor) = getStatusDetails(record!!.status)
                        Badge(
                            containerColor = statusColor,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Chưa chấm",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (isRegistered && record != null) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = "Nhấp để khai báo chấm công ngày này",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (isRegistered) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Chỉnh sửa",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Khai báo",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
