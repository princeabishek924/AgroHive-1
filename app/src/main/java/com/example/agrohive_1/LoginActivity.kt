package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var loginText: TextView
    private lateinit var identifierEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var forgotPasswordLayout: LinearLayout
    private lateinit var resetEmailEditText: EditText
    private lateinit var continueButton: Button
    private lateinit var resetSuccessText: TextView
    private lateinit var backToLoginText: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        loginText = findViewById(R.id.LoginText)
        identifierEditText = findViewById(R.id.identifierEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton)
        progressBar = findViewById(R.id.progressBar)
        forgotPasswordLayout = findViewById(R.id.forgotPasswordLayout)
        resetEmailEditText = findViewById(R.id.resetEmailEditText)
        continueButton = findViewById(R.id.continueButton)
        resetSuccessText = findViewById(R.id.resetSuccessText)
        backToLoginText = findViewById(R.id.backToLoginText)

        passwordEditText.setOnTouchListener { v, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        loginButton.setOnClickListener {
            val identifier = identifierEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email/username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            loginButton.isEnabled = false

            if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                performFirebaseLogin(identifier, password)
            } else {
                checkUsernameAndLogin(identifier, password)
            }
        }

        forgotPasswordButton.setOnClickListener {
            loginButton.visibility = View.GONE
            forgotPasswordButton.visibility = View.GONE
            forgotPasswordLayout.visibility = View.VISIBLE
        }

        continueButton.setOnClickListener {
            val email = resetEmailEditText.text.toString().trim()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetEmailEditText.error = "Please enter a valid email"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            continueButton.isEnabled = false

            fetchUserNameForEmail(email)
        }

        backToLoginText.setOnClickListener {
            forgotPasswordLayout.visibility = View.GONE
            loginButton.visibility = View.VISIBLE
            forgotPasswordButton.visibility = View.VISIBLE
            resetSuccessText.visibility = View.GONE
            backToLoginText.visibility = View.GONE
            resetEmailEditText.visibility = View.VISIBLE
            continueButton.visibility = View.VISIBLE
            resetEmailEditText.text.clear()
            resetEmailEditText.error = null
            resetSuccessText.text = ""
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            passwordEditText.transformationMethod = null
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
        } else {
            passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, R.drawable.eye, 0)
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    private fun performFirebaseLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Login successful: ${auth.currentUser?.uid}")
                    checkFirstLoginAndRedirect()
                } else {
                    val exception = task.exception
                    Log.e("FirebaseAuth", "Login failed: ${exception?.message}")
                    when (exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Login failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener(this) { exception ->
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
                Log.e("FirebaseAuth", "Login failure: ${exception.message}")
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUsernameAndLogin(username: String, password: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getEmailByUsername(username).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    val email = response.body()?.get("email")
                    if (email != null) {
                        performFirebaseLogin(email, password)
                    } else {
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    Log.e("LoginActivity", "Failed to get email: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@LoginActivity, "Error fetching user data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
                Log.e("LoginActivity", "Network error: ${t.message}")
                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkFirstLoginAndRedirect() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        val userData = response.body()
                        if (userData?.username == null) {
                            val intent = Intent(this@LoginActivity, UsernameActivity::class.java)
                            intent.putExtra("firebaseUid", uid)
                            startActivity(intent)
                        } else {
                            when (userData.userType?.lowercase()) {
                                "farmer" -> {
                                    val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                                    intent.putExtra("userName", userData.name)
                                    intent.putExtra("firebaseUid", uid)
                                    intent.putExtra("userPincode", userData.address.pincode)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                "customer", "marketer" -> {
                                    val intent = Intent(this@LoginActivity, HomepageActivity::class.java)
                                    intent.putExtra("userName", userData.name)
                                    intent.putExtra("firebaseUid", uid)
                                    intent.putExtra("userPincode", userData.address.pincode)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                                else -> {
                                    Toast.makeText(this@LoginActivity, "Unknown user type", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@LoginActivity, HomepageActivity::class.java))
                                }
                            }
                        }
                        finish()
                    } else {
                        Log.e("LoginActivity", "Failed to fetch user: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@LoginActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        loginButton.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("LoginActivity", "Network error: ${t.message}")
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                }
            })
        } else {
            progressBar.visibility = View.GONE
            loginButton.isEnabled = true
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserNameForEmail(email: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getUserByEmail(email).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val userData = response.body()!!
                    val displayName = userData.name ?: "User"
                    sendPasswordResetEmail(email)
                    Log.d("LoginActivity", "Fetched displayName: $displayName for email: $email")
                } else {
                    progressBar.visibility = View.GONE
                    continueButton.isEnabled = true
                    Log.e("LoginActivity", "Email not found or invalid: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    resetEmailEditText.error = "Email not found in our records"
                    Toast.makeText(this@LoginActivity, "Email not found in our records", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                continueButton.isEnabled = true
                Log.e("LoginActivity", "Network error fetching user name: ${t.message}")
                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                resetEmailEditText.error = "Network error, please try again"
            }
        })
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this@LoginActivity) { task ->
                progressBar.visibility = View.GONE
                continueButton.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Password reset email sent to $email")
                    resetEmailEditText.visibility = View.GONE
                    continueButton.visibility = View.GONE
                    resetSuccessText.text = "Password reset email sent to $email. Check your inbox!"
                    resetSuccessText.visibility = View.VISIBLE
                    backToLoginText.visibility = View.VISIBLE
                } else {
                    val exception = task.exception
                    Log.e("FirebaseAuth", "Failed to send reset email: ${exception?.message}")
                    Toast.makeText(this@LoginActivity, "Failed to send reset email: ${exception?.message}", Toast.LENGTH_LONG).show()
                    if (exception is FirebaseAuthInvalidCredentialsException) {
                        resetEmailEditText.error = "Email not found in Firebase"
                    }
                }
            }
            .addOnFailureListener(this@LoginActivity) { exception ->
                progressBar.visibility = View.GONE
                continueButton.isEnabled = true
                Log.e("FirebaseAuth", "Failure during reset email: ${exception.message}")
                Toast.makeText(this@LoginActivity, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}