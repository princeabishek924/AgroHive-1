package com.example.agrohive_1

data class MessageRequest(
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val status: String
)