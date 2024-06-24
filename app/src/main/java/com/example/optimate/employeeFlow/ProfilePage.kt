package com.example.optimate.employeeFlow



import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity
import com.example.optimate.loginAndRegister.GlobalUserData
import com.example.optimate.loginAndRegister.Login
import com.google.firebase.auth.FirebaseAuth


class ProfilePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)

        val signOutBtn = findViewById<Button>(R.id.signOutBtn)


        //val viewAvailabilityBtn = findViewById<Button>(R.id.viewAvailabilityBtn)
        val editProfileBtn = findViewById<Button>(R.id.editProfile)
        val addressText = findViewById<TextView>(R.id.addressText)
        val wageText = findViewById<TextView>(R.id.wageText)
        val phoneText = findViewById<TextView>(R.id.phoneText)
        val hourlyText = findViewById<TextView>(R.id.hourlyText)
        val roleText = findViewById<TextView>(R.id.textView7)
        val nameText = findViewById<TextView>(R.id.textView6)
        val profilePic = findViewById<ImageView>(R.id.profilePic)
        val homeBtn = findViewById<ImageView>(R.id.homeBtn)
        val formattedWage = if (!GlobalUserData.wage.toString().contains('.')) {
            "$${GlobalUserData.wage}.00"
        } else if (GlobalUserData.wage.toString().substring(GlobalUserData.wage.toString().indexOf('.') + 1).length == 1) {
            "$${GlobalUserData.wage}0"
        } else {
            "$$GlobalUserData.wage.toString()"
        }


        addressText.text = GlobalUserData.address
        wageText.text = formattedWage
        val phone = GlobalUserData.phone
        val formattedPhone = if (phone.length >= 10) {
            "(${phone.substring(0, 3)})-${phone.substring(3, 6)}-${phone.substring(6, 10)}"
        } else {
            // Handle case where phone number is not long enough
            "Invalid phone number"
        }
        phoneText.text = formattedPhone
        roleText.text = GlobalUserData.role
        nameText.text = GlobalUserData.name
        if (GlobalUserData.profilePic != "") {
            Glide.with(this)
                .load(GlobalUserData.profilePic)
                .into(profilePic)
        }



        if(GlobalUserData.role == "businessOwner"){
            wageText.text = ""
            hourlyText.text = ""
        }

        homeBtn.setOnClickListener {
            val intent = Intent(this, DynamicLandingActivity::class.java)
            startActivity(intent)
        }



// Set OnClickListener for View Availability Button

        // Set OnClickListener for Edit Profile Button
//        editProfileBtn.setOnClickListener {
//            startActivity(Intent(this,EditProfile::class.java))
//        }

        /*viewAvailabilityBtn.setOnClickListener {
            startActivity(Intent(this,Availability::class.java))
        }*/
        editProfileBtn.setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        signOutBtn.setOnClickListener{
            signOutUser(this)
        }


    }



    private fun signOutUser(context: Context) {
        val user = FirebaseAuth.getInstance()
        user.signOut()
        startActivity(Intent(this,Login::class.java))
    }


}

