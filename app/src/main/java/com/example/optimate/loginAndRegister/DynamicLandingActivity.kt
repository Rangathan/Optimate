package com.example.optimate.loginAndRegister

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.optimate.R
import com.example.optimate.businessOwner.Requests.Companion.TAG
import com.example.optimate.employeeFlow.ProfilePage
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging

class DynamicLandingActivity : AppCompatActivity(){
    val db = Firebase.firestore
    private lateinit var businessName: String
    private lateinit var username: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dynamic_landing)
        businessName = GlobalUserData.name
        username = findViewById(R.id.username)
        username.text = businessName
        val callback = object : OnBackPressedCallback(true /* default to enabled */) {
            override fun handleOnBackPressed() {
                // Start the same activity again
                val intent = Intent(this@DynamicLandingActivity, DynamicLandingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)



        val composeView = findViewById<ComposeView>(R.id.compose_view)
        val settingPage = findViewById<ImageView>(R.id.businessIcon)
        getAccountAccess(GlobalUserData.title, GlobalUserData.bid) {
            composeView.setContent {
                DynamicLandingScreen(GlobalUserData.access, GlobalUserData.role, GlobalUserData.modules)
            }
        }
        if (GlobalUserData.profilePic != "") {
            Glide.with(this)
                .load(GlobalUserData.profilePic)
                .into(settingPage)
        }

        settingPage.setOnClickListener{
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }
        requestNotificationPermission()
        updateMessagingToken()
    }

    private fun getAccountAccess(title: String, bid: String, onAccessFetched: () -> Unit) {
        if (title == "businessOwner") {
            GlobalUserData.access = emptyList()
            Log.d("hihihi", "access: ${GlobalUserData.access}")
            Log.d("hihihi", "name: ${GlobalUserData.name}")
            onAccessFetched() // Invoke the callback immediately
        } else {
            db.collection("titles")
                .whereEqualTo("title", title)
                .whereEqualTo("bid", bid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        GlobalUserData.access = document.data["access"] as List<String>
                        Log.d("hihihi", "access: ${GlobalUserData.access}")
                        Log.d("hihihi", "name: ${GlobalUserData.name}")
                    }
                    onAccessFetched() // Invoke the callback after fetching the data
                }
        }
    }

    private fun requestNotificationPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if(!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateMessagingToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            getToken(token)
            // Log and toast
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, msg)
        })
    }

    private fun getToken(token: String) {

        GlobalUserData.deviceToken = token


        // Update token in Firebase
        db.collection("users")
            .whereEqualTo("UID", GlobalUserData.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("EditAccountActivity", "No matching account found")
                    return@addOnSuccessListener
                }

                val account = documents.first()

                account.reference.update("deviceToken", token)
                    .addOnSuccessListener {
                        Log.d("EditAccountActivity", "Account Updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditAccountActivity", "Error Updating account", e)
                    }
            }

    }


}

