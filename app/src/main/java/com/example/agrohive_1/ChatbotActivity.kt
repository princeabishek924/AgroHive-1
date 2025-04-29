package com.example.agrohive_1

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class ChatbotActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var backButton: ImageView
    private lateinit var chatAdapter: ChatbotAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private var userName: String = "User"
    private var userPincode: String = "Unknown"
    private var userCity: String = "Unknown"
    private var firebaseUid: String = ""
    private var isWaitingForConfirmation: Boolean = false
    private var pendingQuery: String? = null
    private var pendingLocation: String? = null
    private var pendingRadius: String = "15km"
    private var pendingPriceFrom: Double = 0.0
    private var pendingPriceTo: Double = 999.0
    private var pendingCategory: String = "All"
    private lateinit var socket: Socket
    private val geminiApiKey = "AIzaSyC2D3onZsFva__dfrCK1-HTIeggxL-UQgE" // Replace with your Gemini API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        backButton = findViewById(R.id.closeButton)

        userName = intent.getStringExtra("userName") ?: "User"
        userPincode = intent.getStringExtra("userPincode") ?: "Unknown"
        userCity = intent.getStringExtra("userCity") ?: "Unknown"
        firebaseUid = intent.getStringExtra("firebaseUid") ?: ""

        // Initialize Socket.io
        try {
            socket = IO.socket("https://backend-agrohive.onrender.com")
            socket.on(Socket.EVENT_CONNECT, Emitter.Listener {
                Log.d("ChatbotActivity", "Socket connected")
                socket.emit("join", firebaseUid)
            }).on("receiveMessage", Emitter.Listener { args ->
                runOnUiThread {
                    try {
                        val message = args[0] as JSONObject
                        val text = message.getString("text")
                        val senderId = message.getString("senderId")
                        if (senderId != firebaseUid) {
                            addBotMessage("Received: $text")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatbotActivity", "Error parsing message: ${e.message}")
                    }
                }
            }).on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener {
                Log.e("ChatbotActivity", "Socket connection error: ${it.joinToString()}")
                runOnUiThread {
                    Toast.makeText(this, "Socket connection failed", Toast.LENGTH_SHORT).show()
                }
            })
            socket.connect()
        } catch (e: Exception) {
            Log.e("ChatbotActivity", "Socket initialization error: ${e.message}")
            Toast.makeText(this, "Failed to initialize socket", Toast.LENGTH_SHORT).show()
        }

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatbotAdapter(chatMessages) { response: ChatResponse? ->
            response?.let { res ->
                if (res.navigationTarget != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToActivity(res.navigationTarget)
                    }, 1000)
                } else {
                    navigateToHomepageWithListings()
                }
            }
        }
        chatRecyclerView.adapter = chatAdapter

        sendButton.setOnClickListener { sendMessage() }
        backButton.setOnClickListener {
            val intent = Intent(this, HomepageActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("firebaseUid", firebaseUid)
            intent.putExtra("userPincode", userPincode)
            intent.putExtra("userCity", userCity)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            socket.disconnect()
        }

        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        addBotMessage("Hello $userName! I see you're in $userCity (Pincode: $userPincode). How can I assist you today? Type something like 'apples in 641107 within 15km' to search for listings, 'details about apples' or 'advantages of apples' for product info, ask about agriculture like 'best crops in Tamil Nadu', or app actions like 'edit profile' or 'my listings'.")
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }

    private fun sendMessage() {
        val message = messageEditText.text.toString().trim()
        if (message.isEmpty()) return

        addUserMessage(message)
        messageEditText.text.clear()

        if (isWaitingForConfirmation) {
            handleConfirmation(message)
        } else {
            processUserInput(message)
        }
    }

    private fun addUserMessage(text: String) {
        chatMessages.add(ChatMessage(text = text, isBot = false))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun addBotMessage(text: String, isClickable: Boolean = false, navigationTarget: String? = null) {
        chatMessages.add(ChatMessage(text = text, isBot = true, isClickable = isClickable, navigationTarget = navigationTarget))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun processUserInput(input: String) {
        val lowercaseInput = input.lowercase()
        val tokens = lowercaseInput.split("\\s+".toRegex())

        // Check for product detail or advantage requests
        val isProductDetailRequest = lowercaseInput.startsWith("details about") || lowercaseInput.startsWith("info about") || lowercaseInput.contains("details of")
        val isAdvantagesRequest = lowercaseInput.startsWith("advantages of") || lowercaseInput.contains("benefits of")
        if (isProductDetailRequest || isAdvantagesRequest) {
            val product = when {
                isProductDetailRequest -> lowercaseInput.replace("details about", "").replace("info about", "").replace("details of", "").trim()
                isAdvantagesRequest -> lowercaseInput.replace("advantages of", "").replace("benefits of", "").trim()
                else -> ""
            }
            if (product.isNotEmpty()) {
                val prompt = if (isProductDetailRequest) {
                    "Provide detailed information about $product, including description and uses in agriculture."
                    "Explain the production and cultivation methods for $product in agriculture."
                    "List the types or varieties of $product available in agriculture."
                    "Describe the origin and history of $product in agriculture."
                    "Explain the quality factors and standards for $product in agriculture."
                    "Describe the uses of $product in agriculture, food, and industry."
                    "Describe the characteristics and properties of $product in agriculture."
                    "Explain the production and cultivation methods for $product in agriculture."
                    "Respond to any question regarding agriculture and its domain"
                } else {
                    val queryType = ""
                    "Provide information about $product related to $queryType in agriculture."

                }
                sendToGeminiApi(prompt)
                return
            } else {
                addBotMessage("Please specify a product (e.g., 'details about apples' or 'advantages of apples').")
                return
            }
        }

        // Check for navigation intents
        val editProfileKeywords = listOf("edit profile", "update profile", "change profile", "edit my profile", "profil adit")
        val isEditProfileIntent = editProfileKeywords.any { lowercaseInput.contains(it) } ||
                (tokens.contains("edit") && tokens.contains("profil")) ||
                (tokens.contains("update") && tokens.contains("profil")) ||
                (tokens.contains("adit") && tokens.contains("profil"))

        val myListingsKeywords = listOf("my listings", "mi listang", "my listing", "view my listings")
        val isMyListingsIntent = myListingsKeywords.any { lowercaseInput.contains(it) } ||
                (tokens.contains("my") && tokens.contains("list"))

        val createListingKeywords = listOf("create listing", "add listing", "new listing")
        val isCreateListingIntent = createListingKeywords.any { lowercaseInput.contains(it) } ||
                (tokens.contains("create") && tokens.contains("listing")) ||
                (tokens.contains("add") && tokens.contains("listing"))

        val isNavigationIntent = isEditProfileIntent || isMyListingsIntent || isCreateListingIntent ||
                lowercaseInput.contains("change address") || lowercaseInput.contains("update address")

        if (isNavigationIntent) {
            val navigationTarget = when {
                isEditProfileIntent -> "EditProfileActivity"
                isMyListingsIntent -> "MyListingsActivity"
                isCreateListingIntent -> "CreateListingActivity"
                lowercaseInput.contains("change address") || lowercaseInput.contains("update address") -> "EditProfileActivity"
                else -> null
            }
            if (navigationTarget != null) {
                sendToBot(input, navigationTarget)
            } else {
                addBotMessage("Unrecognized navigation command. Try 'edit profile', 'my listings', or 'create listing'.")
            }
            return
        }

        // Check for agriculture-related questions
        if (handleAgricultureQuestion(input)) {
            return
        }

        // Check if the input looks like a product search
        val pincodePattern = "\\d{6}".toRegex()
        val hasPincode = tokens.any { it.matches(pincodePattern) }
        val hasSearchKeywords = tokens.any { it == "in" || it == "within" }
        val isProductSearch = hasPincode || hasSearchKeywords

        if (isProductSearch) {
            val (query, location, radius, priceFrom, priceTo, category) = parseUserInput(input)

            query.ifEmpty {
                addBotMessage("Please specify a product to search for (e.g., 'apples in 641107 within 15km').")
                return
            }

            val searchLocation = if (location.isEmpty()) userPincode else location
            if (searchLocation == "Unknown") {
                addBotMessage("I don't have your location. Please provide a pincode (e.g., 'apples in 641107').")
                return
            }

            pendingQuery = query
            pendingLocation = searchLocation
            pendingRadius = radius
            pendingPriceFrom = priceFrom
            pendingPriceTo = priceTo
            pendingCategory = category

            val cityForPincode = if (searchLocation == userPincode) userCity else getCityFromPincode()
            addBotMessage("Looking for '$query' in $cityForPincode (Pincode: $searchLocation) within $radius. Is that correct? (Reply 'yes' or 'no')")
            isWaitingForConfirmation = true
        } else {
            addBotMessage("I can assist with product searches (e.g., 'apples in 641107 within 15km'), product details (e.g., 'details about apples'), advantages (e.g., 'advantages of apples'), agriculture questions (e.g., 'best crops in Tamil Nadu'), or app actions (e.g., 'edit profile', 'my listings'). Please try again.")
        }
    }

    private fun handleAgricultureQuestion(input: String): Boolean {
        val lowercaseInput = input.lowercase()
        val agricultureKeywords = listOf(
            "crop", "crops", "soil", "fertilizer", "pest", "pests", "market price", "price of",
            "agriculture", "farming", "irrigation", "seeds", "harvest", "yield", "planting",
            "cultivation", "disease", "weather", "organic", "pesticide", "compost", "manure"
        )
        val isAgricultureRelated = agricultureKeywords.any { lowercaseInput.contains(it) }

        if (isAgricultureRelated) {
            sendToBot(input, null)
            return true
        }
        return false
    }

    private fun sendToGeminiApi(prompt: String) {
        val apiService = GeminiApiClient.instance
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt)
                    )
                )
            )
        )
        apiService.generateContent(geminiApiKey, request).enqueue(object : Callback<GeminiResponse> {
            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                if (response.isSuccessful) {
                    val geminiResponse = response.body()
                    val answer = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    Log.d("ChatbotActivity", "Gemini response: $answer")
                    if (answer != null) {
                        val chatResponse = ChatResponse(
                            navigationTarget = null,
                            listings = emptyList(),
                            answer = answer,
                            productDetails = null
                        )
                        addBotMessage(answer)
                        chatAdapter.updateLastResponse(chatResponse)
                    } else {
                        addBotMessage("Sorry, I couldn't retrieve information from Gemini API.")
                    }
                } else {
                    Log.e("ChatbotActivity", "Gemini API failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    addBotMessage("Failed to fetch information from Gemini API. Please try again!")
                }
            }

            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                Log.e("ChatbotActivity", "Gemini API network error: ${t.message}")
                addBotMessage("Network error while contacting Gemini API. Please try again!")
                Toast.makeText(this@ChatbotActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendToBot(message: String, fallbackNavigationTarget: String? = null) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val request = ChatBotRequest(message, firebaseUid)
        apiService.sendChatBotMessage(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    Log.d("ChatbotActivity", "Bot response: $chatResponse")
                    val botText = when {
                        chatResponse?.listings?.isNotEmpty() == true -> {
                            "Found ${chatResponse.listings.size} listings:\n" +
                                    chatResponse.listings.joinToString("\n") { "${it.name} - ₹${it.price}" }
                        }
                        chatResponse?.productDetails != null -> {
                            chatResponse.productDetails.let { details ->
                                buildString {
                                    append("Details for ${details.name}:\n")
                                    if (details.description != null) append("Description: ${details.description}\n")
                                    if (details.origin != null) append("Origin: ${details.origin}\n")
                                    if (details.quality != null) append("Quality: ${details.quality}\n")
                                    if (details.sellerInfo != null) append("Seller: ${details.sellerInfo}\n")
                                    if (details.additionalInfo != null) append("Additional Info: ${details.additionalInfo}\n")
                                }
                            }
                        }
                        chatResponse?.answer != null -> {
                            Log.d("ChatbotActivity", "Agriculture answer: ${chatResponse.answer}")
                            chatResponse.answer
                        }
                        chatResponse?.navigationTarget != null -> {
                            Log.d("ChatbotActivity", "Navigation target: ${chatResponse.navigationTarget}")
                            "Navigating to ${chatResponse.navigationTarget.replace("Activity", "")}..."
                        }
                        else -> {
                            Log.w("ChatbotActivity", "No valid response fields")
                            if (fallbackNavigationTarget != null) {
                                "Navigating to ${fallbackNavigationTarget.replace("Activity", "")}..."
                            } else {
                                "Sorry, I couldn't find a relevant response."
                            }
                        }
                    }
                    val effectiveNavigationTarget = chatResponse?.navigationTarget ?: fallbackNavigationTarget
                    val botMessage = ChatMessage(
                        text = botText,
                        isBot = true,
                        isClickable = effectiveNavigationTarget != null,
                        navigationTarget = effectiveNavigationTarget
                    )
                    addBotMessage(botMessage.text, botMessage.isClickable, botMessage.navigationTarget)
                    chatAdapter.updateLastResponse(chatResponse)
                    if (chatResponse?.listings?.isNotEmpty() == true) {
                        initiateChatWithUser(
                            chatResponse.listings.first().userId,
                            message,
                            chatResponse.listings.first().id.toString()
                        )
                    }
                } else {
                    Log.e("ChatbotActivity", "Failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    val botText = if (fallbackNavigationTarget != null) {
                        "Navigating to ${fallbackNavigationTarget.replace("Activity", "")}..."
                    } else {
                        "Sorry, I couldn't process that. Please try again!"
                    }
                    val botMessage = ChatMessage(
                        text = botText,
                        isBot = true,
                        isClickable = fallbackNavigationTarget != null,
                        navigationTarget = fallbackNavigationTarget
                    )
                    addBotMessage(botMessage.text, botMessage.isClickable, botMessage.navigationTarget)
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e("ChatbotActivity", "Network error: ${t.message}")
                val botText = if (fallbackNavigationTarget != null) {
                    "Navigating to ${fallbackNavigationTarget.replace("Activity", "")}..."
                } else {
                    "Network error. Please try again!"
                }
                val botMessage = ChatMessage(
                    text = botText,
                    isBot = true,
                    isClickable = fallbackNavigationTarget != null,
                    navigationTarget = fallbackNavigationTarget
                )
                addBotMessage(botMessage.text, botMessage.isClickable, botMessage.navigationTarget)
                Toast.makeText(this@ChatbotActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleConfirmation(response: String) {
        val lowercaseResponse = response.lowercase()
        when (lowercaseResponse) {
            "yes", "y" -> {
                isWaitingForConfirmation = false
                when (pendingQuery) {
                    "edit_profile_intent" -> {
                        addBotMessage("Let's edit your profile.")
                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateToActivity("EditProfileActivity")
                        }, 1000)
                    }
                    "my_listings_intent" -> {
                        addBotMessage("Let's view your listings.")
                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateToActivity("MyListingsActivity")
                        }, 1000)
                    }
                    else -> {
                        fetchListings(
                            query = pendingQuery ?: "",
                            location = pendingLocation ?: userPincode,
                            radius = pendingRadius,
                            priceFrom = pendingPriceFrom,
                            priceTo = pendingPriceTo,
                            category = pendingCategory
                        )
                    }
                }
            }
            "no", "n" -> {
                isWaitingForConfirmation = false
                addBotMessage("Okay, let's try again. Please specify your request (e.g., 'edit profile', 'details about apples', or 'apples in 641107 within 15km').")
                clearPendingData()
            }
            else -> {
                addBotMessage("Please reply with 'yes' or 'no' to confirm.")
            }
        }
    }

    private fun parseUserInput(input: String): QueryParams {
        val lowercaseInput = input.lowercase()
        var query: String
        var location: String
        var radius = "15km"
        var priceFrom = 0.0
        var priceTo = 999.0
        var category = "All"

        val tokens = lowercaseInput.split("\\s+".toRegex())

        val pincodePattern = "\\d{6}".toRegex()
        val pincode = tokens.find { it.matches(pincodePattern) }

        val radiusPattern = "(\\d+\\s*(km|m))".toRegex()
        val radiusMatch = radiusPattern.find(lowercaseInput)
        if (radiusMatch != null) {
            radius = radiusMatch.groupValues[1]
        }

        val priceRangePattern = "from\\s*₹?(\\d+(\\.\\d+)?)\\s*to\\s*₹?(\\d+(\\.\\d+)?)".toRegex()
        val priceMatch = priceRangePattern.find(lowercaseInput)
        if (priceMatch != null) {
            priceFrom = priceMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            priceTo = priceMatch.groupValues[3].toDoubleOrNull() ?: 999.0
        }

        val categories = listOf("fruits", "vegetables", "grains")
        val foundCategory = tokens.find { categories.contains(it) }
        if (foundCategory != null) {
            category = foundCategory.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }

        val keywordsToRemove = listOf("in", "within Sicilian", "from", "to", radius, foundCategory ?: "") + categories
        val filteredTokens = tokens.filter { token ->
            token != pincode && !keywordsToRemove.contains(token) && !token.matches(priceRangePattern) && !token.matches(radiusPattern)
        }

        query = filteredTokens.joinToString(" ").trim()
        location = pincode ?: ""

        return QueryParams(query, location, radius, priceFrom, priceTo, category)
    }

    private fun getCityFromPincode(): String {
        return "Unknown City"
    }

    private fun fetchListings(query: String, location: String, radius: String, priceFrom: Double, priceTo: Double, category: String) {
        val normalizedQuery = query.trim().lowercase()
        Log.d("ChatbotActivity", "Fetching listings with query='$normalizedQuery', location='$location', radius='$radius', firebaseUid='$firebaseUid', priceFrom=$priceFrom, priceTo=$priceTo, category='$category'")
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        apiService.getListings(normalizedQuery, location, radius, firebaseUid, priceFrom, priceTo, category).enqueue(object : Callback<List<Listing>> {
            override fun onResponse(call: Call<List<Listing>>, response: Response<List<Listing>>) {
                if (response.isSuccessful) {
                    val listings = response.body() ?: emptyList()
                    Log.d("ChatbotActivity", "Received ${listings.size} listings: ${listings.joinToString { it.name }}")
                    if (listings.isNotEmpty()) {
                        val responseMessage = "Found ${listings.size} listings for '$query' in $location within $radius."
                        addBotMessage(responseMessage)
                        chatAdapter.updateLastResponse(ChatResponse(
                            listings = listings,
                            navigationTarget = null,
                            answer = null,
                            productDetails = null
                        ))
                        initiateChatWithUser(
                            listings.first().userId,
                            query,
                            listings.first().id.toString()
                        )
                    } else {
                        addBotMessage("No listings found for '$query' in $location within $radius.")
                    }
                } else {
                    Log.e("ChatbotActivity", "Failed to fetch listings: HTTP ${response.code()} - ${response.errorBody()?.string()}")
                    addBotMessage("Failed to fetch listings. Please try again later.")
                }
            }

            override fun onFailure(call: Call<List<Listing>>, t: Throwable) {
                Log.e("ChatbotActivity", "Error fetching listings: ${t.message}", t)
                addBotMessage("Error fetching listings: ${t.message}")
            }
        })
    }

    private fun initiateChatWithUser(userId: String, initialMessage: String, listingId: String) {
        val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)
        val chatRequest = ChatRequest(
            participants = listOf(firebaseUid, userId),
            listingId = listingId,
            lastMessage = "",
            lastMessageAt = System.currentTimeMillis().toString(),
            lastMessageSenderId = ""
        )
        apiService.createChat(chatRequest).enqueue(object : Callback<Chat> {
            override fun onResponse(call: Call<Chat>, response: Response<Chat>) {
                if (response.isSuccessful) {
                    val chat = response.body()
                    Log.d("ChatbotActivity", "Created chat: $chat")
                    val messageRequest = MessageRequest(
                        chatId = chat?.id ?: "",
                        senderId = firebaseUid,
                        receiverId = userId,
                        text = "Interested in your $initialMessage listing.",
                        timestamp = System.currentTimeMillis(),
                        status = "sent"
                    )
                    apiService.sendMessage(messageRequest).enqueue(object : Callback<Message> {
                        override fun onResponse(call: Call<Message>, response: Response<Message>) {
                            if (response.isSuccessful) {
                                Log.d("ChatbotActivity", "Sent message: ${response.body()}")
                                val messageData = JSONObject().apply {
                                    put("chatId", chat?.id)
                                    put("senderId", firebaseUid)
                                    put("receiverId", userId)
                                    put("text", messageRequest.text)
                                    put("listingId", listingId)
                                }
                                socket.emit("sendMessage", messageData)
                                Toast.makeText(this@ChatbotActivity, "Message sent to user!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("ChatbotActivity", "Failed to send message: ${response.code()}")
                                Toast.makeText(this@ChatbotActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Message>, t: Throwable) {
                            Log.e("ChatbotActivity", "Network error sending message: ${t.message}")
                            Toast.makeText(this@ChatbotActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Log.e("ChatbotActivity", "Failed to create chat: ${response.code()}")
                    Toast.makeText(this@ChatbotActivity, "Failed to create chat", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Chat>, t: Throwable) {
                Log.e("ChatbotActivity", "Network error creating chat: ${t.message}")
                Toast.makeText(this@ChatbotActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToActivity(target: String) {
        val intent = when (target) {
            "EditProfileActivity" -> Intent(this, EditProfileActivity::class.java)
            "MyListingsActivity" -> Intent(this, MyListingsActivity::class.java)
            "CreateListingActivity" -> Intent(this, CreateListingActivity::class.java)
            "ProfileActivity" -> Intent(this, EditProfileActivity::class.java)
            else -> {
                Log.w("ChatbotActivity", "Unknown navigation target: $target")
                return
            }
        }
        intent.putExtra("userName", userName)
        intent.putExtra("firebaseUid", firebaseUid)
        intent.putExtra("userPincode", userPincode)
        intent.putExtra("userCity", userCity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun navigateToHomepageWithListings() {
        val intent = Intent(this, HomepageActivity::class.java)
        intent.putExtra("query", pendingQuery)
        intent.putExtra("location", pendingLocation)
        intent.putExtra("radius", pendingRadius)
        intent.putExtra("priceFrom", pendingPriceFrom)
        intent.putExtra("priceTo", pendingPriceTo)
        intent.putExtra("category", pendingCategory)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun clearPendingData() {
        pendingQuery = null
        pendingLocation = null
        pendingRadius = "15km"
        pendingPriceFrom = 0.0
        pendingPriceTo = 999.0
        pendingCategory = "All"
        isWaitingForConfirmation = false
    }

    data class QueryParams(
        val query: String,
        val location: String,
        val radius: String,
        val priceFrom: Double,
        val priceTo: Double,
        val category: String
    )
}