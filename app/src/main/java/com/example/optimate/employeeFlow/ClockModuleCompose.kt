package com.example.optimate.employeeFlow

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.lang.reflect.Modifier

@Composable
fun WorkLogsList(worklogs: List<Map<String, Any>>) {
    LazyColumn {
        itemsIndexed(worklogs) { _, worklog ->
            WorkLogRow(worklog)
        }
    }
}

@Composable
fun WorkLogRow(worklog: Map<String, Any>) {
    // Transform each map entry into a "key: value" string and join them with a newline character
    val workLogText = worklog.entries.joinToString(separator = "\n") { entry ->
        "${entry.key}: ${entry.value}"
    }

    // Display the resulting string in a Text composable
    Text(text = workLogText, fontSize = 12.sp, fontWeight = FontWeight.Normal)
}
