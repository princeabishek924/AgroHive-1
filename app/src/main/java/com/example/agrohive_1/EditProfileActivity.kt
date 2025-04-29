package com.example.agrohive_1

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rootLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var mainContainer: LinearLayout
    private lateinit var formContainer: LinearLayout
    private lateinit var headerText: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var pincodeEditText: EditText
    private lateinit var cityTextView: TextView
    private lateinit var districtTextView: TextView
    private lateinit var stateTextView: TextView
    private lateinit var newPasswordEditText: CustomEditText
    private lateinit var confirmPasswordEditText: CustomEditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var footerTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var uploadImageButton: Button
    private lateinit var usernameAvailabilityText: TextView
    private var profileImageUrl: String? = null
    private var isPasswordVisible = false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var userName: String? = null
    private var firebaseUid: String = ""
    private var userType: String = "customer"
    private var phone: String = "Unknown"
    private var userPincode: String = "Unknown"

    private val handler = Handler(Looper.getMainLooper())
    private var pincodeRunnable: Runnable? = null

    private var selectedCity: String = "Select City"
    private var selectedDistrict: String = "Select District"
    private var selectedState: String = "Select State"
    private var userCity: String = ""
    private var userDistrict: String = ""
    private var userState: String = ""
    private var isProgrammaticTextChange = false

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> uploadImageToBackend(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppSettings()
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_edit_profile)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading screen", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d("EditProfileActivity", "onCreate: Activity started")

        auth = FirebaseAuth.getInstance()
        rootLayout = findViewById(R.id.root_layout)
        mainContainer = findViewById(R.id.mainContainer)
        formContainer = findViewById(R.id.formContainer)
        headerText = findViewById(R.id.headerText)
        profileImageView = findViewById(R.id.edit_profile_image)
        nameEditText = findViewById(R.id.nameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        pincodeEditText = findViewById(R.id.pincodeEditText)
        cityTextView = findViewById(R.id.cityTextView)
        districtTextView = findViewById(R.id.districtTextView)
        stateTextView = findViewById(R.id.stateTextView)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        saveButton = findViewById(R.id.saveButton)
        progressBar = findViewById(R.id.progressBar)
        footerTextView = findViewById(R.id.footerTextView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        usernameAvailabilityText = findViewById(R.id.usernameAvailabilityText)

        // Ensure BottomNavigationView is visible
        bottomNavigationView.visibility = View.VISIBLE
        Log.d("EditProfileActivity", "BottomNavigationView initialized, visibility: ${bottomNavigationView.visibility}")

        // Apply theme and font size
        applyThemeAndFontSize()

        // Get intent extras
        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        userName = intent.getStringExtra("userName") ?: "User"
        userType = intent.getStringExtra("userType")?.lowercase() ?: "customer"
        phone = intent.getStringExtra("phone") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        profileImageUrl = intent.getStringExtra("profileImageUrl")

        setupBottomNavigation()
        fetchUserData()

        profileImageView.setOnClickListener { selectImage() }
        uploadImageButton.setOnClickListener { selectImage() }

        // Add focus change listeners for debugging
        nameEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "Name EditText focus: $hasFocus") }
        usernameEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "Username EditText focus: $hasFocus") }
        phoneEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "Phone EditText focus: $hasFocus") }
        pincodeEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "Pincode EditText focus: $hasFocus") }
        newPasswordEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "New Password EditText focus: $hasFocus") }
        confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus -> Log.d("EditProfileActivity", "Confirm Password EditText focus: $hasFocus") }

        // Add touch listeners for debugging
        nameEditText.setOnTouchListener { _, event -> Log.d("EditProfileActivity", "Name EditText touched: ${event.action}"); false }
        usernameEditText.setOnTouchListener { _, event -> Log.d("EditProfileActivity", "Username EditText touched: ${event.action}"); false }
        phoneEditText.setOnTouchListener { _, event -> Log.d("EditProfileActivity", "Phone EditText touched: ${event.action}"); false }
        pincodeEditText.setOnTouchListener { _, event -> Log.d("EditProfileActivity", "Pincode EditText touched: ${event.action}"); false }

        // Debounced TextWatcher for pincode
        pincodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChange) {
                    Log.d("EditProfileActivity", "Programmatic text change detected, skipping TextWatcher")
                    return
                }
                pincodeRunnable?.let { handler.removeCallbacks(it) }
                pincodeRunnable = Runnable {
                    val pincode = s.toString().trim()
                    if (pincode.length == 6 && pincode.matches(Regex("^[0-9]{6}$"))) {
                        fetchPincodeData(pincode, userCity, userDistrict, userState)
                    } else {
                        cityTextView.text = "Select City"
                        districtTextView.text = "Select District"
                        stateTextView.text = "Select State"
                        selectedCity = "Select City"
                        selectedDistrict = "Select District"
                        selectedState = "Select State"
                        cityTextView.visibility = View.GONE
                        districtTextView.visibility = View.GONE
                        stateTextView.visibility = View.GONE
                    }
                }
                handler.postDelayed(pincodeRunnable!!, 500)
            }
        })

        // Real-time username availability check
        usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val username = s.toString().trim()
                if (username.isNotEmpty()) {
                    checkUsernameAvailability(username)
                } else {
                    usernameAvailabilityText.text = "Username cannot be empty"
                    usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }
        })

        // Set up click listeners for TextViews
        cityTextView.setOnClickListener { showPopupMenu(cityTextView, cityTextView.tag as? List<String> ?: listOf()) }
        districtTextView.setOnClickListener { showPopupMenu(districtTextView, districtTextView.tag as? List<String> ?: listOf()) }
        stateTextView.setOnClickListener { showPopupMenu(stateTextView, stateTextView.tag as? List<String> ?: listOf()) }

        newPasswordEditText.setOnTouchListener { _, event ->
            val drawableEnd = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (newPasswordEditText.right - newPasswordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                    togglePasswordVisibility()
                    newPasswordEditText.performClick()
                    return@setOnTouchListener true
                }
            }
            Log.d("EditProfileActivity", "New Password EditText touched: ${event.action}")
            false
        }

        confirmPasswordEditText.setOnTouchListener { _, event ->
            val drawableEnd = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (confirmPasswordEditText.right - confirmPasswordEditText.compoundDrawables[drawableEnd].bounds.width())) {
                    togglePasswordVisibility()
                    confirmPasswordEditText.performClick()
                    return@setOnTouchListener true
                }
            }
            Log.d("EditProfileActivity", "Confirm Password EditText touched: ${event.action}")
            false
        }

        saveButton.setOnClickListener {
            Log.d("EditProfileActivity", "Save button clicked")
            val name = nameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val pincode = pincodeEditText.text.toString().trim()
            val city = selectedCity
            val district = selectedDistrict
            val state = selectedState
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            Log.d("EditProfileActivity", "Fields: name=$name, username=$username, phone=$phone, pincode=$pincode, city=$city, district=$district, state=$state, newPassword=$newPassword, confirmPassword=$confirmPassword")

            // Validate only changed or required fields
            if (username.isNotEmpty() && usernameAvailabilityText.text != "Username is available") {
                Toast.makeText(this, "Please choose an available username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Only proceed if at least one field is changed or password is updated
            val hasChanges = name != (nameEditText.tag as? String ?: "") ||
                    username != (usernameEditText.tag as? String ?: "") ||
                    phone != (phoneEditText.tag as? String ?: "") ||
                    pincode != (pincodeEditText.tag as? String ?: "") ||
                    city != (cityTextView.tag as? String ?: "Select City") ||
                    district != (districtTextView.tag as? String ?: "Select District") ||
                    state != (stateTextView.tag as? String ?: "Select State") ||
                    newPassword.isNotEmpty()

            if (!hasChanges) {
                Toast.makeText(this, "No changes detected to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            saveButton.isEnabled = false

            fetchCoordinates(pincode) { lat, lng ->
                latitude = lat
                longitude = lng
                updateUserData(name, username, phone, pincode, city, district, state, newPassword)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyThemeAndFontSize()
        // Ensure BottomNavigationView menu is visible
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationView.selectedItemId = R.id.nav_profile
        Log.d("EditProfileActivity", "onResume: BottomNavigationView set to nav_profile")
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        Log.d("EditProfileActivity", "Back button pressed")
        Toast.makeText(this, "Press back again to exit Edit Profile", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        Log.d("EditProfileActivity", "onPause: Activity paused")
    }

    override fun onStop() {
        super.onStop()
        Log.d("EditProfileActivity", "onStop: Activity stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        pincodeRunnable?.let { handler.removeCallbacks(it) }
        Log.d("EditProfileActivity", "onDestroy: Activity destroyed")
    }

    private fun applyAppSettings() {
        val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val theme = sharedPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }

    private fun applyThemeAndFontSize() {
        val sharedPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val theme = sharedPrefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val fontSize = sharedPrefs.getString("fontSize", "medium") ?: "medium"

        // Apply font size
        val textSize = when (fontSize) {
            "small" -> 14f
            "medium" -> 16f
            "large" -> 18f
            else -> 16f
        }
        headerText.textSize = textSize + 4 // Larger for header
        nameEditText.textSize = textSize
        usernameEditText.textSize = textSize
        usernameAvailabilityText.textSize = textSize - 2 // Smaller for feedback
        phoneEditText.textSize = textSize
        pincodeEditText.textSize = textSize
        cityTextView.textSize = textSize
        districtTextView.textSize = textSize
        stateTextView.textSize = textSize
        newPasswordEditText.textSize = textSize
        confirmPasswordEditText.textSize = textSize
        uploadImageButton.textSize = textSize
        saveButton.textSize = textSize
        footerTextView.textSize = textSize - 2 // Smaller for footer

        // Apply font size to BottomNavigationView text
        val textAppearance = when (fontSize) {
            "small" -> 14f
            "medium" -> 16f
            "large" -> 18f
            else -> 16f
        }


        // Apply theme
        val (backgroundColor, textColor, hintColor) = when (theme) {
            AppCompatDelegate.MODE_NIGHT_YES -> Triple(
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.holo_green_dark), // Green text in Dark theme
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
            else -> Triple(
                ContextCompat.getColor(this, android.R.color.white),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
        rootLayout.setBackgroundColor(backgroundColor)
        mainContainer.setBackgroundColor(backgroundColor)
        formContainer.setBackgroundColor(backgroundColor)
        headerText.setTextColor(textColor)
        nameEditText.setTextColor(textColor)
        nameEditText.setHintTextColor(hintColor)
        usernameEditText.setTextColor(textColor)
        usernameEditText.setHintTextColor(hintColor)
        usernameAvailabilityText.setTextColor(textColor)
        phoneEditText.setTextColor(textColor)
        phoneEditText.setHintTextColor(hintColor)
        pincodeEditText.setTextColor(textColor)
        pincodeEditText.setHintTextColor(hintColor)
        cityTextView.setTextColor(textColor)
        cityTextView.setHintTextColor(hintColor)
        districtTextView.setTextColor(textColor)
        districtTextView.setHintTextColor(hintColor)
        stateTextView.setTextColor(textColor)
        stateTextView.setHintTextColor(hintColor)
        newPasswordEditText.setTextColor(textColor)
        newPasswordEditText.setHintTextColor(hintColor)
        confirmPasswordEditText.setTextColor(textColor)
        confirmPasswordEditText.setHintTextColor(hintColor)
        uploadImageButton.setTextColor(textColor)
        saveButton.setTextColor(textColor)
        footerTextView.setTextColor(textColor)
        // Apply text/icon tint to BottomNavigationView, but keep background as dark_green (set in XML)
        bottomNavigationView.itemIconTintList = android.content.res.ColorStateList.valueOf(textColor)
        bottomNavigationView.itemTextColor = android.content.res.ColorStateList.valueOf(textColor)
        Log.d("EditProfileActivity", "applyThemeAndFontSize: BottomNavigationView tint set to $textColor, background retained as dark_green from XML")
    }

    private fun showPopupMenu(textView: TextView, items: List<String>) {
        val popup = PopupMenu(this, textView)
        items.forEachIndexed { index, item ->
            popup.menu.add(0, index, 0, item)
        }
        popup.setOnMenuItemClickListener { menuItem ->
            textView.text = menuItem.title
            when (textView.id) {
                R.id.cityTextView -> selectedCity = menuItem.title.toString()
                R.id.districtTextView -> selectedDistrict = menuItem.title.toString()
                R.id.stateTextView -> selectedState = menuItem.title.toString()
            }
            Log.d("EditProfileActivity", "Selected ${textView.contentDescription}: ${menuItem.title}")
            true
        }
        popup.show()
    }

    private fun fetchUserData() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("EditProfile", "No authenticated user")
            Toast.makeText(this, "Please log in to edit profile", Toast.LENGTH_LONG).show()
            return
        }

        apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                    return
                }
                if (response.isSuccessful) {
                    val userData = response.body()
                    nameEditText.setText(userData?.name ?: "")
                    nameEditText.tag = userData?.name ?: ""
                    usernameEditText.setText(userData?.username ?: "")
                    usernameEditText.tag = userData?.username ?: ""
                    phoneEditText.setText(userData?.phone ?: "")
                    phoneEditText.tag = userData?.phone ?: ""
                    isProgrammaticTextChange = true
                    pincodeEditText.setText(userData?.address?.pincode ?: "")
                    pincodeEditText.tag = userData?.address?.pincode ?: ""
                    isProgrammaticTextChange = false
                    profileImageUrl = userData?.profileImageUrl
                    latitude = userData?.address?.latitude ?: 0.0
                    longitude = userData?.address?.longitude ?: 0.0

                    userCity = userData?.address?.city ?: ""
                    userDistrict = userData?.address?.district ?: ""
                    userState = userData?.address?.state ?: ""

                    Log.d("EditProfile", "User data fetched: city=$userCity, district=$userDistrict, state=$userState")

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.edit_profile)
                            .into(profileImageView)
                    }

                    val pincode = userData?.address?.pincode ?: ""
                    if (pincode.isNotEmpty()) {
                        fetchPincodeData(pincode, userCity, userDistrict, userState)
                        cityTextView.tag = userCity
                        districtTextView.tag = userDistrict
                        stateTextView.tag = userState
                    }
                } else {
                    Log.e("EditProfile", "Failed to fetch user: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfileActivity, "Failed to load user data. Please try again.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                    return
                }
                Log.e("EditProfile", "Network error fetching user: ${t.message}")
                Toast.makeText(this@EditProfileActivity, "Network error: ${t.message}. Please try again.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun fetchPincodeData(pincode: String, userCity: String = "", userDistrict: String = "", userState: String = "") {
        if (pincode.isEmpty()) {
            Log.d("EditProfile", "Pincode is empty, resetting selectors")
            cityTextView.text = "Select City"
            districtTextView.text = "Select District"
            stateTextView.text = "Select State"
            selectedCity = "Select City"
            selectedDistrict = "Select District"
            selectedState = "Select State"
            cityTextView.visibility = View.GONE
            districtTextView.visibility = View.GONE
            stateTextView.visibility = View.GONE
            return
        }

        Log.d("EditProfile", "Fetching pincode data for: $pincode")
        val apiService = PincodeApiService.create()
        apiService.getPincodeData(pincode).enqueue(object : Callback<List<PincodeResponse>> {
            override fun onResponse(call: Call<List<PincodeResponse>>, response: Response<List<PincodeResponse>>) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                    return
                }
                if (response.isSuccessful) {
                    val pincodeData = response.body()?.firstOrNull()
                    Log.d("EditProfile", "Pincode API response: $pincodeData")
                    if (pincodeData != null && pincodeData.Status == "Success" && !pincodeData.PostOffice.isNullOrEmpty()) {
                        val offices = pincodeData.PostOffice
                        val cities = offices.map { it.Name }.distinct().sorted()
                        val districts = offices.map { it.District }.distinct().sorted()
                        val states = offices.map { it.State }.distinct().sorted()

                        Log.d("EditProfile", "Cities: $cities, Districts: $districts, States: $states")
                        cityTextView.tag = cities
                        districtTextView.tag = districts
                        stateTextView.tag = states

                        if (userCity.isNotEmpty() && cities.contains(userCity)) {
                            cityTextView.text = userCity
                            selectedCity = userCity
                        } else {
                            cityTextView.text = "Select City"
                            selectedCity = "Select City"
                        }

                        if (userDistrict.isNotEmpty() && districts.contains(userDistrict)) {
                            districtTextView.text = userDistrict
                            selectedDistrict = userDistrict
                        } else {
                            districtTextView.text = "Select District"
                            selectedDistrict = "Select District"
                        }

                        if (userState.isNotEmpty() && states.contains(userState)) {
                            stateTextView.text = userState
                            selectedState = userState
                        } else {
                            stateTextView.text = "Select State"
                            selectedState = "Select State"
                        }

                        cityTextView.visibility = View.VISIBLE
                        districtTextView.visibility = View.VISIBLE
                        stateTextView.visibility = View.VISIBLE
                        Log.d("EditProfile", "Selectors set to VISIBLE with values: city=$selectedCity, district=$selectedDistrict, state=$selectedState")
                    } else {
                        Log.w("EditProfile", "Invalid pincode: $pincode, resetting selectors")
                        Toast.makeText(this@EditProfileActivity, "Invalid pincode", Toast.LENGTH_SHORT).show()
                        cityTextView.text = "Select City"
                        districtTextView.text = "Select District"
                        stateTextView.text = "Select State"
                        selectedCity = "Select City"
                        selectedDistrict = "Select District"
                        selectedState = "Select State"
                        cityTextView.visibility = View.GONE
                        districtTextView.visibility = View.GONE
                        stateTextView.visibility = View.GONE
                    }
                } else {
                    Log.e("EditProfile", "Pincode fetch failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfileActivity, "Failed to fetch pincode data", Toast.LENGTH_SHORT).show()
                    cityTextView.text = "Select City"
                    districtTextView.text = "Select District"
                    stateTextView.text = "Select State"
                    selectedCity = "Select City"
                    selectedDistrict = "Select District"
                    selectedState = "Select State"
                    cityTextView.visibility = View.GONE
                    districtTextView.visibility = View.GONE
                    stateTextView.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<List<PincodeResponse>>, t: Throwable) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                    return
                }
                Log.e("EditProfile", "Network error fetching pincode: ${t.message}")
                Toast.makeText(this@EditProfileActivity, "Network error fetching pincode data. Please try again.", Toast.LENGTH_SHORT).show()
                cityTextView.text = "Select City"
                districtTextView.text = "Select District"
                stateTextView.text = "Select State"
                selectedCity = "Select City"
                selectedDistrict = "Select District"
                selectedState = "Select State"
                cityTextView.visibility = View.GONE
                districtTextView.visibility = View.GONE
                stateTextView.visibility = View.GONE
            }
        })
    }

    private fun fetchCoordinates(pincode: String, callback: (Double, Double) -> Unit) {
        val apiService = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoApiService::class.java)

        apiService.getCoordinates(pincode, "json").enqueue(object : Callback<List<GeoResponse>> {
            override fun onResponse(call: Call<List<GeoResponse>>, response: Response<List<GeoResponse>>) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                    return
                }
                if (response.isSuccessful) {
                    val geoData = response.body()?.firstOrNull()
                    val lat = geoData?.lat?.toDoubleOrNull() ?: 0.0
                    val lng = geoData?.lon?.toDoubleOrNull() ?: 0.0
                    Log.d("Geo", "Coordinates fetched for $pincode: lat=$lat, lng=$lng")
                    callback(lat, lng)
                } else {
                    Log.e("Geo", "Failed to fetch coordinates: HTTP ${response.code()}")
                    callback(0.0, 0.0)
                }
            }

            override fun onFailure(call: Call<List<GeoResponse>>, t: Throwable) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                    return
                }
                Log.e("Geo", "Network error fetching coordinates: ${t.message}")
                callback(0.0, 0.0)
            }
        })
    }

    private fun updateUserData(name: String, username: String, phone: String, pincode: String, city: String, district: String, state: String, newPassword: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w("EditProfile", "No authenticated user")
            Toast.makeText(this, "Please log in to update profile", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            saveButton.isEnabled = true
            return
        }

        apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                    return
                }
                if (response.isSuccessful) {
                    val currentUser = response.body()
                    val updatedUser = UserResponse(
                        name = if (name.isNotEmpty()) name else currentUser?.name ?: "",
                        username = if (username.isNotEmpty()) username else currentUser?.username ?: "",
                        phone = if (phone.isNotEmpty()) phone else currentUser?.phone ?: "",
                        address = Address(
                            doorNo = currentUser?.address?.doorNo ?: "",
                            street = currentUser?.address?.street ?: "",
                            city = if (city != "Select City") city else currentUser?.address?.city ?: "",
                            district = if (district != "Select District") district else currentUser?.address?.district ?: "",
                            state = if (state != "Select State") state else currentUser?.address?.state ?: "",
                            country = currentUser?.address?.country ?: "India",
                            pincode = if (pincode.isNotEmpty()) pincode else currentUser?.address?.pincode ?: "",
                            area = currentUser?.address?.area ?: "",
                            latitude = latitude,
                            longitude = longitude
                        ),
                        profileImageUrl = profileImageUrl ?: currentUser?.profileImageUrl ?: ""
                    )

                    if (newPassword.isNotEmpty()) {
                        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                            if (isFinishing || isDestroyed) {
                                Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping password update")
                                return@addOnCompleteListener
                            }
                            if (!task.isSuccessful) {
                                Toast.makeText(this@EditProfileActivity, "Password update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                                saveButton.isEnabled = true
                                return@addOnCompleteListener
                            }
                            apiService.updateUser(uid, updatedUser.copy(password = newPassword)).enqueue(object : Callback<UserResponse> {
                                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                    handleUpdateResponse(response, updatedUser.name, updatedUser.username ?: "")
                                }

                                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                    handleUpdateFailure(t)
                                }
                            })
                        }
                    } else {
                        apiService.updateUser(uid, updatedUser).enqueue(object : Callback<UserResponse> {
                            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                handleUpdateResponse(response, updatedUser.name, updatedUser.username ?: "")
                            }

                            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                handleUpdateFailure(t)
                            }
                        })
                    }
                } else {
                    Log.e("EditProfile", "Failed to fetch current user: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfileActivity, "Failed to fetch current user data", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                    return
                }
                Log.e("EditProfile", "Network error fetching current user: ${t.message}")
                Toast.makeText(this@EditProfileActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
            }
        })
    }

    private fun handleUpdateResponse(response: Response<UserResponse>, updatedName: String, updatedUsername: String) {
        if (isFinishing || isDestroyed) {
            Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping handleUpdateResponse")
            return
        }
        if (response.isSuccessful) {
            val updatedUser = response.body()
            Log.d("EditProfile", "Profile updated with image URL: ${updatedUser?.profileImageUrl}")
            Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            userName = updatedName
            val intent = Intent(this, OthersActivity::class.java).apply {
                putExtra("firebaseUid", firebaseUid)
                putExtra("userName", updatedName)
                putExtra("userType", userType)
                putExtra("phone", updatedUser?.phone ?: phone)
                putExtra("userCity", updatedUser?.address?.city ?: userCity)
                putExtra("userPincode", updatedUser?.address?.pincode ?: userPincode)
                putExtra("profileImageUrl", updatedUser?.profileImageUrl ?: profileImageUrl)
            }
            startActivity(intent)
            finish()
        } else {
            Log.e("EditProfile", "Failed to update profile: HTTP ${response.code()} - ${response.errorBody()?.string()}")
            Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
        }
        progressBar.visibility = View.GONE
        saveButton.isEnabled = true
    }

    private fun handleUpdateFailure(t: Throwable) {
        if (isFinishing || isDestroyed) {
            Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping handleUpdateFailure")
            return
        }
        Log.e("EditProfile", "Network error updating profile: ${t.message}")
        Toast.makeText(this@EditProfileActivity, "Network error: ${t.message}. Please try again.", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.GONE
        saveButton.isEnabled = true
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePicker.launch(intent)
    }

    private fun uploadImageToBackend(imageUri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = bitmap.scale(200, 200, filter = true)
            profileImageView.setImageBitmap(scaledBitmap)

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
            apiService.uploadImage(mapOf("imageBase64" to base64Image)).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>?) {
                    if (isFinishing || isDestroyed) {
                        Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                        return
                    }
                    if (response?.isSuccessful == true) {
                        profileImageUrl = response.body()?.get("url")
                        Log.d("EditProfile", "Image uploaded: $profileImageUrl")
                        Toast.makeText(this@EditProfileActivity, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("EditProfile", "Image upload failed: HTTP ${response?.code()} - ${response?.errorBody()?.string()}")
                        Toast.makeText(this@EditProfileActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    if (isFinishing || isDestroyed) {
                        Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                        return
                    }
                    Log.e("EditProfile", "Network error uploading image: ${t.message}")
                    Toast.makeText(this@EditProfileActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e("EditProfile", "Error uploading image: ${e.message}")
            Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            newPasswordEditText.transformationMethod = null
            confirmPasswordEditText.transformationMethod = null
            newPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
            confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
        } else {
            newPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            confirmPasswordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            newPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
            confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
        }
        newPasswordEditText.setSelection(newPasswordEditText.text?.length ?: 0)
        confirmPasswordEditText.setSelection(confirmPasswordEditText.text?.length ?: 0)
    }

    private fun checkUsernameAvailability(username: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.checkUsernameAvailability(username).enqueue(object : Callback<Map<String, Boolean>> {
            override fun onResponse(call: Call<Map<String, Boolean>>, response: Response<Map<String, Boolean>>) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onResponse")
                    return
                }
                if (response.isSuccessful) {
                    val isAvailable = response.body()?.get("available") ?: false
                    if (isAvailable) {
                        usernameAvailabilityText.text = "Username is available"
                        usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    } else {
                        usernameAvailabilityText.text = "Username is taken"
                        usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    }
                } else {
                    Log.e("EditProfile", "Failed to check username: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    usernameAvailabilityText.text = "Error checking availability"
                    usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            }

            override fun onFailure(call: Call<Map<String, Boolean>>, t: Throwable) {
                if (isFinishing || isDestroyed) {
                    Log.w("EditProfileActivity", "Activity is finishing or destroyed, skipping onFailure")
                    return
                }
                Log.e("EditProfile", "Network error: ${t.message}")
                usernameAvailabilityText.text = "Network error"
                usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
        })
    }

    private fun setupBottomNavigation() {
        // Determine menu resource based on userType
        val menuRes = when (userType.lowercase()) {
            "farmer" -> R.menu.bottom_nav_menu_farmer
            "marketer" -> R.menu.bottom_nav_menu_customer
            else -> R.menu.bottom_nav_menu
        }

        // Explicitly inflate the menu
        try {
            bottomNavigationView.menu.clear()
            bottomNavigationView.inflateMenu(menuRes)
            Log.d("EditProfileActivity", "BottomNavigationView menu inflated: $menuRes for userType: $userType")
        } catch (e: Exception) {
            Log.e("EditProfileActivity", "Failed to inflate menu $menuRes: ${e.message}")
            Toast.makeText(this, "Error loading navigation menu", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure visibility
        bottomNavigationView.visibility = View.VISIBLE
        Log.d("EditProfileActivity", "BottomNavigationView set to VISIBLE")

        bottomNavigationView.setOnItemSelectedListener { item ->
            Log.d("EditProfileActivity", "BottomNavigationView item selected: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("EditProfileActivity", "Navigating to HomepageActivity")
                    val intent = Intent(this, HomepageActivity::class.java).apply {
                        putExtra("firebaseUid", firebaseUid)
                        putExtra("userName", userName)
                        putExtra("userType", userType)
                        putExtra("phone", phone)
                        putExtra("userCity", userCity)
                        putExtra("userPincode", userPincode)
                        putExtra("profileImageUrl", profileImageUrl)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_my_listings -> {
                    if (userType == "farmer") {
                        Log.d("EditProfileActivity", "Navigating to MyListingsActivity")
                        val intent = Intent(this, MyListingsActivity::class.java).apply {
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userName", userName)
                            putExtra("userType", userType)
                            putExtra("phone", phone)
                            putExtra("userCity", userCity)
                            putExtra("userPincode", userPincode)
                            putExtra("profileImageUrl", profileImageUrl)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                        true
                    } else {
                        Log.w("EditProfileActivity", "My Listings not available for $userType")
                        false
                    }
                }
                R.id.nav_create_listing -> {
                    if (userType == "farmer") {
                        Log.d("EditProfileActivity", "Navigating to CreateListingActivity")
                        val intent = Intent(this, CreateListingActivity::class.java).apply {
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userName", userName)
                            putExtra("userType", userType)
                            putExtra("phone", phone)
                            putExtra("userCity", userCity)
                            putExtra("userPincode", userPincode)
                            putExtra("profileImageUrl", profileImageUrl)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                        true
                    } else {
                        Log.w("EditProfileActivity", "Create Listing not available for $userType")
                        false
                    }
                }

                R.id.nav_profile -> {
                    Log.d("EditProfileActivity", "Already in EditProfileActivity, no action needed")
                    true
                }
                R.id.nav_others -> {
                    Log.d("EditProfileActivity", "Navigating to OthersActivity")
                    val intent = Intent(this, OthersActivity::class.java).apply {
                        putExtra("firebaseUid", firebaseUid)
                        putExtra("userName", userName)
                        putExtra("userType", userType)
                        putExtra("phone", phone)
                        putExtra("userCity", userCity)
                        putExtra("userPincode", userPincode)
                        putExtra("profileImageUrl", profileImageUrl)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    Log.w("EditProfileActivity", "Unknown menu item selected: ${item.itemId}")
                    false
                }
            }
        }

        // Set selected item
        try {
            bottomNavigationView.selectedItemId = R.id.nav_profile
            Log.d("EditProfileActivity", "BottomNavigationView selected item set to nav_profile")
        } catch (e: Exception) {
            Log.e("EditProfileActivity", "Failed to set selected item nav_profile: ${e.message}")
            Toast.makeText(this, "Error selecting profile navigation", Toast.LENGTH_SHORT).show()
        }
    }
}