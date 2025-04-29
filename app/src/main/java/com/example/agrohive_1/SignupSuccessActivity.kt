package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignupSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_success)
        Log.d("SignupSuccessActivity", "onCreate: Activity started")

        val successText = findViewById<TextView>(R.id.successText)
        val fullText = "Signup successful! Login again to start exploring AgroHive"
        val spannableString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Log.d("SignupSuccessActivity", "Login again clicked")
                startActivity(Intent(this@SignupSuccessActivity, LoginActivity::class.java))
                finish()
            }
        }

        val startIndex = fullText.indexOf("Login again")
        val endIndex = startIndex + "Login again".length
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        successText.text = spannableString
        successText.movementMethod = LinkMovementMethod.getInstance()
        Log.d("SignupSuccessActivity", "Text set: $fullText")
    }
}