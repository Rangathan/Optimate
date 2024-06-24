package com.example.optimate.businessOwner

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity

class AddTitle : AppCompatActivity(){
    private lateinit var homeBtn : ImageView
    private lateinit var managerBtn: ImageView
    private lateinit var employeeBtn: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_title)

        managerBtn = findViewById(R.id.managerBtn)
        employeeBtn = findViewById(R.id.employeeBtn)
        managerBtn.setOnClickListener {
            navigateToAddTitle("manager")
        }

        employeeBtn.setOnClickListener {
            navigateToAddTitle("employee")
        }

        homeBtn = findViewById(R.id.homeBtn)
        homeBtn.setOnClickListener {
            // Go to the BusinessLanding activity
            startActivity(Intent(this, DynamicLandingActivity::class.java))
        }

    }
    private fun navigateToAddTitle(titleType: String) {
        val intent = Intent(this, AddTitleDetailsActivity::class.java)
        intent.putExtra("TITLE_TYPE", titleType)
        startActivity(intent)
    }
}