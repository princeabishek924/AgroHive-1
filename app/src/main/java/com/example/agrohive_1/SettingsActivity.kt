package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsTitleTextView: TextView
    private lateinit var backButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsTitleTextView = findViewById(R.id.settingsTitleTextView)
        backButton = findViewById(R.id.backButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        userName = intent.getStringExtra("userName") ?: "User"
        settingsTitleTextView.text = "Settings\n\nHello, $userName!"

        backButton.setOnClickListener {
            finish() // Return to OthersActivity
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomepageActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_my_listings -> {
                    val intent = Intent(this, MyListingsActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_create_listing -> {
                    val intent = Intent(this, CreateListingActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_others -> {
                    val intent = Intent(this, OthersActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_others // Still under "Others" section
    }
}