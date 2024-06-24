package com.example.optimate.employeeFlow

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.optimate.R
import com.example.optimate.businessOwner.XmlTopBar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ViewHistoryScreen(showDateRangePicker: (updateDateRange: (String, String) -> Unit) -> Unit, workLogs: List<Map<String, Any>>)   {
    var dateRangeText by remember { mutableStateOf("") }
    val totalHoursForPeriod = remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = { XmlTopBar(titleText = "View History") },
        containerColor = colorResource(id = R.color.screen_border_colors),
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateRangeText,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    SelectDatesButton {

                        showDateRangePicker { startDate, endDate ->
                            dateRangeText = "$startDate - $endDate"
                        }
                    }

                }

                TotalHours(totalHoursForPeriod.longValue)

                Spacer(modifier = Modifier.height(16.dp))

                if (workLogs.isEmpty()) {
                    NoDataFound("No work logs found.")
                } else {
                    WorkLogList(workLogs, totalHoursForPeriod)

                }
            }
        }
    )
}
@Composable
fun SelectDatesButton(onClick: () -> Unit) {
    val buttonColor = colorResource(id = R.color.light_green)
    Button(onClick = onClick,
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp))
    {
        Text("Select Dates",fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)

    }
}

@Composable
fun WorkLogList(workLogs: List<Map<String, Any>>, totalHoursForPeriod: MutableState<Long>) {
    LazyColumn {
        itemsIndexed(workLogs) { _, workLog ->
            // Cast the workLog item to the correct type
            val typedWorkLog = workLog.mapValues { entry ->
                // Assuming the value is a List<*>, cast each item in the list to Map<String, String>
                @Suppress("UNCHECKED_CAST")
                (entry.value as List<Map<String, String>>)
            }

            WorkLogsRow(typedWorkLog, totalHoursForPeriod)
        }
    }
}

@Composable
fun WorkLogsRow(workLog: Map<String, List<Map<String, String>>>, totalHoursForPeriod: MutableState<Long>) {
    val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
    var totalHours: Long
    var totalMinutes: Long
    var totalSeconds: Long
    var containsClockOut: Boolean
    totalHoursForPeriod.value = 0L


    fun formatDate(dateString: String): String {
        return if (dateString.length == 8) {
            "${dateString.substring(4,6)}/${dateString.substring(6,8)}/${dateString.substring(0,4)}"
        } else {
            dateString // Return the original string if it's not in the expected format
        }
    }

    // Convert map entries to a list and iterate with index
    workLog.entries.toList().forEachIndexed { index, (date, logs) ->
        var isExpanded by remember { mutableStateOf(false) }
        var totalDurationInMillis = 0L
        var clockInTime: Long? = null
        var clockOutTime: Long? = null
        val breakStartTimes = mutableListOf<Long>()
        val breakEndTimes = mutableListOf<Long>()
        containsClockOut = false
        val cornerRadius = 12.dp

        logs.forEach { log ->
            log.forEach { (key, value) ->
                when (key) {
                    "clockIn" -> clockInTime = dateFormat.parse(value)?.time
                    "clockOut" -> {clockOutTime = dateFormat.parse(value)?.time
                        containsClockOut = true}
                    "breakStart" -> breakStartTimes.add(dateFormat.parse(value)?.time ?: 0L)
                    "breakEnd" -> breakEndTimes.add(dateFormat.parse(value)?.time ?: 0L)
                }
            }

        }


        if(containsClockOut) {
            val workDuration = (clockOutTime ?: 0L) - (clockInTime ?: 0L)
            val breakDuration = breakEndTimes.sum() - breakStartTimes.sum()
            totalDurationInMillis += (workDuration - breakDuration)
            totalHours = TimeUnit.MILLISECONDS.toHours(totalDurationInMillis)
            totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationInMillis) % 60
            totalSeconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationInMillis) % 60
            totalHoursForPeriod.value += totalDurationInMillis
        }
        else{
            totalHours = 0L
            totalMinutes = 0L
            totalSeconds = 0L
            totalHoursForPeriod.value += 0L
        }

        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .border(
                    1.dp,
                    colorResource(id = R.color.blue),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(cornerRadius),
        ) {
            Column(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Text(text = " ", fontSize = 10.sp)


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        if (containsClockOut) {
                            val styledText = buildAnnotatedString {
                                append(formatDate(date) + " ")
                                // Apply a SpanStyle to "Total Hours" part
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.blue)
                                    )
                                ) {
                                    append(
                                        "(Total Hours: %02d:%02d:%02d)".format(
                                            totalHours,
                                            totalMinutes,
                                            totalSeconds
                                        )
                                    )
                                }
                            }
                            Text(text = styledText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        } else {
                            val styledText = buildAnnotatedString {
                                append(formatDate(date) + " ")
                                // Apply a SpanStyle to "Total Hours" part
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.blue)
                                    )
                                ) {
                                    append("(Total Hours: 00:00:00)")
                                }
                            }
                            Text(text = styledText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        )
                    }
                    Text(text = " ", fontSize = 10.sp)
                }


                // Expandable content for each card
                if (isExpanded) {
                    logs.forEach { log ->
                        log.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                if (key =="clockIn")
                                    Text(text = "Clock In:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (key =="clockOut")
                                    Text(text = "Clock Out:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (key =="breakStart")
                                    Text(text = "Break Start:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (key =="breakEnd")
                                    Text(text = "Break End:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Normal, color = colorResource(id = R.color.blue))
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TotalHours(totalDurationInMillis: Long) {
    // Convert the total duration from milliseconds to hours, minutes, and seconds
    val totalHours = TimeUnit.MILLISECONDS.toHours(totalDurationInMillis)
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationInMillis) % 60
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationInMillis) % 60

    Text(
        text = "Total Hours: %02d:%02d:%02d".format(totalHours, totalMinutes, totalSeconds),
        modifier = Modifier.padding(start = 16.dp),
        color = Color.Black,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun NoDataFound(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = "No Data",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}


//sample data
val theList = listOf(
    mapOf(
        "20240223" to listOf(
            mapOf("clockIn" to "02/23/2024 12:33:21"),
            mapOf("breakStart" to "02/23/2024 12:43:21"),
            mapOf("breakEnd" to "02/23/2024 12:53:21"),
            mapOf("clockOut" to "02/23/2024 22:33:21")
        ),
        "20240224" to listOf(
            mapOf("clockIn" to "02/24/2024 12:33:21"),
            mapOf("breakStart" to "02/24/2024 12:43:21"),
            mapOf("breakEnd" to "02/24/2024 12:53:21"),
            mapOf("clockOut" to "02/24/2024 22:33:21")
        )
    )
)

@Preview
@Composable
fun ViewHistoryScreenPreview() {
    ViewHistoryScreen(
        showDateRangePicker = { }, workLogs = theList
    )
}
