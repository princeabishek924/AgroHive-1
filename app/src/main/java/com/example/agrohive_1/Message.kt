package com.example.agrohive_1

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val sentAt: Long,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val listingId: String? = null
)

