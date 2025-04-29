package com.example.agrohive_1

import android.R.attr.textSize
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrohive_1.databinding.ItemMessageReceivedBinding
import com.example.agrohive_1.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val userId: String) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private var textSize: Float = 14f

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun setTextSize(size: Float) {
        textSize = size
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == userId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.text
            binding.messageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            binding.timestamp.text = formatTimestamp(message.sentAt)
            binding.timestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 2)
            binding.readIndicator.visibility = when {
                message.readAt != null -> View.VISIBLE
                message.deliveredAt != null -> View.VISIBLE
                else -> View.GONE
            }
            binding.readIndicator.setImageResource(
                if (message.readAt != null) R.drawable.ic_read_blue else R.drawable.ic_unread_black
            )
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.messageText.text = message.text
            binding.messageText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            binding.timestamp.text = formatTimestamp(message.sentAt)
            binding.timestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 2)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

fun MessageAdapter.addMessage(message: Message) {
    val newList = currentList.toMutableList().apply { add(message) }
    submitList(newList)
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
}