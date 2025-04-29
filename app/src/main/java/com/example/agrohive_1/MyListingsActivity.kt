package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyListingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var listingsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userName: String? = null
    private var userType: String? = null
    private val listingList = mutableListOf<Listing>()
    private val orderList = mutableListOf<Order>()
    private lateinit var adapter: ListingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_listings)

        auth = FirebaseAuth.getInstance()
        listingsRecyclerView = findViewById(R.id.listingsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        userName = intent.getStringExtra("userName") ?: "User"
        fetchUserRole()

        adapter = ListingsAdapter()
        listingsRecyclerView.layoutManager = LinearLayoutManager(this)
        listingsRecyclerView.adapter = adapter

        setupBottomNavigation()
    }

    private fun fetchUserRole() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        auth.currentUser?.uid?.let { uid ->
            apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        userType = response.body()?.userType
                        Log.d("MyListingsActivity", "User role: $userType")
                        if (userType == "Farmer") {
                            bottomNavigationView.menu.clear()
                            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_farmer)
                            fetchMyListingsAndOrders()
                        } else {
                            Toast.makeText(this@MyListingsActivity, "Access restricted to Farmers", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Log.e("MyListingsActivity", "Failed to fetch user role: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("MyListingsActivity", "Network error: ${t.message}")
                }
            })
        }
    }

    private fun fetchMyListingsAndOrders() {
        progressBar.visibility = View.VISIBLE
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        auth.currentUser?.uid?.let { uid ->
            apiService.getUserListings(userId = uid).enqueue(object : Callback<List<Listing>> {
                override fun onResponse(call: Call<List<Listing>>, response: Response<List<Listing>>) {
                    if (response.isSuccessful) {
                        listingList.clear()
                        listingList.addAll(response.body() ?: emptyList())
                        fetchOrders(uid)
                    } else {
                        progressBar.visibility = View.GONE
                        Log.e("MyListingsActivity", "Failed to fetch listings: ${response.code()}")
                        Toast.makeText(this@MyListingsActivity, "Failed to load listings", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Listing>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("MyListingsActivity", "Network error: ${t.message}")
                    Toast.makeText(this@MyListingsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchOrders(farmerUid: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getOrdersForFarmer(farmerId = farmerUid).enqueue(object : Callback<List<Order>> {
            override fun onResponse(call: Call<List<Order>>, response: Response<List<Order>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    orderList.clear()
                    orderList.addAll(response.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    Log.d("MyListingsActivity", "Fetched ${orderList.size} orders")
                } else {
                    Log.e("MyListingsActivity", "Failed to fetch orders: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Order>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("MyListingsActivity", "Network error: ${t.message}")
            }
        })
    }

    private fun acceptOrder(order: Order) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.acceptOrder(order.id).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MyListingsActivity, "Order accepted", Toast.LENGTH_SHORT).show()
                    updateListingQuantity(order.listingId, -order.quantity)
                } else {
                    Toast.makeText(this@MyListingsActivity, "Failed to accept order", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Log.e("MyListingsActivity", "Network error: ${t.message}")
            }
        })
    }

    private fun cancelOrder(order: Order) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.cancelOrder(order.id).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MyListingsActivity, "Order cancelled", Toast.LENGTH_SHORT).show()
                    notifyCustomerCancellation(order.userId, order.listingId)
                    updateListingQuantity(order.listingId, -order.quantity)
                } else {
                    Toast.makeText(this@MyListingsActivity, "Failed to cancel order", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Log.e("MyListingsActivity", "Network error: ${t.message}")
            }
        })
    }

    private fun notifyCustomerCancellation(customerUid: String, listingId: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val message = "Your order for this product from this farmer was cancelled."
        apiService.sendNotification(customerUid, message).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (!response.isSuccessful) {
                    Log.e("MyListingsActivity", "Failed to send cancellation notification: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                Log.e("MyListingsActivity", "Network error: ${t.message}")
            }
        })
    }

    private fun updateListingQuantity(listingId: String, delta: Int) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val index = listingList.indexOfFirst { it.userId == listingId }
        if (index != -1) {
            val listing = listingList[index]
            val newQuantity = listing.quantity + delta
            apiService.updateListingQuantity(listingId, newQuantity).enqueue(object : Callback<OrderResponse> {
                override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                    if (response.isSuccessful) {
                        val updatedQuantity = response.body()?.updatedQuantity ?: newQuantity
                        listingList[index] = listing.copy(quantity = updatedQuantity)
                        if (updatedQuantity <= 0) {
                            apiService.deleteListing(listingId).enqueue(object : Callback<OrderResponse> {
                                override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                                    if (response.isSuccessful) {
                                        listingList.removeAt(index)
                                        adapter.notifyDataSetChanged()
                                        notifyUserDeletion(listingId)
                                    }
                                }

                                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                                    Log.e("MyListingsActivity", "Network error: ${t.message}")
                                }
                            })
                        } else {
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                    Log.e("MyListingsActivity", "Network error: ${t.message}")
                }
            })
        }
    }

    private fun notifyUserDeletion(listingId: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val message = "Your listing has been deleted due to zero quantity."
        apiService.sendNotification(auth.currentUser?.uid ?: "", message).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (!response.isSuccessful) {
                    Log.e("MyListingsActivity", "Failed to send deletion notification: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                Log.e("MyListingsActivity", "Network error: ${t.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    if (userType == "Farmer") {
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.putExtra("userName", userName)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_messages -> {
                    if (userType == "Farmer") {
                        val intent = Intent(this, MessageActivity::class.java)
                        intent.putExtra("userName", userName)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_create_listing -> {
                    if (userType == "Farmer") {
                        val intent = Intent(this, CreateListingActivity::class.java)
                        intent.putExtra("userName", userName)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_my_listings -> true
                R.id.nav_others -> {
                    if (userType == "Farmer") {
                        val intent = Intent(this, OthersActivity::class.java)
                        intent.putExtra("userName", userName)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_my_listings
    }

    inner class ListingsAdapter : RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_listing, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val listing = listingList[position]
            holder.nameTextView.text = listing.name
            holder.priceTextView.text = "₹${listing.price}"
            holder.quantityTextView.text = "Available: ${listing.quantity} ${listing.unit}"
            holder.dateTextView.text = "Listed on: ${listing.createdAt ?: "N/A"}"
            holder.locationTextView.text = "Location: ${listing.location}"
            Glide.with(holder.itemView.context)
                .load(listing.imageUrl)
                .placeholder(R.drawable.placeholder_image_bg)
                .into(holder.imageView)

            val orders = orderList.filter { it.listingId == listing.userId && it.status == "pending" }
            if (orders.isNotEmpty()) {
                holder.orderStatusTextView.visibility = View.VISIBLE
                holder.orderStatusTextView.text = "${orders.size} new order(s)"
                holder.orderStatusTextView.setOnClickListener {
                    showOrderDialog(orders)
                }
            } else {
                holder.orderStatusTextView.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = listingList.size

        private fun showOrderDialog(orders: List<Order>) {
            val builder = AlertDialog.Builder(this@MyListingsActivity)
            builder.setTitle("Pending Orders")
            val view = LayoutInflater.from(this@MyListingsActivity).inflate(R.layout.dialog_order, null)
            builder.setView(view)

            val recyclerView = view.findViewById<RecyclerView>(R.id.orderRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this@MyListingsActivity)
            recyclerView.adapter = OrderAdapter(orders)

            builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            builder.show()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.listingImageView)
            val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
            val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
            val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
            val locationTextView: TextView = itemView.findViewById(R.id.viewDetailsText)
            val orderStatusTextView: TextView = itemView.findViewById(R.id.orderStatusTextView)
        }
    }

    inner class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = orders[position]
            holder.orderTextView.text = "\"${order.userId}\" ordered ${order.quantity} units of this product"
            holder.acceptButton.setOnClickListener { acceptOrder(order) }
            holder.cancelButton.setOnClickListener { cancelOrder(order) }
        }

        override fun getItemCount(): Int = orders.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val orderTextView: TextView = itemView.findViewById(R.id.orderTextView)
            val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
            val cancelButton: Button = itemView.findViewById(R.id.cancelButton)
        }
    }
}