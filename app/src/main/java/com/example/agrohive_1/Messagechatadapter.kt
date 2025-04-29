package com.example.agrohive_1

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agrohive_1.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageChatAdapter(
    internal var chats: List<Chat>,
    private val currentUserId: String,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<MessageChatAdapter.ViewHolder>() {

    private var textSize: Float = 16f

    inner class ViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            binding.participantName.text = chat.otherUserName
            binding.participantName.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            binding.lastMessage.text = chat.lastMessage ?: "No messages yet"
            binding.lastMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 2)
            binding.timestamp.text = chat.lastMessageAt?.let {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
            } ?: ""
            binding.timestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 4)
            binding.readIndicator.setImageResource(
                if (chat.lastMessageSenderId != currentUserId && chat.lastMessageAt != null) {
                    R.drawable.ic_unread_black
                } else {
                    R.drawable.ic_read_blue
                }
            )
            Glide.with(binding.root.context)
                .load(chat.otherUserProfileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(binding.profileImage)

            binding.root.setOnClickListener { onChatClick(chat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }

    fun setTextSize(size: Float) {
        textSize = size
        notifyDataSetChanged()
    }
}