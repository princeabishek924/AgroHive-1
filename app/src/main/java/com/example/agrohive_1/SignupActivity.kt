package com.example.agrohive_1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var pincodeEditText: EditText
    private lateinit var areaSpinner: Spinner
    private lateinit var citySpinner: Spinner
    private lateinit var districtSpinner: Spinner
    private lateinit var stateSpinner: Spinner
    private lateinit var countrySpinner: Spinner
    private lateinit var userTypeSpinner: Spinner
    private lateinit var profileImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var signUpButton: Button
    private lateinit var progressBar: ProgressBar
    private var profileImageUrl: String? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> uploadImageToBackend(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        Log.d("SignupActivity", "onCreate: Activity started")

        auth = FirebaseAuth.getInstance()
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        pincodeEditText = findViewById(R.id.pincodeEditText)
        areaSpinner = findViewById(R.id.areaSpinner)
        citySpinner = findViewById(R.id.citySpinner)
        districtSpinner = findViewById(R.id.districtSpinner)
        stateSpinner = findViewById(R.id.stateSpinner)
        countrySpinner = findViewById(R.id.countrySpinner)
        userTypeSpinner = findViewById(R.id.userTypeSpinner)
        profileImageView = findViewById(R.id.profileImageView)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        signUpButton = findViewById(R.id.signUpButton)
        progressBar = findViewById(R.id.progressBar)

        val userTypes = arrayOf("Farmer", "Customer", "Marketer")
        setupSearchableSpinner(userTypeSpinner, userTypes.toList())

        fetchCountries()

        pincodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pincode = s.toString().trim()
                if (pincode.length == 6 && isValidPincode(pincode)) {
                    fetchPincodeData(pincode)
                }
            }
        })

        uploadImageButton.setOnClickListener { selectImage() }

        signUpButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val pincode = pincodeEditText.text.toString().trim()
            val area = areaSpinner.selectedItem?.toString() ?: ""
            val city = citySpinner.selectedItem?.toString() ?: ""
            val district = districtSpinner.selectedItem?.toString() ?: ""
            val state = stateSpinner.selectedItem?.toString() ?: ""
            val country = countrySpinner.selectedItem?.toString() ?: "India"
            val userType = userTypeSpinner.selectedItem?.toString() ?: ""

            if (!validateInputs(name, email, password, phone, pincode, area, city, district, state)) return@setOnClickListener

            progressBar.visibility = View.VISIBLE
            signUpButton.isEnabled = false

            fetchCoordinates(pincode) { lat, lng ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FirebaseAuth", "User created in Firebase: ${auth.currentUser?.uid}")
                            val userData = UserData(
                                name = name,
                                email = email,
                                password = password,
                                phone = phone,
                                address = Address("", "", city, district, state, country, pincode, area, lat, lng),
                                userType = userType,
                                profileImageUrl = profileImageUrl ?: "",
                                firebaseUid = auth.currentUser?.uid
                            )
                            Log.d("Signup", "Sending userData to MongoDB: $userData")
                            saveUserDataToMongoDB(userData)
                        } else {
                            Log.e("FirebaseAuth", "Failed: ${task.exception?.message}")
                            Toast.makeText(this@SignupActivity, "Firebase signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            progressBar.visibility = View.GONE
                            signUpButton.isEnabled = true
                        }
                    }
            }
        }
    }

    private fun saveUserDataToMongoDB(userData: UserData) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.saveUserData(userData).enqueue(object : Callback<UserData> {
            override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                if (response.isSuccessful) {
                    Log.d("MongoDB", "User saved: ${response.body()?.email}")
                    Toast.makeText(this@SignupActivity, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    val intent = Intent(this@SignupActivity, SignupSuccessActivity::class.java)
                    intent.putExtra("firebaseUid", userData.firebaseUid)
                    intent.putExtra("profileImageUrl", profileImageUrl)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("MongoDB", "Failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@SignupActivity, "Failed to save user data: ${response.code()}", Toast.LENGTH_LONG).show()
                    auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d("FirebaseAuth", "Rolled back Firebase user")
                        } else {
                            Log.e("FirebaseAuth", "Rollback failed: ${deleteTask.exception?.message}")
                        }
                    }
                    progressBar.visibility = View.GONE
                    signUpButton.isEnabled = true
                }
            }

            override fun onFailure(call: Call<UserData>, t: Throwable) {
                Log.e("MongoDB", "Network error: ${t.message}")
                Toast.makeText(this@SignupActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        Log.d("FirebaseAuth", "Rolled back Firebase user")
                    } else {
                        Log.e("FirebaseAuth", "Rollback failed: ${deleteTask.exception?.message}")
                    }
                }
                progressBar.visibility = View.GONE
                signUpButton.isEnabled = true
            }
        })
    }

    private fun fetchCountries() {
        val apiService = CountryApiService.create()
        apiService.getCountries().enqueue(object : Callback<List<Country>> {
            override fun onResponse(call: Call<List<Country>>, response: Response<List<Country>>) {
                if (response.isSuccessful) {
                    val countries = response.body() ?: emptyList()
                    val countryNames = countries.map { it.name.common }.sorted()
                    setupSearchableSpinner(countrySpinner, countryNames)
                    val indiaPosition = countryNames.indexOf("India")
                    countrySpinner.setSelection(if (indiaPosition != -1) indiaPosition else 0)
                } else {
                    Log.w("Countries", "Failed to fetch countries: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    setupSearchableSpinner(countrySpinner, listOf("India"))
                    Toast.makeText(this@SignupActivity, "Using default country: India", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Country>>, t: Throwable) {
                Log.w("Countries", "Network error fetching countries: ${t.message}")
                setupSearchableSpinner(countrySpinner, listOf("India"))
                Toast.makeText(this@SignupActivity, "Using default country: India", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPincodeData(pincode: String) {
        val apiService = PincodeApiService.create()
        apiService.getPincodeData(pincode).enqueue(object : Callback<List<PincodeResponse>> {
            override fun onResponse(call: Call<List<PincodeResponse>>, response: Response<List<PincodeResponse>>) {
                Log.d("Pincode", "Raw response: ${response.raw()}")
                Log.d("Pincode", "Response code: ${response.code()}, Body: ${response.body()}, Error: ${response.errorBody()?.string()}")

                if (response.isSuccessful) {
                    val pincodeDataList = response.body()
                    Log.d("Pincode", "Parsed data: $pincodeDataList")
                    val pincodeData = pincodeDataList?.firstOrNull()

                    if (pincodeData != null && pincodeData.Status == "Success" && !pincodeData.PostOffice.isNullOrEmpty()) {
                        val offices = pincodeData.PostOffice
                        Log.d("Pincode", "Post offices: $offices")
                        val areas = offices.map { office -> office.Name }.sorted()
                        val cities = offices.map { office -> office.Name }.distinct().sorted()
                        val districts = offices.map { office -> office.District }.distinct().sorted()
                        val states = offices.map { office -> office.State }.distinct().sorted()

                        setupSearchableSpinner(areaSpinner, areas)
                        setupSearchableSpinner(citySpinner, cities)
                        setupSearchableSpinner(districtSpinner, districts)
                        setupSearchableSpinner(stateSpinner, states)

                        areaSpinner.setSelection(0)
                        citySpinner.setSelection(0)
                        districtSpinner.setSelection(0)
                        stateSpinner.setSelection(0)

                        areaSpinner.visibility = View.VISIBLE
                        citySpinner.visibility = View.VISIBLE
                        districtSpinner.visibility = View.VISIBLE
                        stateSpinner.visibility = View.VISIBLE
                    } else {
                        Log.w("Pincode", "Invalid or empty response: Status=${pincodeData?.Status}, PostOffice=${pincodeData?.PostOffice}")
                        Toast.makeText(this@SignupActivity, "Invalid pincode or no data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Pincode", "HTTP Error: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@SignupActivity, "Failed to fetch pincode: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<PincodeResponse>>, t: Throwable) {
                Log.e("Pincode", "Network failure: ${t.message}")
                Toast.makeText(this@SignupActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
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
                if (response.isSuccessful) {
                    val geoData = response.body()?.firstOrNull()
                    val lat = geoData?.lat?.toDoubleOrNull() ?: 0.0
                    val lng = geoData?.lon?.toDoubleOrNull() ?: 0.0
                    Log.d("Geo", "Coordinates fetched: lat=$lat, lng=$lng")
                    callback(lat, lng)
                } else {
                    Log.e("Geo", "Failed to fetch coordinates: HTTP ${response.code()}")
                    callback(0.0, 0.0)
                }
            }

            override fun onFailure(call: Call<List<GeoResponse>>, t: Throwable) {
                Log.e("Geo", "Network error: ${t.message}")
                callback(0.0, 0.0)
            }
        })
    }

    private fun setupSearchableSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)
    }

    private fun validateInputs(name: String, email: String, password: String, phone: String, pincode: String, area: String, city: String, district: String, state: String): Boolean {
        var isValid = true
        if (name.isEmpty()) { nameEditText.error = "Name is required"; isValid = false }
        if (email.isEmpty() || !isValidEmail(email)) { emailEditText.error = "Enter a valid email"; isValid = false }
        if (password.isEmpty() || !isValidPassword(password)) { passwordEditText.error = "Password must have 1 uppercase, 1 lowercase, 1 number, 1 special char"; isValid = false }
        if (phone.isEmpty()) { phoneEditText.error = "Phone is required"; isValid = false }
        if (pincode.isEmpty() || !isValidPincode(pincode)) { pincodeEditText.error = "Enter a valid 6-digit pincode"; isValid = false }
        if (area.isEmpty()) { Toast.makeText(this, "Select an area", Toast.LENGTH_SHORT).show(); isValid = false }
        if (city.isEmpty()) { Toast.makeText(this, "Select a city", Toast.LENGTH_SHORT).show(); isValid = false }
        if (district.isEmpty()) { Toast.makeText(this, "Select a district", Toast.LENGTH_SHORT).show(); isValid = false }
        if (state.isEmpty()) { Toast.makeText(this, "Select a state", Toast.LENGTH_SHORT).show(); isValid = false }
        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailPattern.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$")
        return passwordPattern.matcher(password).matches()
    }

    private fun isValidPincode(pincode: String): Boolean {
        return pincode.matches(Regex("^[0-9]{6}$"))
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
            profileImageView.setImageBitmap(scaledBitmap)

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
            apiService.uploadImage(mapOf("imageBase64" to base64Image)).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        profileImageUrl = response.body()?.get("url")
                        Log.d("Upload", "Image uploaded: $profileImageUrl")
                        Toast.makeText(this@SignupActivity, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("Upload", "Failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@SignupActivity, "Image upload failed: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Log.e("Upload", "Network error: ${t.message}")
                    Toast.makeText(this@SignupActivity, "Image upload failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e("Upload", "Error: ${e.message}")
            Toast.makeText(this@SignupActivity, "Image selection failed", Toast.LENGTH_SHORT).show()
        }
    }
}

data class Country(
    val name: Name,
    val idd: Idd
) {
    data class Name(val common: String, val official: String)
    data class Idd(val root: String, val suffixes: List<String>?)
}

interface CountryApiService {
    @GET("v3.1/all")
    fun getCountries(): Call<List<Country>>

    companion object {
        fun create(): CountryApiService {
            return Retrofit.Builder()
                .baseUrl("https://restcountries.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CountryApiService::class.java)
        }
    }
}