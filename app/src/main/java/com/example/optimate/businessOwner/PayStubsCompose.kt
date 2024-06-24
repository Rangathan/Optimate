package com.example.optimate.businessOwner

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.optimate.R
import com.example.optimate.employeeFlow.NoDataFound
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.biWeeklyDateRanges2024
import com.example.optimate.loginAndRegister.getWage
import com.example.optimate.loginAndRegister.milliSecondsToHours
import com.example.optimate.loginAndRegister.uidToName
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PayStubsScreen() {
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    Box(modifier = Modifier.fillMaxSize()) { // Fill the entire screen
        Scaffold(
            topBar = { XmlTopBar(titleText = "Pay Stubs") },
            containerColor = colorResource(id = R.color.screen_border_colors),
            content = { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    BiWeeklyDropDown(biWeeklyDateRanges2024 = biWeeklyDateRanges2024, today = today)
                    // Other content can go here
                }
            }
        )
        PayRequests(modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)) // Positioned at the bottom right of the Box
    }
}
@Composable
fun BiWeeklyDropDown(biWeeklyDateRanges2024: List<List<String>>, today: String) {
    var expanded by remember { mutableStateOf(false) }
    val validBiWeeklyDateRanges = biWeeklyDateRanges2024.filter { it[0] <= today}
    val originalFormat = SimpleDateFormat("yyyyMMdd")
    val targetFormat = SimpleDateFormat("MM/dd/yyyy")

    // Find the current or nearest future bi-weekly period index
    val currentPeriodIndex = validBiWeeklyDateRanges.indexOfFirst { today >= it[0] && today <= it[1] }
    var selectedIndex by remember { mutableStateOf(if (currentPeriodIndex != -1) currentPeriodIndex else 0) }

    // Prepare to store and display fetched work logs
    var workLogs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val cornerRadius = 12.dp

    // React to selection changes and fetch data
    LaunchedEffect(selectedIndex) {
        getWorkedHoursForDateRange(validBiWeeklyDateRanges[selectedIndex]) { logs ->
            workLogs = logs
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { expanded = !expanded }
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(cornerRadius)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {

                val selectedRangeText = if (validBiWeeklyDateRanges.isNotEmpty())
                //format to MM/dd/yyyy
                    "${targetFormat.format(originalFormat.parse(validBiWeeklyDateRanges[selectedIndex][0])!!)} to ${targetFormat.format(originalFormat.parse(validBiWeeklyDateRanges[selectedIndex][1])!!)}"
                else
                    "Select Date Range"

                Text(
                    text = selectedRangeText,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Arrow"
                )
            }
            if (expanded) {
                Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                validBiWeeklyDateRanges.forEachIndexed { index, dateRange ->
                    Text(
                        text = "${targetFormat.format(originalFormat.parse(dateRange[0])!!)} to ${targetFormat.format(originalFormat.parse(dateRange[1])!!)}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIndex = index
                                expanded = false
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (workLogs.isEmpty()) {
        NoDataFound(text = "No data found")
    } else {
        DisplayWorkLogs(workLogs = workLogs)
    }
}


private fun getWorkedHoursForDateRange(dateRange: List<String>, onResult: (List<Map<String, Any>>) -> Unit) {
    val db = Firebase.firestore
    val startDate = SimpleDateFormat("yyyyMMdd").parse(dateRange[0])
    val endDate = SimpleDateFormat("yyyyMMdd").parse(dateRange[1])

    db.collection("totalHours")
        .whereEqualTo("bid", GlobalUserData.bid)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val approvedWorkLogs = mutableListOf<Map<String, Any>>()

            querySnapshot.documents.forEach { documentSnapshot ->
                val workLogs = documentSnapshot.data as Map<String, Any>?
                workLogs?.forEach { (key, value) ->
                    if (key != "bid") {
                        val uid = key // Assume the key that is not "bid" is the UID
                        (value as List<Map<String, Any>>).forEach { log ->
                            val approved = log["approved"] as Boolean?
                            if (approved == true) {
                                log.keys.filter { it.matches(Regex("\\d{8}")) }.forEach { dateKey ->
                                    try {
                                        val logDate = SimpleDateFormat("yyyyMMdd").parse(dateKey)
                                        if (logDate in startDate..endDate) {
                                            val hours = log[dateKey] as? Long ?: 0L // Assuming hours are stored as Long
                                            val wage = (log["wage"] as? Number)?.toDouble() ?: 0.0
                                            // Create a new map that includes the UID, the date, and the hours
                                            val logWithUid = mapOf("uid" to uid, "date" to dateKey, "hours" to hours, "wage" to wage)
                                            approvedWorkLogs.add(logWithUid)
                                        }
                                    } catch (e: Exception) {
                                        // Handle date parsing exception
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Return the result through the callback
            onResult(approvedWorkLogs)
        }
        .addOnFailureListener { e ->
            // Handle the error appropriately, maybe log or show a UI indication
        }
}
data class AggregatedLog(val totalHours: Long, val wage: Double)
@Composable
fun DisplayWorkLogs(workLogs: List<Map<String, Any>>) {
    // Aggregate workLogs by UID, summing up the hours
    val aggregatedLogs = workLogs.groupBy { it["uid"] as? String ?: "" }
        .mapValues { entry ->
            val totalHours = entry.value.sumOf { it["hours"] as? Long ?: 0L }
            val wage = entry.value[0]["wage"] as? Double ?: 0.0
            AggregatedLog(totalHours, wage)
        }


    // Wrap the card display logic in a LazyColumn for scrollable behavior
    LazyColumn {
        itemsIndexed(aggregatedLogs.entries.toList()) { index, (uid, AggregatedLog) ->
            // Convert milliseconds to hours in 2 decimal places
            val hoursInDouble = milliSecondsToHours(AggregatedLog.totalHours)

            // State for storing the name and wage fetched asynchronously
            var name by remember { mutableStateOf(uid) } // Initialize with UID as a fallback
            var wage by remember { mutableStateOf(0.0) }
            var pay by remember { mutableStateOf(0.0) }
            val cornerRadius = 12.dp

            // Determine the background color based on the index
            val backgroundColor = colorResource(id = R.color.light_purple)

            // Fetch the user's name based on the UID and calculate pay
            LaunchedEffect(uid) {
                uidToName(uid) { fetchedName ->
                    if (fetchedName.isNotEmpty()) {
                        name = fetchedName // Update the name only if fetched name is not empty
                    }
                }
            }
            wage = AggregatedLog.wage
            pay = wage * hoursInDouble

            // UI to display the user's name, the sum of hours, and the pay
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .border(1.dp, colorResource(id = R.color.blue), shape = RoundedCornerShape(cornerRadius)),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(cornerRadius),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "$name ($$wage)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "$hoursInDouble hrs", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "$%.2f".format(pay), fontSize = 16.sp, fontWeight = FontWeight.Normal, color = colorResource(
                        id = R.color.blue))
                }
            }
        }
    }
}
@Composable
fun PayRequests(modifier: Modifier = Modifier) {
    val buttonColor = colorResource(id = R.color.light_green)
    val payRequestsBtn = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(payRequestsBtn, PayRequestsActivity::class.java)
            payRequestsBtn.startActivity(intent)
        },
        modifier = modifier.zIndex(1f) ,
        colors = ButtonDefaults.run { buttonColors(buttonColor) },
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Text("Pay Requests", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
fun PayStubsScreenPreview() {
    PayStubsScreen()
}
