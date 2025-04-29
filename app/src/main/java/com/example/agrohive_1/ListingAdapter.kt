package com.example.agrohive_1

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ListingAdapter(private val listings: List<Listing>, private val userName: String) : RecyclerView.Adapter<ListingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.listingImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val viewDetailsText: TextView = itemView.findViewById(R.id.viewDetailsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        Glide.with(holder.itemView.context)
            .load(listing.imageUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(holder.imageView)
        holder.nameTextView.text = listing.name
        holder.priceTextView.text = "Price: ₹${listing.price} / ${listing.unit}"
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val listedDate = listing.createdAt?.let {
            try {
                val date = inputFormat.parse(it)
                if (date != null) {
                    dateFormat.format(date)
                }
            } catch (e: Exception) {
                "Unknown"
            }
        } ?: "Unknown"
        holder.dateTextView.text = "Listed on: $listedDate"

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ListingDetailActivity::class.java)
            intent.putExtra("listing", listing)
            intent.putExtra("userName", userName)
            holder.itemView.context.startActivity(intent)
        }

        holder.viewDetailsText.setOnClickListener {
            val intent = Intent(holder.itemView.context, ListingDetailActivity::class.java)
            intent.putExtra("listing", listing)
            intent.putExtra("userName", userName)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listings.size
}