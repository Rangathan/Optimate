package com.example.optimate.businessOwner

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.example.optimate.loginAndRegister.GlobalUserData
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class PayRequestsActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private val workLogsWaitForApproval: MutableMap<String, List<Map<String, Long>>> = mutableStateMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWorkLogsWaitForApproval()



        setContent {
            PayRequestsScreen(workLogsWaitForApproval)
        }
    }

    private fun getWorkLogsWaitForApproval() {
        db.collection("totalHours")
            .whereEqualTo("bid", GlobalUserData.bid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { documentSnapshot ->
                    val workLog = documentSnapshot.data

                    if (workLog != null) {
                        Log.d("PayRequestsActivity", "workLog: $workLog")
                        val filteredWorkLog = workLog
                            .filter { (key, value) ->
                                key != "bid" &&
                                        value is List<*> &&
                                        value.any { (it as? Map<*, *>)?.get("approved") == false }
                            }
                            .mapValues { (_, value) ->
                                if (value is List<*>) {
                                    value.filterIsInstance<Map<*, *>>()
                                        .filter { it["approved"] == false }
                                        .map { entry ->
                                            // Remove the "approved" and "wage" key from each map
                                            entry.filterKeys { it != "approved" }

                                        }
                                } else {
                                    value
                                }
                            }

                        workLogsWaitForApproval.putAll(filteredWorkLog as Map<String, List<Map<String, Long>>>)
                        Log.d("PayRequestsActivity", "filteredWorkLog: $filteredWorkLog")
                        Log.d("PayRequestsActivity", "workLogsWaitForApproval: $workLogsWaitForApproval")
                    }
                }

            }
            .addOnFailureListener { e ->
                Log.w("PayRequestsActivity", "Error getting workLogs for approval", e)
            }
    }

    override fun onResume() {
        super.onResume()
        getWorkLogsWaitForApproval()
    }

}

