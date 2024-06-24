package com.example.optimate.loginAndRegister

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.example.optimate.R

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.json.JSONObject
import kotlin.properties.Delegates

class ModuleChoosingMain : AppCompatActivity() {
    private var db = Firebase.firestore
    lateinit var paymentSheet: PaymentSheet
    lateinit var customerConfig: PaymentSheet.CustomerConfiguration
    lateinit var paymentIntentClientSecret: String
    lateinit var customerIdforPage: String
    var totalAmount by Delegates.notNull<Double>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_choosing_main)
        val uid = intent.getStringExtra("USER_UID")
        val container1 = findViewById<TextView>(R.id.container1)
        val container2 = findViewById<TextView>(R.id.container2)


        val payButtonMain = findViewById<Button>(R.id.payButtonMain)
        var container1Picked = false
        var container2Picked = false

        val module1Price = 10.00
        val module2Price = 30.00

        var currentAmount = 0.00
        val amount = findViewById<TextView>(R.id.Amount)
        val moduleList = mutableListOf<String>()
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                intent.putExtra("USER_UID", uid)
                val intent = Intent(this@ModuleChoosingMain, ModuleChoosingMain::class.java)
                startActivity(intent)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        container1.setOnClickListener {
            changeColor(container1, !container1Picked)
            if (container1Picked) {
                currentAmount -= module1Price
            } else {
                currentAmount += module1Price
            }
            amount.text = "$" + currentAmount.toString()
            container1Picked = !container1Picked
        }

        container2.setOnClickListener {
            changeColor(container2, !container2Picked)
            if (container2Picked) {
                currentAmount -= module2Price
            } else {
                currentAmount += module2Price
            }
            amount.text = "$" + currentAmount.toString()
            container2Picked = !container2Picked
        }

        payButtonMain.setOnClickListener {
            if(container1Picked){
                moduleList.add("Basic")
                if (container2Picked){
                    moduleList.add("Plus")
                }
                if (uid != null) {
                    GlobalUserData.modules = moduleList
                    totalAmount = currentAmount
                    fetchPaymentIntent(currentAmount, GlobalUserData.bid)

                }
            } else {
                Toast.makeText(this, "You need the Basic Plan to continue", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun changeColor(view: TextView, hasStroke: Boolean) {
        val defaultBackground = ContextCompat.getDrawable(this, R.drawable.stroke_white_color)
        val clickedBackground = ContextCompat.getDrawable(this, R.drawable.stroke_dark_purple)

        val newBackground = if (hasStroke) clickedBackground else defaultBackground
        view.foreground = newBackground
    }


    private fun updateUser() {
        if(GlobalUserData.uid != "") {
            Log.e(this.toString(), "updateUser: called ",)
            db.collection("users")
                .whereEqualTo("UID", GlobalUserData.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.e("EditAccountActivity", "No matching account found")
                        return@addOnSuccessListener
                    }

                    val account = documents.first()

                    account.reference.update("modules", GlobalUserData.modules)
                        .addOnSuccessListener {
                            Log.d("EditAccountActivity", "Account updated successfully")

                        }
                        .addOnFailureListener { e ->
                            Log.e("EditAccountActivity", "Error updating account", e)
                        }
                }
        }
    }
    private fun reload() {
        // Reload the current activity or perform other actions if the user is already signed in
    }
   private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when(paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                print("Canceled")
            }
            is PaymentSheetResult.Failed -> {
                print("Error: ${paymentSheetResult.error}")
            }
            is PaymentSheetResult.Completed -> {
                // Display for example, an order confirmation screen
                print("Completed")
                updateUser()
                saveCustomerToDatabase()
                val intent = Intent(this, PaymentConfirm::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun presentPaymentSheet() {
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "Optimate",
                customer = customerConfig,
                // Set `allowsDelayedPaymentMethods` to true if your business handles
                // delayed notification payment methods like US bank accounts.
                allowsDelayedPaymentMethods = true
            )
        )
    }

    private fun fetchPaymentIntent(amount: Double, businessId: String) {
        // Convert amount to cents (integer)
        val amountInCents = (amount * 100).toInt()

        val jsonBody = JSONObject()
        jsonBody.put("amount", amountInCents) // Use amount in cents
        jsonBody.put("businessId", businessId)

        // Make a network request with the amount and businessId
        "https://optimateserver.onrender.com/payment-sheet"
            .httpPost()
            .header("Content-Type" to "application/json")
            .body(jsonBody.toString())
            .responseJson { request, response, result ->
                when (result) {
                    is Result.Success -> {
                        val responseJson = result.get().obj()
                        val paymentIntent = responseJson.optString("paymentIntent", "")
                        val ephemeralKey = responseJson.optString("ephemeralKey", "")
                        val customerId = responseJson.optString("customer", "")
                        val publishableKey = responseJson.optString("publishableKey", "")

                        if (paymentIntent.isNotEmpty() && ephemeralKey.isNotEmpty() && customerId.isNotEmpty() && publishableKey.isNotEmpty()) {
                            paymentIntentClientSecret = paymentIntent
                            customerConfig = PaymentSheet.CustomerConfiguration(customerId, ephemeralKey)
                            PaymentConfiguration.init(this, publishableKey)
                            customerIdforPage = customerId
                            presentPaymentSheet()
                        } else {
                            // Handle invalid response or missing data
                            Log.e(TAG, "Invalid response: $responseJson")
                            // Show an error message or retry the request
                        }
                    }
                    is Result.Failure -> {
                        val ex = result.getException()
                        Log.e(TAG, "Error fetching payment intent", ex)
                        // Handle network request failure
                        // Show an error message or retry the request
                    }
                }
            }
    }

    private fun saveCustomerToDatabase() {

        val paymentDetailtoDB = hashMapOf(
            "BID" to GlobalUserData.bid,
            "stripeId" to customerIdforPage,
            "Payment" to totalAmount,
            "Date" to Timestamp.now()
        )


        db.collection("accountPayment")
            .add(paymentDetailtoDB)
            .addOnSuccessListener { documentReference ->
                Log.d("EditAccountActivity", "Payment updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("EditAccountActivity", "Payment updating account", e)
            }

    }




}