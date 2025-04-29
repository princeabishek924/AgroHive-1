package com.example.agrohive_1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class ListingDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var priceTextView: TextView
    private lateinit var availableQuantityTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var decreaseQuantityButton: Button
    private lateinit var increaseQuantityButton: Button
    private lateinit var orderQuantityTextView: TextView
    private lateinit var orderAmountTextView: TextView
    private lateinit var placeOrderButton: Button
    private lateinit var chatButton: Button
    private lateinit var contactButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private var listing: Listing? = null
    private var orderQuantity = 1
    private val handler = Handler(Looper.getMainLooper())
    private var quantityUpdateRunnable: Runnable? = null
    private var userRole: String? = null
    private var userName: String = "User"
    private var firebaseUid: String = ""
    private var userType: String = "customer"
    private var userPincode: String = "Unknown"
    private var userCity: String = "Unknown"
    private lateinit var sharedPreferences: SharedPreferences
    private val CALL_PHONE_PERMISSION_REQUEST_CODE = 100
    private var isChatButtonEnabled = true
    private val chatButtonDebounceDelay = 1000L // 1 second

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_detail)

        auth = FirebaseAuth.getInstance()
        imageView = findViewById(R.id.listingImageView)
        nameTextView = findViewById(R.id.listingNameTextView)
        priceTextView = findViewById(R.id.listingPriceTextView)
        availableQuantityTextView = findViewById(R.id.listingAvailableQuantityTextView)
        locationTextView = findViewById(R.id.listingLocationTextView)
        decreaseQuantityButton = findViewById(R.id.decreaseQuantityButton)
        increaseQuantityButton = findViewById(R.id.increaseQuantityButton)
        orderQuantityTextView = findViewById(R.id.orderQuantityTextView)
        orderAmountTextView = findViewById(R.id.orderAmountTextView)
        placeOrderButton = findViewById(R.id.placeOrderButton)
        chatButton = findViewById(R.id.chatButton)
        contactButton = findViewById(R.id.contactButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        applyFontSize()

        userName = intent.getStringExtra("userName") ?: "User"
        firebaseUid = intent.getStringExtra("firebaseUid") ?: auth.currentUser?.uid ?: ""
        userType = intent.getStringExtra("userType")?.lowercase() ?: "customer"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"
        listing = intent.getSerializableExtra("listing") as? Listing
        fetchUserRole()

        if (listing != null) {
            nameTextView.text = listing!!.name
            priceTextView.text = "₹${listing!!.price}"
            updateQuantityDisplay()
            locationTextView.text = "Location: ${listing!!.location}"

            Glide.with(this)
                .load(listing!!.imageUrl)
                .placeholder(R.drawable.placeholder_image_bg)
                .error(R.drawable.placeholder_image_bg)
                .into(imageView)

            updateAmount()
            startQuantityUpdate()
        } else {
            Log.e("ListingDetail", "No listing data provided")
            Toast.makeText(this, "Failed to load listing details.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        decreaseQuantityButton.setOnClickListener {
            if (orderQuantity > 1) {
                orderQuantity--
                orderQuantityTextView.text = orderQuantity.toString()
                updateAmount()
            }
        }

        increaseQuantityButton.setOnClickListener {
            listing?.quantity?.let { maxQty ->
                if (orderQuantity < maxQty) {
                    orderQuantity++
                    orderQuantityTextView.text = orderQuantity.toString()
                    updateAmount()
                } else {
                    Toast.makeText(this, "Maximum quantity reached.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        placeOrderButton.setOnClickListener {
            if (orderQuantity > 0 && orderQuantity <= (listing?.quantity ?: 0)) {
                placeOrder(listing?.userId ?: "")
            } else {
                Toast.makeText(this, "Invalid quantity selected.", Toast.LENGTH_SHORT).show()
            }
        }

        chatButton.setOnClickListener {
            if (isChatButtonEnabled) {
                isChatButtonEnabled = false
                handler.postDelayed({ isChatButtonEnabled = true }, chatButtonDebounceDelay)
                listing?.userId?.let { ownerUid ->
                    if (ownerUid == firebaseUid) {
                        Toast.makeText(this, "You cannot chat with yourself.", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    Log.d("ListingDetail", "Initiating chat with owner: $ownerUid")
                    initiateChat(ownerUid)
                } ?: run {
                    Log.e("ListingDetail", "Missing ownerUid for listing")
                    Toast.makeText(this, "Failed to load chat details.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        contactButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_PERMISSION_REQUEST_CODE)
            } else {
                fetchProductDetails { productDetail ->
                    productDetail?.userPhone?.let { phone ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        startActivity(intent)
                    } ?: Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchProductDetails { productDetail ->
                    productDetail?.userPhone?.let { phone ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        startActivity(intent)
                    } ?: Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Call permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        applyFontSize()
        startQuantityUpdate()
        if (userRole != null) {
            setupBottomNavigation()
        }
    }

    override fun onPause() {
        super.onPause()
        quantityUpdateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun applyTheme() {
        try {
            val theme = sharedPreferences.getString("theme", "light")
            if (theme == "dark") {
                setTheme(R.style.AgroTheme_Dark)
            } else {
                setTheme(R.style.AgroTheme_Light)
            }
            Log.d("ListingDetail", "Applied theme: $theme")
        } catch (e: Exception) {
            Log.e("ListingDetail", "Error applying theme: ${e.message}", e)
            setTheme(R.style.AgroTheme_Light)
        }
    }

    private fun applyFontSize() {
        try {
            val fontSize = sharedPreferences.getString("font_size", "medium")
            val textSize = when (fontSize) {
                "small" -> 14f
                "large" -> 20f
                else -> 16f
            }
            listOf(nameTextView, priceTextView, availableQuantityTextView, locationTextView, orderQuantityTextView, orderAmountTextView).forEach { textView ->
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            }
            listOf(decreaseQuantityButton, increaseQuantityButton, placeOrderButton, chatButton, contactButton).forEach { button ->
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            }
            Log.d("ListingDetail", "Applied font size: $fontSize ($textSize sp)")
        } catch (e: Exception) {
            Log.e("ListingDetail", "Error applying font size: ${e.message}", e)
        }
    }

    private fun showOrderNotification(order: Order) {
        val channelId = "order_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Order Placed")
            .setContentText("Order for ${listing?.name} (${order.quantity} ${listing?.unit ?: "units"}) placed successfully!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun updateAmount() {
        listing?.price?.let { price ->
            val amount = price * orderQuantity
            orderAmountTextView.text = "Amount: ₹$amount"
        }
    }

    private fun startQuantityUpdate() {
        quantityUpdateRunnable?.let { handler.removeCallbacks(it) }
        quantityUpdateRunnable = Runnable {
            fetchUpdatedQuantity()
            handler.postDelayed(quantityUpdateRunnable!!, 5000)
        }
        handler.post(quantityUpdateRunnable!!)
    }

    private fun fetchUpdatedQuantity() {
        listing?.let { lst ->
            val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
            apiService.getListingQuantity(lst.userId).enqueue(object : Callback<OrderResponse> {
                override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                    if (response.isSuccessful) {
                        val updatedQty = response.body()?.updatedQuantity ?: lst.quantity
                        listing = listing?.copy(quantity = updatedQty)
                        updateQuantityDisplay()
                        if (orderQuantity > updatedQty) {
                            orderQuantity = updatedQty
                            orderQuantityTextView.text = orderQuantity.toString()
                            updateAmount()
                        }
                    } else {
                        Log.e("ListingDetail", "Failed to fetch quantity: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                    Log.e("ListingDetail", "Network error fetching quantity: ${t.message}")
                }
            })
        }
    }

    private fun updateQuantityDisplay() {
        listing?.quantity?.let { qty ->
            availableQuantityTextView.text = "Available: $qty ${listing?.unit ?: "units"}"
        }
    }

    private fun placeOrder(listingId: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val order = Order(
            id = listingId,
            listingId = listingId,
            userId = firebaseUid,
            quantity = orderQuantity
        )
        apiService.placeOrder(order).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ListingDetailActivity, response.body()?.message ?: "Order placed!", Toast.LENGTH_SHORT).show()
                    showOrderNotification(order)
                    fetchUpdatedQuantity()
                    val intent = Intent(this@ListingDetailActivity, OrdersActivity::class.java).apply {
                        putExtra("userName", userName)
                        putExtra("firebaseUid", firebaseUid)
                        putExtra("userType", userType)
                        putExtra("userPincode", userPincode)
                        putExtra("userCity", userCity)
                    }
                    startActivity(intent)
                } else {
                    Log.e("ListingDetail", "Failed to place order: ${response.code()}")
                    Toast.makeText(this@ListingDetailActivity, "Failed to place order: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Log.e("ListingDetail", "Network error placing order: ${t.message}")
                Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserRole() {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        firebaseUid.takeIf { it.isNotEmpty() }?.let { uid ->
            apiService.getUser(uid).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        userRole = response.body()?.userType
                        if (userRole?.lowercase() != "customer") {
                            Log.w("ListingDetail", "Non-customer user attempted access: $userRole")
                            Toast.makeText(this@ListingDetailActivity, "Access restricted. Only customers can view this page.", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            bottomNavigationView.menu.clear()
                            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_customer)
                            bottomNavigationView.visibility = View.VISIBLE
                            setupBottomNavigation()
                        }
                    } else {
                        Log.e("ListingDetail", "Failed to fetch user role: ${response.code()}")
                        Toast.makeText(this@ListingDetailActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("ListingDetail", "Network error fetching user role: ${t.message}")
                    Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        } ?: run {
            Log.e("ListingDetail", "No Firebase UID available")
            Toast.makeText(this@ListingDetailActivity, "Invalid user data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initiateChat(ownerUid: String) {
        if (isFinishing || isDestroyed) {
            Log.w("ListingDetail", "Activity is finishing, aborting chat initiation")
            return
        }
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getChats(firebaseUid).enqueue(object : Callback<List<Chat>> {
            override fun onResponse(call: Call<List<Chat>>, response: Response<List<Chat>>) {
                if (response.isSuccessful) {
                    val chats = response.body() ?: emptyList()
                    Log.d("ListingDetail", "Fetched ${chats.size} chats for user: $firebaseUid")
                    val existingChat = chats.find { chat ->
                        chat.participants.contains(ownerUid) && chat.participants.contains(firebaseUid)
                    }
                    if (existingChat != null) {
                        Log.d("ListingDetail", "Found existing chat: ${existingChat.id}")
                        navigateToChat(existingChat)
                    } else {
                        Log.d("ListingDetail", "No existing chat found, creating new chat with owner: $ownerUid")
                        createNewChat(ownerUid)
                    }
                } else {
                    Log.e("ListingDetail", "Failed to fetch chats: ${response.code()}")
                    Toast.makeText(this@ListingDetailActivity, "Failed to load chats: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Chat>>, t: Throwable) {
                Log.e("ListingDetail", "Network error fetching chats: ${t.message}")
                Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNewChat(ownerUid: String) {
        if (isFinishing || isDestroyed) {
            Log.w("ListingDetail", "Activity is finishing, aborting chat creation")
            return
        }
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getUser(ownerUid).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val ownerName = response.body()?.username ?: "Unknown User"
                    Log.d("ListingDetail", "Fetched owner details: $ownerName ($ownerUid)")
                    val message = MessageRequest(
                        chatId = "",
                        senderId = firebaseUid,
                        receiverId = ownerUid,
                        text = "Hello! I'm interested in your listing: ${listing?.name}",
                        timestamp = System.currentTimeMillis(),
                        status = "sent"
                    )
                    apiService.sendMessage(message).enqueue(object : Callback<Message> {
                        override fun onResponse(call: Call<Message>, response: Response<Message>) {
                            if (response.isSuccessful) {
                                val sentMessage = response.body()
                                if (sentMessage?.chatId != null && sentMessage.chatId.isNotEmpty()) {
                                    Log.d("ListingDetail", "Message sent, chatId: ${sentMessage.chatId}")
                                    apiService.getChats(firebaseUid).enqueue(object : Callback<List<Chat>> {
                                        override fun onResponse(call: Call<List<Chat>>, response: Response<List<Chat>>) {
                                            if (response.isSuccessful) {
                                                val chats = response.body() ?: emptyList()
                                                val newChat = chats.find { it.id == sentMessage.chatId }
                                                if (newChat != null && newChat.participants.contains(ownerUid)) {
                                                    Log.d("ListingDetail", "Found new chat: ${newChat.id}")
                                                    navigateToChat(newChat)
                                                } else {
                                                    Log.e("ListingDetail", "New chat not found for chatId: ${sentMessage.chatId} or missing ownerUid")
                                                    Toast.makeText(this@ListingDetailActivity, "Failed to find new chat", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Log.e("ListingDetail", "Failed to fetch new chat: ${response.code()}")
                                                Toast.makeText(this@ListingDetailActivity, "Failed to load new chat: ${response.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onFailure(call: Call<List<Chat>>, t: Throwable) {
                                            Log.e("ListingDetail", "Network error fetching new chat: ${t.message}")
                                            Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                } else {
                                    Log.e("ListingDetail", "Message response missing or empty chatId: $sentMessage")
                                    Toast.makeText(this@ListingDetailActivity, "Failed to create chat: Invalid message data", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.e("ListingDetail", "Failed to send message: ${response.code()}")
                                Toast.makeText(this@ListingDetailActivity, "Failed to create chat: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Message>, t: Throwable) {
                            Log.e("ListingDetail", "Network error sending message: ${t.message}")
                            Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Log.e("ListingDetail", "Failed to fetch owner details: ${response.code()}")
                    Toast.makeText(this@ListingDetailActivity, "Failed to load owner details: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e("ListingDetail", "Network error fetching owner: ${t.message}")
                Toast.makeText(this@ListingDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToChat(chat: Chat) {
        if (isFinishing || isDestroyed) {
            Log.w("ListingDetail", "Activity is finishing, aborting navigation to chat")
            return
        }
        if (chat.id.isEmpty() || chat.participants.size < 2) {
            Log.e("ListingDetail", "Invalid chat data - chatId: ${chat.id}, participants: ${chat.participants}")
            Toast.makeText(this, "Invalid chat data", Toast.LENGTH_SHORT).show()
            return
        }
        val participantUid = chat.participants.firstOrNull { it != firebaseUid }
        if (participantUid == null) {
            Log.e("ListingDetail", "No valid participant found in chat: ${chat.id}")
            Toast.makeText(this, "Invalid chat participant", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ChatDetailActivity::class.java).apply {
            putExtra("chatId", chat.id)
            putExtra("participantName", chat.otherUserName ?: "Unknown User")
            putExtra("participantUid", participantUid)
            putExtra("userName", userName)
            putExtra("firebaseUid", firebaseUid)
            putExtra("userType", userType)
            putExtra("userPincode", userPincode)
            putExtra("userCity", userCity)
        }
        try {
            startActivity(intent)
            Log.d("ListingDetail", "Navigated to ChatDetailActivity with chatId: ${chat.id}, participantUid: $participantUid")
        } catch (e: Exception) {
            Log.e("ListingDetail", "Error starting ChatDetailActivity: ${e.message}", e)
            Toast.makeText(this, "Unable to open chat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchProductDetails(callback: (ProductDetailResponse?) -> Unit) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        listing?.let { lst ->
            apiService.getProductDetails(lst.userId, lst.imageUrl).enqueue(object : Callback<ProductDetailResponse> {
                override fun onResponse(call: Call<ProductDetailResponse>, response: Response<ProductDetailResponse>) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        Log.e("ListingDetail", "Failed to fetch product details: ${response.code()}")
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<ProductDetailResponse>, t: Throwable) {
                    Log.e("ListingDetail", "Network error fetching product details: ${t.message}")
                    callback(null)
                }
            })
        } ?: callback(null)
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        val intent = Intent(this, HomepageActivity::class.java).apply {
                            putExtra("userName", userName)
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userType", userType)
                            putExtra("userPincode", userPincode)
                            putExtra("userCity", userCity)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.nav_chat -> {
                        val intent = Intent(this, MessageActivity::class.java).apply {
                            putExtra("userName", userName)
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userType", userType)
                            putExtra("userPincode", userPincode)
                            putExtra("userCity", userCity)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.nav_profile -> {
                        val intent = Intent(this, EditProfileActivity::class.java).apply {
                            putExtra("userName", userName)
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userType", userType)
                            putExtra("userPincode", userPincode)
                            putExtra("userCity", userCity)
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.nav_others -> {
                        val intent = Intent(this, OthersActivity::class.java).apply {
                            putExtra("userName", userName)
                            putExtra("firebaseUid", firebaseUid)
                            putExtra("userType", userType)
                            putExtra("userPincode", userPincode)
                            putExtra("userCity", userCity)
                        }
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.e("ListingDetail", "Error setting up bottom navigation: ${e.message}", e)
            Toast.makeText(this, "Navigation setup failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Utility to parse ISO 8601 timestamp to Long (optional)
    private fun parseIso8601ToLong(isoString: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoString)?.time
        } catch (e: Exception) {
            Log.e("ListingDetail", "Error parsing ISO 8601 timestamp: $isoString, ${e.message}")
            null
        }
    }
}