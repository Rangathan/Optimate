package com.example.optimate.businessOwner

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.optimate.R
import com.example.optimate.loginAndRegister.DynamicLandingActivity

class XmlTopBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val homeButton: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.topbar, this, true)

        titleTextView = findViewById(R.id.topBarTitle)
        homeButton = findViewById(R.id.homeBtn)

        homeButton.setOnClickListener {
            // Navigate to DynamicLandingActivity
            val intent = Intent(context, DynamicLandingActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun setTitle(title: CharSequence) {
        titleTextView.text = title
    }

}
