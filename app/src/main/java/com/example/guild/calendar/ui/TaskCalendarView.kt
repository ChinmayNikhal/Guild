// TaskCalendarView.kt
package com.example.guild.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun TaskCalendarView(
    onDateSelected: (year: Int, month: Int, dayOfMonth: Int) -> Unit,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int
) {
    val calendar = remember { Calendar.getInstance() }
    val currentYear = remember { mutableStateOf(initialYear) }
    val currentMonth = remember { mutableStateOf(initialMonth) }

    Column(modifier = Modifier.fillMaxWidth()) {
        MonthHeader(
            currentYear = currentYear.value,
            currentMonth = currentMonth.value,
            onPreviousMonth = {
                if (currentMonth.value == 1) {
                    currentYear.value--
                    currentMonth.value = 12
                } else {
                    currentMonth.value--
                }
            },
            onNextMonth = {
                if (currentMonth.value == 12) {
                    currentYear.value++
                    currentMonth.value = 1
                } else {
                    currentMonth.value++
                }
            }
        )
        DaysOfWeekHeaderAlternative()
        CalendarGridAlternative(
            year = currentYear.value,
            month = currentMonth.value,
            selectedDay = initialDay,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun MonthHeader(
    currentYear: Int,
    currentMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = DateFormatSymbols(Locale.getDefault()).months[currentMonth - 1]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPreviousMonth) { Text("<") }
        Text(
            text = "$monthName $currentYear",
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onNextMonth) { Text(">") }
    }
}

@Composable
fun DaysOfWeekHeaderAlternative() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = DateFormatSymbols(Locale.getDefault()).shortWeekdays.drop(1)
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f), // Keeping weight here for even distribution
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = DateFormatSymbols(Locale.getDefault()).shortWeekdays[1],
            modifier = Modifier.weight(1f), // Keeping weight here for even distribution
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CalendarGridAlternative(
    year: Int,
    month: Int,
    selectedDay: Int,
    onDateSelected: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)
    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val offset = firstDayOfMonth - Calendar.SUNDAY

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val dayWidth = (screenWidthDp / 7).dp

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0 until ((daysInMonth + offset + 6) / 7)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (dayOfWeek in 1..7) {
                    val dayNumber = week * 7 + dayOfWeek - offset
                    if (dayNumber in 1..daysInMonth) {
                        val isSelected = dayNumber == selectedDay
                        DayItemAlternative(
                            day = dayNumber.toString(),
                            isSelected = isSelected,
                            onDateSelected = { onDateSelected(year, month, dayNumber) },
                            modifier = Modifier.width(dayWidth).aspectRatio(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(dayWidth).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayItemAlternative(
    day: String,
    isSelected: Boolean,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent)
            .border(1.dp, Color.LightGray)
            .clickable(onClick = onDateSelected),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
        )
    }
}