package com.example.optimate.businessOwner


import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import com.example.optimate.R
import com.example.optimate.employeeFlow.NoDataFound
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.addRevenueOrExpenseToDB
import com.example.optimate.loginAndRegister.getWage
import com.example.optimate.loginAndRegister.milliSecondsToHours
import com.example.optimate.loginAndRegister.uidToName
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.String.format
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

//workLogsWaitForApproval:
// {lV0Hg8ikbRM6GZZN6FjslXki4MD2=[{20240229=4000}],
// JiHljMeEbuO7vEG1aUaKrbLto5t1=[{20240228=20000}],
// KbClYhd5vXhRPf84jbCPr7cAglq1=[{20240229=4000}, {20240228=40000}]}
@Composable
fun PayRequestsScreen(workLogsWaitForApproval: MutableMap<String, List<Map<String, Long>>>) {
    Scaffold(
        topBar = { XmlTopBar( titleText = "Pay Requests" ) },
        containerColor = colorResource(id = R.color.screen_border_colors),
        content = {innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).padding(top = 16.dp)
            ) {
                if (workLogsWaitForApproval.isEmpty()) {
                    NoDataFound(text = "No pay requests found")
                } else {
                    WorkingHoursCardList(workLogsWaitForApproval)
                }
            }
        }
    )
}

@Composable
fun WorkingHoursCardList(workLogsWaitForApproval: MutableMap<String, List<Map<String, Long>>>) {
    LazyColumn {
        itemsIndexed(workLogsWaitForApproval.keys.toList()) { index, userId ->
            val logs = workLogsWaitForApproval[userId] ?: emptyList()
            var userName by remember { mutableStateOf("") }

            LaunchedEffect(userId) {
                uidToName(userId) { name ->
                    userName = name
                }
            }

            logs.forEach { log ->
                WorkingHoursCard(index, userId, userName, log)
            }
        }
    }
}

@Composable
fun WorkingHoursCard(index: Int, userId: String, userName: String, log: Map<String, Long>) {
    val date = log.keys.first { it != "wage" } // Filter out the wage key to find the date
    val hours: Long = (log[date] as? Number)?.toLong() ?: 0L // Safely cast to Number then to Long
    val wage: Double = (log["wage"] as? Number)?.toDouble() ?: 0.0 // Safely cast to Number then to Double
    var pay by remember { mutableStateOf(0.0) }
    var isExpanded by remember { mutableStateOf(false) }
    val backgroundColor = colorResource(id = R.color.light_purple)
    val cornerRadius = 12.dp
    val workLogsDetails = remember { mutableStateListOf<Map<String, String>>() }

    // Calculate the pay when the wage or hours change
    LaunchedEffect(wage, hours) {
        pay = milliSecondsToHours(hours) * wage
    }

    // Load detailed work logs when the card is expanded
    LaunchedEffect(isExpanded, userId, date) {
        if (isExpanded) {
            val db = Firebase.firestore
            db.collection("workLogs")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener { documents ->
                    workLogsDetails.clear()
                    for (document in documents) {
                        val data = document.data.toMutableMap()
                        val dateLogs = data[date] as? List<Map<String, String>> ?: emptyList()
                        workLogsDetails.addAll(dateLogs)
                    }
                }
        }
    }

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .border(1.dp, colorResource(id = R.color.blue), shape = RoundedCornerShape(cornerRadius)),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            //format yyyyMMdd to MM/dd/yyyy
            val originalFormat = SimpleDateFormat("yyyyMMdd")
            val targetFormat = SimpleDateFormat("MM/dd/yyyy")
            val parsedDate = originalFormat.parse(date)
            val formattedDate = targetFormat.format(parsedDate)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "$userName ($$wage)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "$formattedDate: ${milliSecondsToHours(hours)} hs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Pay: $${format("%.2f", pay)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.blue)
                        )
                    }
                }
                if (isExpanded) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        workLogsDetails.forEach { detailMap ->
                            detailMap.forEach { (key, value) ->
                                if (key != "uid" && key != "bid") {
                                    Text(
                                        text = "$key: $value",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        val originalFormat = SimpleDateFormat("yyyyMMdd")
                        val targetFormat = SimpleDateFormat("MM/dd/yy")
                        val parsedDate = originalFormat.parse(date)
                        val formattedDate = targetFormat.format(parsedDate)


                        ApproveBtn(modifier = Modifier.fillMaxWidth(), uid = userId, date = date, expensesDate = formattedDate, pay = pay, name =userName)
                    }
                }
            }

            // The expand/collapse icon
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
fun LoadingDialog(showDialog: Boolean) {
    if (showDialog) {
        Dialog(onDismissRequest = { /* Dialog cannot be dismissed by the user */ }) {
            // Customize the dialog's appearance
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ApproveBtn(modifier: Modifier = Modifier, uid: String, date: String, expensesDate: String, pay: Double, name: String) {
    val approveBtn = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) } // State to control the visibility of the loading dialog

    Box(modifier = modifier) {
        if (showDialog) {
            ApproveDialog(
                onConfirm = {
                    setApprovedToTrue(uid, date)
                    addRevenueOrExpenseToDB("expense", expensesDate, pay, "Pay to $name", true)
                    showDialog = false
                    showLoading = true // Show loading dialog

                    // Launch a coroutine to delay the restart
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000) // Wait for 2 seconds
                        showLoading = false // Hide loading dialog before restarting the activity
                        (approveBtn as? Activity)?.let { activity ->
                            activity.finish() // Finish the current activity
                            val restartIntent = Intent(activity, PayRequestsActivity::class.java)
                            activity.startActivity(restartIntent) // Start the new instance of the activity
                        }

                    }
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.light_green)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Approve", fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
        }

        LoadingDialog(showDialog = showLoading) // Show the loading dialog when showLoading is true
    }
}




@Composable
fun ApproveDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material.Text("Pay Request Approval") },
        text = { androidx.compose.material.Text("Are you sure you want to approve?") },
        confirmButton = {
            androidx.compose.material.Button(onClick = onConfirm) {
                androidx.compose.material.Text("Yes, Approve")
            }
        },
        dismissButton = {
            androidx.compose.material.Button(onClick = onDismiss) {
                androidx.compose.material.Text("No, Go Back")
            }
        }
    )
}

//bid:"d89RXe3xFjNNCEAftuslt3pGWR23ab"
//JiHljMeEbuO7vEG1aUaKrbLto5t1(this is uid)=[{20240228: 20000, approved: false}]
//KbClYhd5vXhRPf84jbCPr7cAglq1(this is uid)=[{20240229: 4000, approved: false}, {20240228: 40000, approved: false}]
fun setApprovedToTrue(uid: String, date: String) {
    val db = Firebase.firestore
    db.collection("totalHours")
        .whereEqualTo("bid", GlobalUserData.bid)
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val totalHours = document.data[uid] as? List<Map<String, Any>>
                totalHours?.let {
                    // Find the index of the map that contains the specified date
                    val index = it.indexOfFirst { log -> log.containsKey(date) && log["approved"] == false }

                    if (index != -1) {
                        // If found, create a copy of the map to modify it
                        val updatedLog = HashMap(it[index])
                        updatedLog["approved"] = true // Set approved to true

                        // Create a new list with the updated map
                        val updatedTotalHours = ArrayList(it)
                        updatedTotalHours[index] = updatedLog

                        // Prepare the update for the Firestore document
                        val update = hashMapOf<String, Any>(uid to updatedTotalHours)

                        // Update the document
                        db.collection("totalHours").document(document.id)
                            .update(update)
                            .addOnSuccessListener {
                                Log.d("Firestore", "DocumentSnapshot successfully updated!")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error updating document", e)
                            }
                    }
                }
            }
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
        }
}

////getWorkLogsByUidAndDate("lV0Hg8ikbRM6GZZN6FjslXki4MD2", "20240229")
//fun getWorkLogsByUidAndDate(uid: String, date: String) {
//    val db = Firebase.firestore
//    val workLogsList = mutableListOf<Map<String, Any>>()
//    db.collection("workLogs")
//        .whereEqualTo("uid", uid)
//        .get()
//        .addOnSuccessListener { documents ->
//            for (document in documents) {
//                val data = document.data.toMutableMap()
//                data.remove("uid")
//                data.remove("bid")
//                workLogsList.add(data)
//            }
//            Log.d("PayRequestsActivity", "workLogsList: $workLogsList")
//            //workLogsList:
//            // [{20240229=[{clockIn=02/29/2024 09:27:13}, {breakStart=02/29/2024 09:27:15}, {breakEnd=02/29/2024 09:27:18}, {clockOut=02/29/2024 09:27:20}],
//            // 20240227=[{clockIn=02/27/2024 12:04:31}, {breakStart=02/27/2024 12:04:33}, {breakEnd=02/27/2024 12:04:51}, {clockOut=02/27/2024 12:04:56}],
//            // 20240225=[{clockIn=02/25/2024 00:04:58}, {clockOut=02/25/2024 00:05:02}],
//            // 20240224=[{clockIn=02/24/2024 23:24:07}, {breakStart=02/24/2024 23:24:11}, {breakEnd=02/24/2024 23:24:14}, {clockOut=02/24/2024 23:24:15}]}]
//            for (workLog in workLogsList) {
//                val filteredWorkLog = mutableMapOf<String, Any>()
//                for (key in workLog.keys) {
//                    val workLogDate = key
//                    if (workLogDate == date) {
//                        filteredWorkLog[key] = workLog[key]!!
//                    }
//                }
//                Log.d("PayRequestsActivity", "filteredWorkLog: $filteredWorkLog")
//                //filteredWorkLog: {20240229=[{clockIn=02/29/2024 09:27:13}, {breakStart=02/29/2024 09:27:15}, {breakEnd=02/29/2024 09:27:18}, {clockOut=02/29/2024 09:27:20}]}
//            }
//
//        }
//}


@Preview
@Composable
fun ExpensesRequestsScreenPreview() {
    //workLogsWaitForApproval:
// {lV0Hg8ikbRM6GZZN6FjslXki4MD2=[{20240229=4000}],
// JiHljMeEbuO7vEG1aUaKrbLto5t1=[{20240228=20000}],
// KbClYhd5vXhRPf84jbCPr7cAglq1=[{20240229=4000}, {20240228=40000}]}
    val workLogsWaitForApproval = mutableMapOf<String, List<Map<String, Long>>>()
    workLogsWaitForApproval["lV0Hg8ikbRM6GZZN6FjslXki4MD2"] = listOf(mapOf("20240229" to 4000))
    workLogsWaitForApproval["JiHljMeEbuO7vEG1aUaKrbLto5t1"] = listOf(mapOf("20240228" to 20000))
    workLogsWaitForApproval["KbClYhd5vXhRPf84jbCPr7cAglq1"] = listOf(mapOf("20240229" to 4000), mapOf("20240228" to 40000))
    PayRequestsScreen(workLogsWaitForApproval)
}


