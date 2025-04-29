package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var salesChart: BarChart
    private lateinit var engagementChart: BarChart
    private lateinit var revenueChart: BarChart
    private lateinit var userNameText: TextView
    private lateinit var notificationBell: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userName: String? = null
    private var userType: String? = null
    private lateinit var firebaseUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        salesChart = findViewById(R.id.salesChart)
        engagementChart = findViewById(R.id.engagementChart)
        revenueChart = findViewById(R.id.revenueChart)
        userNameText = findViewById(R.id.userNameText)
        notificationBell = findViewById(R.id.notificationBell)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        userName = intent.getStringExtra("userName") ?: "Farmer"
        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""
        userNameText.text = userName
        fetchUserRoleAndSetup()
    }

    private fun fetchUserRoleAndSetup() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getUser(firebaseUid).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    userType = response.body()?.userType?.lowercase()
                    Log.d("DashboardActivity", "Fetched userType: $userType")
                    if (userType == "farmer") {
                        bottomNavigationView.menu.clear()
                        bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_farmer)
                        setupCharts()
                        setupNotificationBell()
                        setupBottomNavigation()
                        fetchDashboardData()
                    } else {
                        Log.e("DashboardActivity", "Access restricted: User is not a farmer (userType: $userType)")
                        Toast.makeText(this@DashboardActivity, "Access restricted to Farmers", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Log.e("DashboardActivity", "Failed to fetch user role: ${response.code()} - ${response.errorBody()}")
                    Toast.makeText(this@DashboardActivity, "Failed to verify role", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("DashboardActivity", "Network error fetching role: ${t.message}")
                Toast.makeText(this@DashboardActivity, "Network error", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupCharts() {
        listOf<BarChart>(salesChart, engagementChart, revenueChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setDrawGridBackground(false)
            chart.legend.isEnabled = false
            chart.axisLeft.setDrawLabels(false)
            chart.axisRight.setDrawLabels(false)
            chart.xAxis.setDrawLabels(false)
            chart.animateY(1000)
        }
    }

    private fun fetchDashboardData() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getDashboardData(firebaseUid).enqueue(object : Callback<DashboardData> {
            override fun onResponse(call: Call<DashboardData>, response: Response<DashboardData>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateSalesChart(data.sales)
                    updateEngagementChart(data.engagement)
                    updateRevenueChart(data.revenue)
                } else {
                    Log.e("DashboardActivity", "Failed to fetch dashboard data: ${response.code()}")
                    Toast.makeText(this@DashboardActivity, "Failed to load dashboard", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DashboardData>, t: Throwable) {
                Log.e("DashboardActivity", "Network error: ${t.message}")
                Toast.makeText(this@DashboardActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSalesChart(sales: List<Pair<String, Int>>) {
        val entries = sales.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "Sales").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.progress_bar_color)
        }
        salesChart.data = BarData(dataSet)
        salesChart.invalidate()
    }

    private fun updateEngagementChart(engagement: List<Pair<String, Int>>) {
        val entries = engagement.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "Engagement").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.teal_200)
        }
        engagementChart.data = BarData(dataSet)
        engagementChart.invalidate()
    }

    private fun updateRevenueChart(revenue: List<Pair<String, Double>>) {
        val entries = revenue.mapIndexed { index, pair -> BarEntry(index.toFloat(), pair.second.toFloat()) }
        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.purple_500)
        }
        revenueChart.data = BarData(dataSet)
        revenueChart.invalidate()
    }

    private fun setupNotificationBell() {
        notificationBell.setOnClickListener {
            if (userType == "farmer") {
                val intent = Intent(this, NotificationActivity::class.java)
                intent.putExtra("userName", userName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Access restricted to Farmers", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_messages -> {
                    if (userType == "farmer") {
                        val intent = Intent(this, MessageActivity::class.java)
                        intent.putExtra("userName", userName)
                        intent.putExtra("firebaseUid", firebaseUid)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_create_listing -> {
                    if (userType == "farmer") {
                        val intent = Intent(this, CreateListingActivity::class.java)
                        intent.putExtra("userName", userName)
                        intent.putExtra("firebaseUid", firebaseUid)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_my_listings -> {
                    if (userType == "farmer") {
                        val intent = Intent(this, MyListingsActivity::class.java)
                        intent.putExtra("userName", userName)
                        intent.putExtra("firebaseUid", firebaseUid)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_others -> {
                    if (userType == "farmer") {
                        val intent = Intent(this, OthersActivity::class.java)
                        intent.putExtra("userName", userName)
                        intent.putExtra("firebaseUid", firebaseUid)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_dashboard
    }
}