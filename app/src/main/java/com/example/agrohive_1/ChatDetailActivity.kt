package com.example.agrohive_1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var toolbarTitle: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var apiService: ApiService
    private var firebaseUid: String? = null
    private var userName: String? = null
    private var userType: String? = null
    private var userPincode: String? = null
    private var userCity: String? = null
    private var chatId: String? = null
    private var participantUid: String? = null
    private var participantName: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)

        recyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        firebaseUid = intent.getStringExtra("firebaseUid")
        userName = intent.getStringExtra("userName")
        userType = intent.getStringExtra("userType")
        userPincode = intent.getStringExtra("userPincode")
        userCity = intent.getStringExtra("userCity")
        chatId = intent.getStringExtra("chatId")
        participantUid = intent.getStringExtra("participantUid")
        participantName = intent.getStringExtra("participantName")

        toolbarTitle.text = participantName ?: "Chat"

        apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        messageAdapter = MessageAdapter(firebaseUid ?: "anonymous")

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        fetchMessages()

        sendButton.setOnClickListener {
            val text = messageEditText.text.toString().trim()
            if (text.isNotEmpty() && chatId != null && participantUid != null) {
                sendMessage(text)
                messageEditText.text.clear()
            }
        }

        setupBottomNavigation()
    }

    private fun fetchMessages() {
        chatId?.let { id ->
            apiService.getMessages(id).enqueue(object : Callback<List<Message>> {
                override fun onResponse(call: Call<List<Message>>, response: Response<List<Message>>) {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        Log.d("ChatDetailActivity", "Fetched ${messages.size} messages: $messages")
                        messageAdapter.submitList(messages)
                        recyclerView.scrollToPosition(messages.size - 1)
                    } else {
                        Log.e("ChatDetailActivity", "Failed to fetch messages: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@ChatDetailActivity, "Failed to load messages: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Message>>, t: Throwable) {
                    Log.e("ChatDetailActivity", "Network error fetching messages: ${t.message}")
                    Toast.makeText(this@ChatDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            Toast.makeText(this, "Invalid chat data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(text: String) {
        val messageRequest = MessageRequest(
            chatId = chatId ?: "",
            senderId = firebaseUid ?: "anonymous",
            receiverId = participantUid ?: "",
            text = text,
            timestamp = System.currentTimeMillis(),
            status = "sent"
        )
        apiService.sendMessage(messageRequest).enqueue(object : Callback<Message> {
            override fun onResponse(call: Call<Message>, response: Response<Message>) {
                if (response.isSuccessful) {
                    val message = response.body()
                    Log.d("ChatDetailActivity", "Sent message: $message")
                    message?.let { messageAdapter.addMessage(it) }
                    recyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                } else {
                    Log.e("ChatDetailActivity", "Failed to send message: ${response.code()}")
                    Toast.makeText(this@ChatDetailActivity, "Failed to send message: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Message>, t: Throwable) {
                Log.e("ChatDetailActivity", "Network error sending message: ${t.message}")
                Toast.makeText(this@ChatDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(intentFor(HomepageActivity::class.java))
                    true
                }
                R.id.nav_chat -> {
                    startActivity(intentFor(MessageActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(intentFor(EditProfileActivity::class.java))
                    true
                }
                R.id.nav_others -> {
                    startActivity(intentFor(OthersActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_chat
    }

    private fun intentFor(activity: Class<out AppCompatActivity>): Intent {
        return Intent(this, activity).apply {
            putExtra("firebaseUid", firebaseUid)
            putExtra("userName", userName)
            putExtra("userType", userType)
            putExtra("userPincode", userPincode)
            putExtra("userCity", userCity)
        }
    }
}