package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noChatsText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var apiService: ApiService
    private var firebaseUid: String? = null
    private var userName: String? = null
    private var userType: String? = null
    private var userPincode: String? = null
    private var userCity: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        noChatsText = findViewById(R.id.noChatsText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)

        firebaseUid = intent.getStringExtra("firebaseUid")
        userName = intent.getStringExtra("userName")
        userType = intent.getStringExtra("userType")
        userPincode = intent.getStringExtra("userPincode")
        userCity = intent.getStringExtra("userCity")

        chatAdapter = ChatAdapter(emptyList(), { chat ->
            startActivity(Intent(this, ChatDetailActivity::class.java).apply {
                putExtra("chatId", chat.id)
                putExtra("participantUid", chat.participants.firstOrNull { it != firebaseUid })
                putExtra("participantName", chat.otherUserName)
                putExtra("firebaseUid", firebaseUid)
                putExtra("userName", userName)
                putExtra("userType", userType)
                putExtra("userPincode", userPincode)
                putExtra("userCity", userCity)
            })
        })
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageActivity)
            adapter = chatAdapter
        }

        fetchChats()
        setupBottomNavigation()
    }

    private fun fetchChats() {
        progressBar.visibility = View.VISIBLE
        noChatsText.visibility = View.GONE
        firebaseUid?.let { uid ->
            apiService.getChats(uid).enqueue(object : Callback<List<Chat>> {
                override fun onResponse(call: Call<List<Chat>>, response: Response<List<Chat>>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val chats = response.body() ?: emptyList()
                        Log.d("MessageActivity", "Fetched ${chats.size} chats: $chats")
                        chatAdapter.updateChats(chats)
                        noChatsText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Log.e("MessageActivity", "Failed to fetch chats: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@MessageActivity, "Failed to load chats: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Chat>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("MessageActivity", "Network error fetching chats: ${t.message}")
                    Toast.makeText(this@MessageActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            progressBar.visibility = View.GONE
            noChatsText.visibility = View.VISIBLE
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(intentFor(HomepageActivity::class.java))
                    true
                }
                R.id.nav_chat -> {
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

    private fun <T : AppCompatActivity> intentFor(activity: Class<T>): Intent {
        return Intent(this, activity).apply {
            putExtra("userName", userName)
            putExtra("firebaseUid", firebaseUid)
            putExtra("userType", userType)
            putExtra("userPincode", userPincode)
            putExtra("userCity", userCity)
        }
    }
}

class ChatAdapter(
    private var chats: List<Chat>,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(chat: Chat) {
            chatName.text = chat.otherUserName ?: "Unknown User"
            itemView.setOnClickListener { onChatClick(chat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }
}