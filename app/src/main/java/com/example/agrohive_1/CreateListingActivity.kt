package com.example.agrohive_1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class CreateListingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var productNameEditText: EditText
    private lateinit var productImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var priceEditText: EditText
    private lateinit var quantityEditText: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var locationSpinner: Spinner
    private lateinit var pincodeEditText: EditText
    private lateinit var pincodeLocationAutoComplete: AutoCompleteTextView
    private lateinit var listButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pincodeCityAdapter: ArrayAdapter<String>

    private var productImageUrl: String? = null
    private var isImageUploaded = false
    private var userLocation: String = "Unknown"

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> uploadImageToBackend(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_listing)

        auth = FirebaseAuth.getInstance()
        productNameEditText = findViewById(R.id.productNameEditText)
        productImageView = findViewById(R.id.productImageView)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        priceEditText = findViewById(R.id.priceEditText)
        quantityEditText = findViewById(R.id.quantityEditText)
        unitSpinner = findViewById(R.id.unitSpinner)
        categorySpinner = findViewById(R.id.categorySpinner)
        locationSpinner = findViewById(R.id.locationSpinner)
        pincodeEditText = findViewById(R.id.pincodeEditText)
        pincodeLocationAutoComplete = findViewById(R.id.pincodeLocationAutoComplete)
        listButton = findViewById(R.id.listButton)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        pincodeCityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)

        fetchUserLocation()
        setupSpinners()
        setupListeners()
        listButton.isEnabled = false
        setupBottomNavigation()
    }

    private fun setupSpinners() {
        val locationOptions = listOf("Default", "Other")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationOptions)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter
        locationSpinner.setSelection(0)

        val unitOptions = listOf("kg", "L")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitOptions)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter
        unitSpinner.setSelection(0)

        val categoryOptions = listOf("Fruits", "Vegetables", "Grains")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        categorySpinner.setSelection(0)
    }

    private fun setupListeners() {
        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                Log.d("CreateListing", "Location selected: $selected")
                if (selected == "Other") {
                    pincodeEditText.visibility = View.VISIBLE
                    pincodeLocationAutoComplete.visibility = View.VISIBLE
                    pincodeEditText.requestFocus()
                } else {
                    pincodeEditText.visibility = View.GONE
                    pincodeLocationAutoComplete.visibility = View.GONE
                    pincodeEditText.text.clear()
                    pincodeLocationAutoComplete.text.clear()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        pincodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pincode = s.toString()
                Log.d("CreateListing", "Pincode input: $pincode")
                if (pincode.length == 6) {
                    fetchCitiesFromPincode(pincode)
                    hideKeyboard()
                } else {
                    pincodeCityAdapter.clear()
                    pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)
                }
            }
        })

        pincodeLocationAutoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedLocation = parent.getItemAtPosition(position).toString()
            pincodeLocationAutoComplete.setText(selectedLocation)
            Log.d("CreateListing", "Selected location from autocomplete: $selectedLocation")
            hideKeyboard()
        }

        pincodeLocationAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Log.d("CreateListing", "Autocomplete text changed: ${s.toString()}")
                pincodeCityAdapter.filter.filter(s)
            }
        })
        pincodeLocationAutoComplete.threshold = 1

        uploadImageButton.setOnClickListener { selectImage() }

        listButton.setOnClickListener {
            val name = productNameEditText.text.toString().trim()
            val price = priceEditText.text.toString().toDoubleOrNull()
            val quantity = quantityEditText.text.toString().toIntOrNull()
            val unit = unitSpinner.selectedItem?.toString() ?: "kg"
            val category = categorySpinner.selectedItem?.toString() ?: "Fruits"
            val locationOption = locationSpinner.selectedItem?.toString() ?: "Default"
            val pincode = if (locationOption == "Other") {
                pincodeLocationAutoComplete.text.toString().trim().ifEmpty {
                    pincodeEditText.text.toString().trim()
                }
            } else {
                userLocation
            }

            Log.d("CreateListing", "List button clicked: name='$name', price=$price, quantity=$quantity, unit=$unit, category=$category, imageUrl=$productImageUrl, location=$pincode, isImageUploaded=$isImageUploaded")

            if (name.isEmpty()) {
                productNameEditText.error = "Product name is required"
                Toast.makeText(this, "Please enter a product name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (price == null) {
                priceEditText.error = "Valid price is required"
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantity == null) {
                quantityEditText.error = "Valid quantity is required"
                Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isImageUploaded || productImageUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Please upload a product image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pincode.isEmpty()) {
                Toast.makeText(this, "Please select or enter a valid location", Toast.LENGTH_SHORT).show()
                if (locationOption == "Other") {
                    pincodeEditText.error = "Valid pincode or location is required"
                }
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: run {
                Log.w("CreateListing", "No authenticated user")
                Toast.makeText(this, "Please log in to list a product", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            listButton.isEnabled = false

            fetchCoordinates(pincode) { lat: Double, lng: Double ->
                if (lat == 0.0 && lng == 0.0) {
                    Log.w("CreateListing", "Invalid coordinates for pincode: $pincode")
                    Toast.makeText(this, "Failed to fetch coordinates for location", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    listButton.isEnabled = true
                    return@fetchCoordinates
                }

                val listing = Listing(
                    id = null, // Backend generates ID
                    userId = userId,
                    name = name,
                    imageUrl = productImageUrl!!,
                    price = price,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    location = pincode,
                    coordinates = Listing.Coordinates(
                        type = "Point",
                        coordinates = listOf(lng, lat)
                    )
                )
                Log.d("CreateListing", "Listing payload: $listing")
                createListing(listing)
            }
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: pincodeLocationAutoComplete
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken ?: window.decorView.windowToken, 0)
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
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            productImageView.setImageBitmap(scaledBitmap)

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
            apiService.uploadImage(mapOf("imageBase64" to base64Image)).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        productImageUrl = response.body()?.get("url")
                        isImageUploaded = true
                        Log.d("CreateListing", "Image uploaded: $productImageUrl")
                        Toast.makeText(this@CreateListingActivity, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                        listButton.isEnabled = true
                    } else {
                        isImageUploaded = false
                        Log.e("CreateListing", "Image upload failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@CreateListingActivity, "Image upload failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    isImageUploaded = false
                    Log.e("CreateListing", "Network error during image upload: ${t.message}")
                    Toast.makeText(this@CreateListingActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            isImageUploaded = false
            Log.e("CreateListing", "Error uploading image: ${e.message}")
            Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createListing(listing: Listing, retryCount: Int = 0) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.createListing(listing).enqueue(object : Callback<Listing> {
            override fun onResponse(call: Call<Listing>, response: Response<Listing>) {
                if (response.isSuccessful) {
                    Log.d("CreateListing", "Listing created successfully: ${response.body()}")
                    Toast.makeText(this@CreateListingActivity, "Product listed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e("CreateListing", "Failed to create listing: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    if (response.code() == 500 && retryCount < 3) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            createListing(listing, retryCount + 1)
                        }, TimeUnit.SECONDS.toMillis(2))
                    } else {
                        Toast.makeText(this@CreateListingActivity, "Failed to list product: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                progressBar.visibility = View.GONE
                listButton.isEnabled = true
            }

            override fun onFailure(call: Call<Listing>, t: Throwable) {
                Log.e("CreateListing", "Network error creating listing: ${t.message}")
                if (retryCount < 3) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        createListing(listing, retryCount + 1)
                    }, TimeUnit.SECONDS.toMillis(2))
                } else {
                    Toast.makeText(this@CreateListingActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
                listButton.isEnabled = true
            }
        })
    }

    private fun fetchUserLocation(retryCount: Int = 0) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        auth.currentUser?.uid?.let { uid ->
            apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        val userData = response.body()
                        userLocation = userData?.address?.pincode ?: "Unknown"
                        Log.d("CreateListing", "User location fetched: $userLocation")
                    } else {
                        Log.e("CreateListing", "Failed to fetch user: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        if (retryCount < 5) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                fetchUserLocation(retryCount + 1)
                            }, TimeUnit.SECONDS.toMillis(3))
                        } else {
                            userLocation = "Unknown"
                            Toast.makeText(this@CreateListingActivity, "Failed to fetch user location", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("CreateListing", "Network error fetching user location: ${t.message}")
                    if (retryCount < 5) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            fetchUserLocation(retryCount + 1)
                        }, TimeUnit.SECONDS.toMillis(3))
                    } else {
                        userLocation = "Unknown"
                        Toast.makeText(this@CreateListingActivity, "Network error fetching location", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } ?: run {
            Log.w("CreateListing", "No authenticated user for location fetch")
            userLocation = "Unknown"
            Toast.makeText(this, "Please log in to fetch location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCitiesFromPincode(pincode: String) {
        val apiService = PincodeApiService.create()
        apiService.getPincodeData(pincode).enqueue(object : Callback<List<PincodeResponse>> {
            override fun onResponse(call: Call<List<PincodeResponse>>, response: Response<List<PincodeResponse>>) {
                if (response.isSuccessful) {
                    val pincodeData = response.body()?.firstOrNull()
                    if (pincodeData != null && pincodeData.Status == "Success" && !pincodeData.PostOffice.isNullOrEmpty()) {
                        val offices = pincodeData.PostOffice
                        val cities = offices.map { it.Name }.distinct().sorted()
                        Log.d("CreateListing", "Cities fetched for pincode $pincode: $cities")
                        pincodeCityAdapter.clear()
                        pincodeCityAdapter.addAll(cities)
                        pincodeCityAdapter.notifyDataSetChanged()
                        pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)
                        pincodeLocationAutoComplete.showDropDown()
                    } else {
                        Log.w("CreateListing", "Invalid pincode response for $pincode: $pincodeData")
                        pincodeCityAdapter.clear()
                        pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)
                        Toast.makeText(this@CreateListingActivity, "Invalid pincode", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("CreateListing", "Failed to fetch pincode: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    pincodeCityAdapter.clear()
                    pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)
                    Toast.makeText(this@CreateListingActivity, "Failed to fetch pincode data: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PincodeResponse>>, t: Throwable) {
                Log.e("CreateListing", "Network error fetching pincode: ${t.message}")
                pincodeCityAdapter.clear()
                pincodeLocationAutoComplete.setAdapter(pincodeCityAdapter)
                Toast.makeText(this@CreateListingActivity, "Network error fetching pincode: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchCoordinates(location: String, callback: (Double, Double) -> Unit) {
        val apiService = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoApiService::class.java)

        apiService.getCoordinates(location, "json").enqueue(object : Callback<List<GeoResponse>> {
            override fun onResponse(call: Call<List<GeoResponse>>, response: Response<List<GeoResponse>>) {
                if (response.isSuccessful) {
                    val geoData = response.body()?.firstOrNull()
                    val lat = geoData?.lat?.toDoubleOrNull() ?: 0.0
                    val lng = geoData?.lon?.toDoubleOrNull() ?: 0.0
                    Log.d("CreateListing", "Coordinates fetched for $location: lat=$lat, lng=$lng")
                    callback(lat, lng)
                } else {
                    Log.e("CreateListing", "Failed to fetch coordinates: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    callback(0.0, 0.0)
                }
            }

            override fun onFailure(call: Call<List<GeoResponse>>, t: Throwable) {
                Log.e("CreateListing", "Network error fetching coordinates: ${t.message}")
                callback(0.0, 0.0)
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomepageActivity::class.java).apply {
                        putExtra("userName", intent.getStringExtra("userName"))
                        putExtra("firebaseUid", auth.currentUser?.uid)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_my_listings -> {
                    val intent = Intent(this, MyListingsActivity::class.java).apply {
                        putExtra("userName", intent.getStringExtra("userName"))
                        putExtra("firebaseUid", auth.currentUser?.uid)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_create_listing -> {
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("userName", intent.getStringExtra("userName"))
                        putExtra("firebaseUid", auth.currentUser?.uid)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_others -> {
                    val intent = Intent(this, OthersActivity::class.java).apply {
                        putExtra("userName", intent.getStringExtra("userName"))
                        putExtra("firebaseUid", auth.currentUser?.uid)
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_create_listing
    }
}