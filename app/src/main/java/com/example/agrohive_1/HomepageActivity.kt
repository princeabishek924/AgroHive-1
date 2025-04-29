package com.example.agrohive_1

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.agrohive_1.databinding.ActivityHomepageBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomepageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding
    private lateinit var auth: FirebaseAuth
    private var userType: String? = null
    private var userPincode: String = "Unknown"
    private var userCity: String = "Unknown"
    private var searchPincode: String = "Unknown"
    private var userName: String = "User"
    private var firebaseUid: String = ""

    private var isFetchingUserData = false
    private var allCities: List<CityPincode> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityHomepageBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("HomepageActivity", "onCreate: Activity started")
        } catch (e: Exception) {
            Log.e("HomepageActivity", "Error inflating layout: ${e.message}", e)
            Toast.makeText(this, "Error loading screen. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            binding.footerTextView.text = getString(R.string.footer_text)
        } catch (e: Exception) {
            Log.e("HomepageActivity", "Error setting footer text: ${e.message}", e)
            Toast.makeText(this, "Footer initialization failed", Toast.LENGTH_SHORT).show()
        }

        auth = FirebaseAuth.getInstance()

        binding.listingsRecyclerView.layoutManager = GridLayoutManager(this, 1)
        binding.listingsRecyclerView.addItemDecoration(SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.listing_spacing)))
        binding.listingsRecyclerView.adapter = ListingAdapter(emptyList(), userName)

        val user = auth.currentUser
        if (user != null) {
            firebaseUid = user.uid
            Log.d("HomepageActivity", "Firebase UID: $firebaseUid")

            fetchCitiesAndPincodes()
            fetchUserData(firebaseUid)

            val query = intent.getStringExtra("query") ?: ""
            val location = intent.getStringExtra("location") ?: userPincode
            val radius = intent.getStringExtra("radius") ?: "15km"
            val priceFrom = intent.getDoubleExtra("priceFrom", 0.0)
            val priceTo = intent.getDoubleExtra("priceTo", 999.0)
            val category = intent.getStringExtra("category") ?: "All"

            if (query.isNotEmpty()) {
                Log.d("HomepageActivity", "Received query: query='$query', location='$location', radius='$radius', priceFrom=$priceFrom, priceTo=$priceTo, category='$category'")
                binding.searchEditText.setText(query)
                fetchListings(query, location, radius, priceFrom, priceTo, category)
            }

            setupLocationSpinner()
            binding.filterButton.setOnClickListener { showFilterDialog() }
            setupChatbotFab()
            setupSearchBar()
            setupProfileNavigation()
        } else {
            Log.w("HomepageActivity", "User not authenticated")
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomepageActivity", "onResume: Checking user data")
        if (firebaseUid.isNotEmpty() && !isFetchingUserData) {
            fetchUserData(firebaseUid)
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = binding.searchEditText.compoundDrawables[2]
                if (drawableEnd != null) {
                    val drawableWidth = drawableEnd.bounds.width()
                    if (event.rawX >= (binding.searchEditText.right - drawableWidth - binding.searchEditText.paddingEnd)) {
                        val query = binding.searchEditText.text.toString().trim()
                        if (query.isNotEmpty()) {
                            val location = getSelectedLocation()
                            Log.d("HomepageActivity", "Search icon: query='$query', location='$location'")
                            fetchListings(query, location, "15km", 0.0, 999.0)
                            hideKeyboard()
                        } else {
                            Toast.makeText(this, "Enter a search query", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = binding.searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    val location = getSelectedLocation()
                    Log.d("HomepageActivity", "Search keyboard: query='$query', location='$location'")
                    fetchListings(query, location, "15km", 0.0, 999.0)
                    hideKeyboard()
                } else {
                    Toast.makeText(this, "Enter a search query", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        val location = getSelectedLocation()
                        Log.d("HomepageActivity", "Search text: query='$query', location='$location'")
                        fetchListings(query, location, "15km", 0.0, 999.0)
                    } else {
                        binding.listingsRecyclerView.adapter = ListingAdapter(emptyList(), userName)
                        binding.listingsFoundText.text = getString(R.string.listings_found, 0)
                        binding.listingsFoundText.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun setupProfileNavigation() {
        binding.profileImageView.setOnClickListener {
            Log.d("HomepageActivity", "Navigating to ProfileActivity")
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userPincode", userPincode)
            intent.putExtra("userCity", userCity)
            intent.putExtra("userType", userType)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun fetchCitiesAndPincodes() {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    apiService.getAllCities().execute()
                }
                if (response.isSuccessful) {
                    allCities = response.body() ?: emptyList()
                    Log.d("HomepageActivity", "Fetched ${allCities.size} cities")
                    if (userPincode != "Unknown") {
                        updateUserCity(userPincode)
                    }
                    setupLocationSpinner()
                } else {
                    Log.e("HomepageActivity", "Failed to fetch cities: HTTP ${response.code()}")
                    Toast.makeText(this@HomepageActivity, "Failed to load cities", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomepageActivity", "Network error fetching cities: ${e.message}")
                Toast.makeText(this@HomepageActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserCity(pincode: String) {
        val cityPincode = allCities.find { it.pincode == pincode }
        userCity = cityPincode?.city ?: "Unknown"
        userPincode = pincode
        searchPincode = userPincode
        Log.d("HomepageActivity", "User city: $userCity, pincode: $pincode")
        binding.locationLabel.text = getString(R.string.location_label, userCity, userPincode)
    }

    private fun fetchCityFromPincode(pincode: String, callback: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val pincodeApiService = PincodeApiService.create()
                val response = withContext(Dispatchers.IO) {
                    pincodeApiService.getPincodeData(pincode).execute()
                }
                if (response.isSuccessful) {
                    val city = response.body()?.firstOrNull()?.PostOffice?.firstOrNull()?.Name
                    Log.d("HomepageActivity", "Fetched city: $city for pincode: $pincode")
                    callback(city)
                } else {
                    Log.e("HomepageActivity", "Failed to fetch city: HTTP ${response.code()}")
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e("HomepageActivity", "Network error fetching city: ${e.message}")
                callback(null)
            }
        }
    }

    private fun setupChatbotFab() {
        binding.chatbotFab.setOnClickListener {
            Log.d("HomepageActivity", "Navigating to ChatbotActivity")
            val intent = Intent(this, ChatbotActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("userPincode", userPincode)
            intent.putExtra("userCity", userCity)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userType", userType)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun fetchUserData(firebaseUid: String, retryCount: Int = 0) {
        if (isFetchingUserData) return
        isFetchingUserData = true
        binding.userNameTextView.text = getString(R.string.loading)

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
                    userName = userData?.name ?: "User"
                    userType = userData?.userType?.lowercase() ?: "customer"
                    binding.userNameTextView.text = userName
                    userPincode = userData?.address?.pincode ?: "Unknown"
                    Log.d("HomepageActivity", "User: name='$userName', pincode='$userPincode', type='$userType'")
                    updateUserCity(userPincode)
                    val profileImageUrl = userData?.profileImageUrl
                    Glide.with(this@HomepageActivity)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.profileImageView)

                    if (intent.getStringExtra("query")?.isNotEmpty() != true) {
                        fetchListings("", userPincode, "15km", 0.0, 999.0)
                    }
                    setupBottomNavigation()
                } else {
                    Log.e("HomepageActivity", "Failed to fetch user: HTTP ${response.code()}")
                    if (retryCount < 2) {
                        delay(1000)
                        fetchUserData(firebaseUid, retryCount + 1)
                    } else {
                        Toast.makeText(this@HomepageActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        userName = "User"
                        binding.userNameTextView.text = userName
                        userPincode = "Unknown"
                        fetchListings("", userPincode, "15km", 0.0, 999.0)
                        userType = "customer"
                        setupBottomNavigation()
                    }
                }
            } catch (e: Exception) {
                isFetchingUserData = false
                if (isFinishing || isDestroyed) return@launch
                Log.e("HomepageActivity", "Network error fetching user: ${e.message}")
                if (retryCount < 2) {
                    delay(1000)
                    fetchUserData(firebaseUid, retryCount + 1)
                } else {
                    Toast.makeText(this@HomepageActivity, "Network error", Toast.LENGTH_SHORT).show()
                    userName = "User"
                    binding.userNameTextView.text = userName
                    userPincode = "Unknown"
                    fetchListings("", userPincode, "15km", 0.0, 999.0)
                    userType = "customer"
                    setupBottomNavigation()
                }
            }
        }
    }

    private fun fetchListings(
        query: String,
        location: String,
        radius: String,
        priceFrom: Double? = 0.0,
        priceTo: Double? = 999.0,
        category: String = "All",
        retryCount: Int = 0
    ) {
        if (firebaseUid.isEmpty()) {
            Log.w("HomepageActivity", "No firebaseUid")
            binding.listingsFoundText.text = getString(R.string.user_auth_required)
            binding.listingsFoundText.visibility = View.VISIBLE
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("HomepageActivity", "Fetching: query='$query', location='$location', radius='$radius', priceFrom=$priceFrom, priceTo=$priceTo, category='$category', uid='$firebaseUid'")
        binding.listingsFoundText.text = "Searching..."
        binding.listingsFoundText.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
                val response = withContext(Dispatchers.IO) {
                    apiService.getListings(query, location, radius, firebaseUid, priceFrom, priceTo, category).execute()
                }
                if (isFinishing || isDestroyed) return@launch
                if (response.isSuccessful) {
                    val listings = response.body() ?: emptyList()
                    Log.d("HomepageActivity", "Fetched ${listings.size} listings")
                    binding.listingsRecyclerView.adapter = ListingAdapter(listings, userName)
                    binding.listingsFoundText.text = getString(R.string.listings_found, listings.size)
                    binding.listingsFoundText.visibility = View.VISIBLE
                    if (listings.isEmpty() && query.isNotEmpty()) {
                        Toast.makeText(this@HomepageActivity, "No results for '$query'", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("HomepageActivity", "Fetch failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    binding.listingsFoundText.text = getString(R.string.no_listings_found, location, radius)
                    binding.listingsFoundText.visibility = View.VISIBLE
                    Toast.makeText(this@HomepageActivity, "Search error: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    if (retryCount < 1 && query.isNotEmpty()) {
                        delay(1000)
                        fetchListings("", location, radius, priceFrom, priceTo, category, 0)
                    }
                }
            } catch (e: Exception) {
                if (isFinishing || isDestroyed) return@launch
                Log.e("HomepageActivity", "Network error: ${e.message}")
                binding.listingsFoundText.text = getString(R.string.no_listings_found, location, radius)
                binding.listingsFoundText.visibility = View.VISIBLE
                Toast.makeText(this@HomepageActivity, "Network error", Toast.LENGTH_SHORT).show()
                if (retryCount < 1 && query.isNotEmpty()) {
                    delay(1000)
                    fetchListings("", location, radius, priceFrom, priceTo, category, 0)
                }
            }
        }
    }

    private fun setupLocationSpinner() {
        val locationOptions = listOf("Default", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.locationSpinner.adapter = adapter
        binding.locationSpinner.setSelection(0)

        binding.locationSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "Default") {
                    binding.locationInputLayout.visibility = View.GONE
                    searchPincode = userPincode
                    binding.locationLabel.text = getString(R.string.location_label, userCity, userPincode)
                    fetchListings("", userPincode, "15km", 0.0, 999.0)
                } else {
                    binding.locationInputLayout.visibility = View.VISIBLE
                    binding.locationPincodeEditText.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.locationPincodeEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        binding.doneButton.setOnClickListener {
            val pincode = binding.locationPincodeEditText.text.toString().trim()
            if (pincode.length == 6 && pincode.all { it.isDigit() }) {
                searchPincode = pincode
                fetchCityFromPincode(pincode) { city ->
                    val searchCity = city ?: "Unknown"
                    binding.locationLabel.text = getString(R.string.location_label, searchCity, pincode)
                    fetchListings("", pincode, "15km", 0.0, 999.0)
                }
                binding.locationInputLayout.visibility = View.GONE
                binding.locationPincodeEditText.text.clear()
                hideKeyboard()
            } else {
                Toast.makeText(this@HomepageActivity, "Enter a valid 6-digit pincode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedLocation(): String {
        return searchPincode.ifEmpty { userPincode }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val priceFromEditText = dialogView.findViewById<android.widget.EditText>(R.id.priceFromEditText)
        val priceToEditText = dialogView.findViewById<android.widget.EditText>(R.id.priceToEditText)
        val categorySpinner = dialogView.findViewById<android.widget.Spinner>(R.id.categorySpinner)
        val radiusSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.radiusSpinner)
        val applyButton = dialogView.findViewById<android.widget.Button>(R.id.applyFilterButton)

        val categories = listOf("All", "Fruits", "Vegetables", "Grains")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val radii = listOf("5km", "10km", "15km", "30km")
        val radiusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, radii)
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        radiusSpinner.adapter = radiusAdapter
        radiusSpinner.setSelection(radii.indexOf("15km"))

        applyButton.setOnClickListener {
            val priceFrom = priceFromEditText.text.toString().toDoubleOrNull() ?: 0.0
            val priceTo = priceToEditText.text.toString().toDoubleOrNull() ?: 999.0
            val category = categorySpinner.selectedItem.toString()
            val radius = radiusSpinner.selectedItem.toString()
            binding.filterLabel.text = getString(R.string.filter_label, radius, priceFrom, priceTo)
            val query = binding.searchEditText.text.toString().trim()
            val location = getSelectedLocation()
            Log.d("HomepageActivity", "Filters: location='$location', radius='$radius', priceFrom=$priceFrom, priceTo=$priceTo, category='$category'")
            fetchListings(query, location, radius, priceFrom, priceTo, category)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.menu.clear()
        binding.bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_customer)

        val currentUserType = userType ?: "customer"
        Log.d("HomepageActivity", "Bottom nav: userType='$currentUserType'")

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_my_listings -> {
                    Log.d("HomepageActivity", "Navigating to MyListingsActivity")
                    val intent = Intent(this, MyListingsActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userType", currentUserType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    Log.d("HomepageActivity", "Navigating to ProfileActivity")
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userType", currentUserType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_chat -> {
                    Log.d("HomepageActivity", "Navigating to messageactivity")
                    val intent = Intent(this, MessageActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("userType", currentUserType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_others -> {
                    Log.d("HomepageActivity", "Navigating to OthersActivity")
                    val intent = Intent(this, OthersActivity::class.java)
                    intent.putExtra("userName", userName)
                    intent.putExtra("userPincode", userPincode)
                    intent.putExtra("userCity", userCity)
                    intent.putExtra("firebaseUid", firebaseUid)
                    intent.putExtra("userType", currentUserType)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HomepageActivity", "onDestroy")
    }
}