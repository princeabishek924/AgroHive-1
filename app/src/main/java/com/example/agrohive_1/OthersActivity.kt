package com.example.agrohive_1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class OthersActivity : AppCompatActivity() {

    private lateinit var firebaseUid: String
    private lateinit var userNameText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var optionsScrollView: View
    private lateinit var fragmentContainer: View
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var rootLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var profileHeader: android.widget.RelativeLayout
    private lateinit var roleChangeText: TextView
    private lateinit var settingsText: TextView
    private lateinit var editProfileText: TextView
    private lateinit var roleChangeIcon: ImageView
    private lateinit var settingsIcon: ImageView
    private lateinit var editProfileIcon: ImageView
    private var userName: String = "User"
    private var userType: String = "customer"
    private var phone: String = "Unknown"
    private var userCity: String = "Unknown"
    private var userPincode: String = "Unknown"
    private var profileImageUrl: String? = null
    private var isSettingsFragmentVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppSettings()
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_others)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading screen", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        userName = intent.getStringExtra("userName") ?: "User"
        userType = intent.getStringExtra("userType") ?: "customer"
        phone = intent.getStringExtra("phone") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        profileImageUrl = intent.getStringExtra("profileImageUrl")

        // Initialize views
        rootLayout = findViewById(R.id.root_layout)
        profileHeader = findViewById(R.id.profileHeader)
        userNameText = findViewById(R.id.userNameText)
        profileImage = findViewById(R.id.profileImage)
        optionsScrollView = findViewById(R.id.optionsScrollView)
        fragmentContainer = findViewById(R.id.fragmentContainer)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        roleChangeText = findViewById(R.id.roleChangeText)
        settingsText = findViewById(R.id.settingsText)
        editProfileText = findViewById(R.id.editProfileText)
        roleChangeIcon = findViewById(R.id.roleChangeIcon)
        settingsIcon = findViewById(R.id.settingsIcon)
        editProfileIcon = findViewById(R.id.editProfileIcon)

        // Apply theme and font size
        applyThemeAndFontSize()

        userNameText.text = userName
        showUserUploadedImage()

        findViewById<androidx.cardview.widget.CardView>(R.id.roleChangeCard).setOnClickListener {
            val intent = Intent(this, RoleChangeActivity::class.java)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userType", userType)
            intent.putExtra("userName", userName)
            intent.putExtra("phone", phone)
            intent.putExtra("userCity", userCity)
            intent.putExtra("userPincode", userPincode)
            intent.putExtra("profileImageUrl", profileImageUrl)
            startActivity(intent)
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.settingsCard).setOnClickListener {
            showSettingsFragment()
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.editProfileCard).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userType", userType)
            intent.putExtra("phone", phone)
            intent.putExtra("userCity", userCity)
            intent.putExtra("userPincode", userPincode)
            intent.putExtra("profileImageUrl", profileImageUrl)
            startActivity(intent)
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener { logout() }

        setupBottomNavigation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSettingsFragmentVisible) {
                    hideSettingsFragment()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        profileImageUrl = intent.getStringExtra("profileImageUrl")
        Log.d("OthersActivity", "onResume: profileImageUrl=$profileImageUrl")
        showUserUploadedImage()
        applyThemeAndFontSize()
    }

    private fun applyAppSettings() {
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    private fun applyThemeAndFontSize() {
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val fontSize = sharedPrefs.getString("fontSize", "medium") ?: "medium"

        // Apply font size
        val textSize = when (fontSize) {
            "small" -> 14f
            "medium" -> 16f
            "large" -> 18f
            else -> 16f
        }
        userNameText.textSize = textSize
        try {
            roleChangeText.textSize = textSize
            settingsText.textSize = textSize
            editProfileText.textSize = textSize
        } catch (e: UninitializedPropertyAccessException) {
            // Fallback: Find TextViews in CardViews
            val roleCard = findViewById<androidx.cardview.widget.CardView>(R.id.roleChangeCard)
            val settingsCard = findViewById<androidx.cardview.widget.CardView>(R.id.settingsCard)
            val editCard = findViewById<androidx.cardview.widget.CardView>(R.id.editProfileCard)
            (roleCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.textSize = textSize
            (settingsCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.textSize = textSize
            (editCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.textSize = textSize
        }

        // Apply theme
        val (backgroundColor, textColor, iconTint) = when (theme) {
            AppCompatDelegate.MODE_NIGHT_YES -> Triple(
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.white),
                ContextCompat.getColor(this, android.R.color.white)
            )
            else -> Triple(
                ContextCompat.getColor(this, android.R.color.white),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.black)
            )
        }
        rootLayout.setBackgroundColor(backgroundColor)
        profileHeader.setBackgroundColor(backgroundColor)
        userNameText.setTextColor(textColor)
        bottomNavigationView.itemIconTintList = android.content.res.ColorStateList.valueOf(iconTint)
        bottomNavigationView.itemTextColor = android.content.res.ColorStateList.valueOf(iconTint)
        try {
            roleChangeText.setTextColor(textColor)
            settingsText.setTextColor(textColor)
            editProfileText.setTextColor(textColor)
            roleChangeIcon.setColorFilter(iconTint)
            settingsIcon.setColorFilter(iconTint)
            editProfileIcon.setColorFilter(iconTint)
            val roleLayout = roleChangeText.parent as? android.view.ViewGroup
            val settingsLayout = settingsText.parent as? android.view.ViewGroup
            val editLayout = editProfileText.parent as? android.view.ViewGroup
            roleLayout?.setBackgroundColor(backgroundColor)
            settingsLayout?.setBackgroundColor(backgroundColor)
            editLayout?.setBackgroundColor(backgroundColor)
        } catch (e: UninitializedPropertyAccessException) {
            // Fallback: Update CardViews
            val roleCard = findViewById<androidx.cardview.widget.CardView>(R.id.roleChangeCard)
            val settingsCard = findViewById<androidx.cardview.widget.CardView>(R.id.settingsCard)
            val editCard = findViewById<androidx.cardview.widget.CardView>(R.id.editProfileCard)
            (roleCard.getChildAt(0) as? android.view.ViewGroup)?.setBackgroundColor(backgroundColor)
            (settingsCard.getChildAt(0) as? android.view.ViewGroup)?.setBackgroundColor(backgroundColor)
            (editCard.getChildAt(0) as? android.view.ViewGroup)?.setBackgroundColor(backgroundColor)
            (roleCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.setTextColor(textColor)
            (settingsCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.setTextColor(textColor)
            (editCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<TextView>(android.R.id.text1)?.setTextColor(textColor)
            (roleCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<ImageView>(android.R.id.icon)?.setColorFilter(iconTint)
            (settingsCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<ImageView>(android.R.id.icon)?.setColorFilter(iconTint)
            (editCard.getChildAt(0) as? android.view.ViewGroup)?.findViewById<ImageView>(android.R.id.icon)?.setColorFilter(iconTint)
        }
    }

    private fun showUserUploadedImage() {
        Log.d("OthersActivity", "showUserUploadedImage: profileImageUrl=$profileImageUrl")
        if (profileImageUrl.isNullOrEmpty()) {
            Log.d("OthersActivity", "profileImageUrl is null or empty, loading placeholder")
            Glide.with(this)
                .load(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(profileImage)
        } else {
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(profileImage)
        }
    }

    private fun showSettingsFragment() {
        isSettingsFragmentVisible = true
        optionsScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE

        val fragment = SettingsFragment()
        val bundle = Bundle().apply {
            putString("userName", userName)
            putString("firebaseUid", firebaseUid)
            putString("userType", userType)
            putString("phone", phone)
            putString("userCity", userCity)
            putString("userPincode", userPincode)
            putString("profileImageUrl", profileImageUrl)
        }
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun hideSettingsFragment() {
        isSettingsFragmentVisible = false
        optionsScrollView.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        supportFragmentManager.beginTransaction()
            .remove(supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!)
            .commit()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.putExtra("phone", phone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    val intent = Intent(this, MessageActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.putExtra("phone", phone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_create_listing -> {
                    val intent = Intent(this, CreateListingActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.putExtra("phone", phone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_my_listings -> {
                    val intent = Intent(this, MyListingsActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.putExtra("phone", phone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_others -> true
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_others
    }
}