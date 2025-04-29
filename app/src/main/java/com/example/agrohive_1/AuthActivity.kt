package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Find views
        val loginButton: Button = findViewById(R.id.loginButton)
        val signupButton: Button = findViewById(R.id.signupButton)

        // Handle login button click
        loginButton.setOnClickListener {
            // Navigate to Login Screen or Activity
            startActivity(Intent(this@AuthActivity, LoginActivity::class.java))
        }

        // Handle sign-up button click
        signupButton.setOnClickListener {
            // Navigate to Sign-Up Screen or Activity
            startActivity(Intent(this@AuthActivity, SignupActivity::class.java))
        }
    }
}
