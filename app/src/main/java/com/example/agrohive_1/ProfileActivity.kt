package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var nameTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var editProfileButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userName: String = "User"
    private var userPhone: String = "Unknown"
    private var userCity: String = "Unknown"
    private var userPincode: String = "Unknown"
    private var firebaseUid: String = ""
    private var userType: String = "customer"
    private var isFetchingUserData = false

    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            userName = result.data?.getStringExtra("userName") ?: userName
            userPhone = result.data?.getStringExtra("phone") ?: userPhone
            userCity = result.data?.getStringExtra("city") ?: userCity
            userPincode = result.data?.getStringExtra("pincode") ?: userPincode
            updateUI()
            Log.d("ProfileActivity", "Updated: name=$userName, phone=$userPhone, city=$userCity, pincode=$userPincode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile)
            Log.d("ProfileActivity", "onCreate: Activity started")
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error inflating layout: ${e.message}", e)
            Toast.makeText(this, "Error loading screen", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        nameTextView = findViewById(R.id.nameTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        addressTextView = findViewById(R.id.addressTextView)
        profileImageView = findViewById(R.id.profileImageView)
        editProfileButton = findViewById(R.id.editButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Get intent extras as fallbacks
        userName = intent.getStringExtra("userName") ?: "User"
        userPhone = intent.getStringExtra("phone") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        userType = intent.getStringExtra("userType") ?: "customer"

        // Update UI with intent data initially
        updateUI()

        // Fetch user data if authenticated
        val user = auth.currentUser
        if (user != null && firebaseUid.isNotEmpty()) {
            fetchUserData(firebaseUid)
        } else {
            Log.w("ProfileActivity", "User not authenticated or no UID")
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        editProfileButton.setOnClickListener {
            Log.d("ProfileActivity", "Launching EditProfileActivity")
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("phone", userPhone)
            intent.putExtra("city", userCity)
            intent.putExtra("pincode", userPincode)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userType", userType)
            editProfileLauncher.launch(intent)
        }

        setupBottomNavigation()
    }

    private fun updateUI() {
        nameTextView.text = "Name: $userName"
        phoneTextView.text = "Phone: ${if (userPhone == "Unknown") "Not set" else userPhone}"
        addressTextView.text = "Location: ${if (userCity == "Unknown") "Not set" else "$userCity, $userPincode"}"
    }

    private fun fetchUserData(firebaseUid: String, retryCount: Int = 0) {
        if (isFetchingUserData) return
        isFetchingUserData = true

        lifecycleScope.launch {
            try {
                val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    apiService.getUser(firebaseUid).execute()
                }
                isFetchingUserData = false
                if (isFinishing || isDestroyed) return@launch
                if (response.isSuccessful) {
                    val userData = response.body()
                    userName = userData?.name ?: userName
                    userPhone = userData?.phone ?: userPhone
                    userCity = userData?.address?.city ?: userCity
                    userPincode = userData?.address?.pincode ?: userPincode
                    Log.d("ProfileActivity", "Fetched: name=$userName, phone=$userPhone, city=$userCity, pincode=$userPincode")
                    updateUI()
                    val profileImageUrl = userData?.profileImageUrl
                    Glide.with(this@ProfileActivity)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileImageView)
                } else {
                    Log.e("ProfileActivity", "Failed to fetch user: HTTP ${response.code()}")
                    if (retryCount < 2) {
                        delay(1000)
                        fetchUserData(firebaseUid, retryCount + 1)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        updateUI()
                    }
                }
            } catch (e: Exception) {
                isFetchingUserData = false
                if (isFinishing || isDestroyed) return@launch
                Log.e("ProfileActivity", "Network error fetching user: ${e.message}")
                if (retryCount < 2) {
                    delay(1000)
                    fetchUserData(firebaseUid, retryCount + 1)
                } else {
                    Toast.makeText(this@ProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("ProfileActivity", "Navigating to HomepageActivity")
                    val intent = Intent(this, HomepageActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("phone", userPhone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_my_listings -> {
                    Log.d("ProfileActivity", "Navigating to MyListingsActivity")
                    val intent = Intent(this, MyListingsActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("phone", userPhone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_create_listing -> {
                    Log.d("ProfileActivity", "Navigating to CreateListingActivity")
                    val intent = Intent(this, CreateListingActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("phone", userPhone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    true
                }
                R.id.nav_others -> {
                    Log.d("ProfileActivity", "Navigating to OthersActivity")
                    val intent = Intent(this, OthersActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("phone", userPhone)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", userType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ProfileActivity", "onDestroy: Activity destroyed")
    }
}