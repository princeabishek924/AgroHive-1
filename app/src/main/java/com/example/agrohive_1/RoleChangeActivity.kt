package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RoleChangeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var passwordEditText: EditText
    private lateinit var warningsText: WebView
    private lateinit var understandCheckbox: CheckBox
    private lateinit var changeRoleButton: Button
    private lateinit var firebaseUid: String
    private lateinit var currentUserType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_change)

        auth = FirebaseAuth.getInstance()
        passwordEditText = findViewById(R.id.passwordEditText)
        warningsText = findViewById(R.id.warningsText)
        understandCheckbox = findViewById(R.id.understandCheckbox)
        changeRoleButton = findViewById(R.id.changeRoleButton)

        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        currentUserType = intent.getStringExtra("userType") ?: "farmer"

        // Update WebView with HTML, inserting currentUserType directly
        val htmlText = """
            <html>
            <body style="text-align: justify; font-family: Montserrat, sans-serif; font-size: 16px; color: black;">
            ${getString(R.string.role_change_warnings).replace("%s", currentUserType).replace("\n", "<br>")}
            </body>
            </html>
        """.trimIndent()
        warningsText.loadDataWithBaseURL(null, htmlText, "text/html", "UTF-8", null)

        changeRoleButton.setOnClickListener { attemptRoleChange() }
    }

    private fun attemptRoleChange() {
        val password = passwordEditText.text.toString().trim()
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }
        if (!understandCheckbox.isChecked) {
            Toast.makeText(this, "Please acknowledge the changes", Toast.LENGTH_SHORT).show()
            return
        }

        val email = auth.currentUser?.email ?: ""
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showRoleSelectionDialog()
                } else {
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showRoleSelectionDialog() {
        val roles = arrayOf("Farmer", "Customer", "Marketer")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select New Role")
            .setItems(roles) { _, which ->
                val newRole = roles[which]
                if (newRole.lowercase() != currentUserType.lowercase()) {
                    updateRoleInDB(newRole.lowercase())
                } else {
                    Toast.makeText(this, "Same role selected", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRoleInDB(newRole: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val updateData = mapOf("firebaseUid" to firebaseUid, "userType" to newRole)
        apiService.updateRole(updateData).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    deleteOldRoleData(newRole)
                    auth.signOut()
                    startActivity(Intent(this@RoleChangeActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Log.e("RoleChangeActivity", "Failed to update role: ${response.code()}")
                    Toast.makeText(this@RoleChangeActivity, "Failed to change role", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("RoleChangeActivity", "Network error: ${t.message}")
                Toast.makeText(this@RoleChangeActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteOldRoleData(newRole: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.deleteRoleData(firebaseUid, currentUserType).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("RoleChangeActivity", "Failed to delete old data: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("RoleChangeActivity", "Network error deleting data: ${t.message}")
            }
        })
    }
}