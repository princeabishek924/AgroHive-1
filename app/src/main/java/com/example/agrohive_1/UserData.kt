package com.example.agrohive_1

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Updated UserData
data class UserData(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val address: Address,
    val userType: String,
    val profileImageUrl: String,
    val firebaseUid: String?,
    val username: String? = null
) : java.io.Serializable

data class Address(
    val doorNo: String,
    val street: String,
    val city: String,
    val district: String,
    val state: String,
    val country: String,
    val pincode: String,
    val area: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : java.io.Serializable

data class UserResponse(
    val name: String,
    val phone: String,
    val address: Address,
    val profileImageUrl: String,
    val password: String? = null,
    val username: String? = null,
    val userType: String? = null
) : java.io.Serializable

data class Listing(
    val id: String?,
    val userId: String,
    val name: String,
    val imageUrl: String,
    val price: Double,
    val quantity: Int,
    val unit: String,
    val category: String,
    val location: String,
    val coordinates: Coordinates,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : java.io.Serializable {
    data class Coordinates(
        val type: String,
        val coordinates: List<Double>
    ) : java.io.Serializable
}

data class ProductDetailResponse(
    val imageUrl: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val userName: String,
    val userPhone: String,
    val userAddress: Address,
    val userId: String,
    val userType: String,
    val description: String,
    val  origin: String? = null
) : java.io.Serializable

data class Order(
    val id: String,
    val listingId: String,
    val userId: String,
    val quantity: Int,
    val status: String = "pending"
) : java.io.Serializable

data class OrderResponse(
    val message: String?,
    val updatedQuantity: Int?,
    val order: Order?
) : java.io.Serializable

data class NotificationResponse(
    val success: Boolean,
    val message: String
) : java.io.Serializable

data class Notification(
    val id: String,
    val userId: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean,
    val fromUid: String,
    val fromType: String
) : java.io.Serializable

data class GeoResponse(
    val lat: String,
    val lon: String
) : java.io.Serializable

data class PincodeResponse(
    val Message: String?,
    val Status: String,
    val PostOffice: List<PincodePostOffice>?
) : java.io.Serializable {
    data class PincodePostOffice(
        val Name: String,
        val District: String,
        val State: String
    ) : java.io.Serializable
}

data class CityPincode(
    val city: String,
    val pincode: String
)

data class DashboardData(
    val sales: List<Pair<String, Int>>,
    val engagement: List<Pair<String, Int>>,
    val revenue: List<Pair<String, Double>>
)

interface GeoApiService {
    @GET("search")
    fun getCoordinates(
        @Query("q") query: String,
        @Query("format") format: String
    ): Call<List<GeoResponse>>
}

interface PincodeApiService {
    @GET("pincode/{pincode}")
    fun getPincodeData(@Path("pincode") pincode: String): Call<List<PincodeResponse>>

    companion object {
        fun create(): PincodeApiService {
            return Retrofit.Builder()
                .baseUrl("https://api.postalpincode.in/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PincodeApiService::class.java)
        }
    }
}

data class ChatMessage(
    val text: String,
    val isBot: Boolean,
    val isClickable: Boolean = false,
    val navigationTarget: String? = null
)

data class ChatRequest(
    val participants: List<String>,
    val lastMessage: String,
    val lastMessageSenderId: String,
    val lastMessageAt: String,
    val listingId: String
)

data class ChatBotRequest(
    val message: String,
    val userId: String
)
data class ProductDetails(
    val name: String,
    val description: String?,
    val origin: String?,
    val quality: String?,
    val sellerInfo: String?,
    val answer: String?,
    val additionalInfo: String?
)


data class ChatResponse(
    val navigationTarget: String?,
    val listings: List<Listing>,
    val answer: String?, // Used for Gemini API responses
    val productDetails: ProductDetails? // Added for product details

)

data class Chat(
    val id: String,
    val participants: List<String>,
    val listingId: String,
    val lastMessage: String,
    val lastMessagetime: String,
    val lastMessageAt: Long?,
    val lastMessageSenderId: String,
    val otherUserName: String,
    val otherUserType: String,
    val otherUserProfileImageUrl: String?
) : java.io.Serializable






