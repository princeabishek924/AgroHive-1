package com.example.agrohive_1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationsRecyclerView: RecyclerView
    private val notificationList = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter // Declare as class property
    private var userRole: String? = null
    private var userName: String? = null // Add to pass through navigation

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        auth = FirebaseAuth.getInstance()
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        userName = intent.getStringExtra("userName") ?: "User" // Retrieve userName

        adapter = NotificationAdapter() // Initialize adapter
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = adapter

        fetchUserRoleAndNotifications()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.menu.clear()
        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_customer)
        bottomNavigationView.visibility = View.VISIBLE
        setupBottomNavigation()
    }

    private fun fetchUserRoleAndNotifications() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        auth.currentUser?.uid?.let { uid ->
            apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        userRole = response.body()?.let {
                            it.address // Trigger address access to ensure full deserialization
                            it.userType
                        }
                        if (userRole == "Farmer") {
                            apiService.getNotifications(userId = uid).enqueue(object : Callback<List<Notification>> {
                                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                                    if (response.isSuccessful) {
                                        notificationList.clear()
                                        notificationList.addAll(response.body() ?: emptyList())
                                        adapter.notifyDataSetChanged() // Use adapter here
                                    } else {
                                        Log.e("NotificationActivity", "Failed to fetch notifications: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                                    Log.e("NotificationActivity", "Network error: ${t.message}")
                                }
                            })
                        } else {
                            Toast.makeText(this@NotificationActivity, "Access restricted to Farmers", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Log.e("NotificationActivity", "Failed to fetch user role: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("NotificationActivity", "Network error fetching role: ${t.message}")
                }
            })
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomepageActivity::class.java)
                    intent.putExtra("userName", userName)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_chat -> {
                    val intent = Intent(this, MessageActivity::class.java)
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
        bottomNavigationView.selectedItemId = R.id.nav_my_listings // Adjust based on context
    }

    inner class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = notificationList[position]
            holder.text1.text = "${notification.fromType} ${notification.fromUid}: ${notification.message}"
            holder.text2.text = notification.timestamp
            if (!notification.isRead) {
                holder.text1.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
            }
        }

        override fun getItemCount(): Int = notificationList.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text1: TextView = itemView.findViewById(android.R.id.text1)
            val text2: TextView = itemView.findViewById(android.R.id.text2)
        }
    }
}