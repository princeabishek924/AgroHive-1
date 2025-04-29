package com.example.agrohive_1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrohive_1.databinding.ActivityOrdersBinding

class OrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrdersBinding
    private var userName: String = "User"
    private var firebaseUid: String = ""
    private var userType: String = "customer"
    private var userPincode: String = "Unknown"
    private var userCity: String = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("userName") ?: "User"
        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        userType = intent.getStringExtra("userType")?.lowercase() ?: "customer"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"

        // TODO: Fetch and display orders using ApiService.getOrders(firebaseUid)
    }
}