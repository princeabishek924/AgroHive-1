package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsernameActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var usernameEditText: EditText
    private lateinit var saveUsernameButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var usernameAvailabilityText: TextView
    private var firebaseUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username)
        Log.d("UsernameActivity", "onCreate: Activity started")

        auth = FirebaseAuth.getInstance()
        usernameEditText = findViewById(R.id.usernameEditText)
        saveUsernameButton = findViewById(R.id.saveUsernameButton)
        progressBar = findViewById(R.id.progressBar)
        usernameAvailabilityText = findViewById(R.id.usernameAvailabilityText)
        firebaseUid = intent.getStringExtra("firebaseUid")

        if (firebaseUid == null) {
            Log.e("UsernameActivity", "No Firebase UID provided")
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
                    saveUsernameButton.isEnabled = false
                }
            }
        })

        saveUsernameButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            if (username.isNotEmpty() && usernameAvailabilityText.text == "Username is available") {
                progressBar.visibility = View.VISIBLE
                saveUsernameButton.isEnabled = false
                updateUsername(username)
            } else {
                Toast.makeText(this, "Please enter a valid and available username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUsernameAvailability(username: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.checkUsernameAvailability(username).enqueue(object : Callback<Map<String, Boolean>> {
            override fun onResponse(call: Call<Map<String, Boolean>>, response: Response<Map<String, Boolean>>) {
                if (response.isSuccessful) {
                    val isAvailable = response.body()?.get("available") ?: false
                    if (isAvailable) {
                        usernameAvailabilityText.text = "Username is available"
                        usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                        saveUsernameButton.isEnabled = true
                    } else {
                        usernameAvailabilityText.text = "Username is taken"
                        usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                        saveUsernameButton.isEnabled = false
                    }
                } else {
                    Log.e("UsernameActivity", "Failed to check username: HTTP ${response.code()} - ${response.errorBody()?.string() ?: "No error body"}")
                    usernameAvailabilityText.text = "Error checking availability"
                    usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    saveUsernameButton.isEnabled = false
                }
            }

            override fun onFailure(call: Call<Map<String, Boolean>>, t: Throwable) {
                Log.e("UsernameActivity", "Network error: ${t.message}")
                usernameAvailabilityText.text = "Network error"
                usernameAvailabilityText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                saveUsernameButton.isEnabled = false
            }
        })
    }

    private fun updateUsername(username: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val updateData = mapOf("firebaseUid" to firebaseUid!!, "username" to username)
        apiService.updateUsername(updateData).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Log.d("UsernameActivity", "Username updated: ${response.body()?.username}")
                    Toast.makeText(this@UsernameActivity, "Username set successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@UsernameActivity, HomepageActivity::class.java)
                    intent.putExtra("userName", username)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("UsernameActivity", "Failed to update username: HTTP ${response.code()} - ${response.errorBody()?.string() ?: "No error body"}")
                    Toast.makeText(this@UsernameActivity, "Failed to set username", Toast.LENGTH_SHORT).show()
                    saveUsernameButton.isEnabled = true
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("UsernameActivity", "Network error: ${t.message}")
                Toast.makeText(this@UsernameActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                saveUsernameButton.isEnabled = true
            }
        })
    }
}