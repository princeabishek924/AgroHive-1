package com.example.agrohive_1

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

class ChatbotAdapter(
    private val chatMessages: MutableList<ChatMessage>,
    private val onResponseClick: (ChatResponse?) -> Unit
) : RecyclerView.Adapter<ChatbotAdapter.ChatViewHolder>() {

    private var lastResponse: ChatResponse? = null

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_BOT) {
            R.layout.item_chatbot
        } else {
            R.layout.item_chat_user
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatMessage = chatMessages[position]
        Log.d("ChatbotAdapter", "Binding message at position $position: $chatMessage")

        holder.messageTextView.text = chatMessage.text

        if (chatMessage.isBot && chatMessage.isClickable) {
            val spannableString = SpannableString(chatMessage.text)
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Log.d("ChatbotAdapter", "Clickable message clicked: ${chatMessage.text}")
                        val updatedResponse = if (chatMessage.navigationTarget != null) {
                            lastResponse?.copy(navigationTarget = chatMessage.navigationTarget)
                        } else {
                            lastResponse
                        }
                        onResponseClick(updatedResponse)
                    }
                },
                0,
                chatMessage.text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.messageTextView.text = spannableString
            holder.messageTextView.movementMethod = LinkMovementMethod.getInstance()
        } else if (chatMessage.isBot && lastResponse?.listings?.isNotEmpty() == true && position == chatMessages.size - 1) {
            holder.messageTextView.append("\n\n")
            lastResponse?.listings?.forEachIndexed { index, listing ->
                val listingText = "${index + 1}. ${listing.name} - ₹${listing.price}\n"
                val spannableString = SpannableString(listingText)
                spannableString.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            Log.d("ChatbotAdapter", "Listing clicked: ${listing.name}")
                            val intent = Intent(holder.itemView.context, ListingDetailActivity::class.java).apply {
                                putExtra("listing", listing as Serializable)
                            }
                            holder.itemView.context.startActivity(intent)
                        }
                    },
                    0,
                    listingText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.messageTextView.append(spannableString)
            }
            holder.messageTextView.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun getItemCount(): Int = chatMessages.size

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].isBot) VIEW_TYPE_BOT else VIEW_TYPE_USER
    }

    fun updateLastResponse(response: ChatResponse?) {
        Log.d("ChatbotAdapter", "Updating lastResponse: $response")
        lastResponse = response
        notifyItemChanged(chatMessages.size - 1)
    }

    fun addMessage(message: ChatMessage) {
        chatMessages.add(message)
        notifyItemInserted(chatMessages.size - 1)
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }
}