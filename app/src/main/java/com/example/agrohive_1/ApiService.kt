package com.example.agrohive_1

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/signup")
    fun saveUserData(@Body userData: UserData): Call<UserData>

    @POST("auth/upload-image")
    fun uploadImage(@Body imageData: Map<String, String>): Call<Map<String, String>>

    @GET("auth/user")
    fun getUser(@Query("firebaseUid") firebaseUid: String): Call<UserResponse>

    @PUT("auth/user")
    fun updateUser(
        @Query("firebaseUid") firebaseUid: String,
        @Body userData: UserResponse
    ): Call<UserResponse>

    @GET("auth/listings")
    fun getListings(
        @Query("query") query: String,
        @Query("location") location: String,
        @Query("radius") radius: String,
        @Query("firebaseUid") firebaseUid: String,
        @Query("priceFrom") priceFrom: Double?,
        @Query("priceTo") priceTo: Double?,
        @Query("category") category: String?
    ): Call<List<Listing>>

    @POST("auth/listings")
    fun createListing(@Body listing: Listing): Call<Listing>

    @GET("auth/user-listings")
    fun getUserListings(@Query("userId") userId: String): Call<List<Listing>>

    @GET("auth/product-details")
    fun getProductDetails(
        @Query("userId") userId: String,
        @Query("imageUrl") imageUrl: String
    ): Call<ProductDetailResponse>

    @GET("auth/cities")
    fun getAllCities(): Call<List<CityPincode>>

    @GET("auth/check-username")
    fun checkUsernameAvailability(@Query("username") username: String): Call<Map<String, Boolean>>

    @PUT("auth/update-username")
    fun updateUsername(@Body updateData: Map<String, String>): Call<UserResponse>

    @GET("auth/email-by-username")
    fun getEmailByUsername(@Query("username") username: String): Call<Map<String, String>>

    @GET("auth/user-by-email")
    fun getUserByEmail(@Query("email") email: String): Call<UserResponse>

    @POST("auth/send-password-reset")
    fun sendPasswordReset(
        @Query("email") email: String,
        @Query("displayName") displayName: String
    ): Call<Void>

    @PUT("auth/update-role")
    fun updateRole(@Body updateData: Map<String, String>): Call<UserResponse>

    @DELETE("auth/delete-role-data")
    fun deleteRoleData(
        @Query("firebaseUid") firebaseUid: String,
        @Query("userType") userType: String
    ): Call<Void>

    @GET("auth/dashboard")
    fun getDashboardData(@Query("firebaseUid") firebaseUid: String): Call<DashboardData>

    @GET("auth/orders/farmer")
    fun getOrdersForFarmer(@Query("farmerId") farmerId: String): Call<List<Order>>

    @POST("auth/orders/{orderId}/accept")
    fun acceptOrder(@Path("orderId") orderId: String): Call<OrderResponse>

    @POST("auth/orders/{orderId}/cancel")
    fun cancelOrder(@Path("orderId") orderId: String): Call<OrderResponse>

    @PUT("auth/listings/{listingId}/quantity")
    fun updateListingQuantity(
        @Path("listingId") listingId: String,
        @Body quantity: Int
    ): Call<OrderResponse>

    @DELETE("auth/listings/{listingId}")
    fun deleteListing(@Path("listingId") listingId: String): Call<OrderResponse>

    @POST("auth/notifications/send")
    fun sendNotification(
        @Query("userId") userId: String,
        @Query("message") message: String
    ): Call<NotificationResponse>

    @GET("auth/notifications")
    fun getNotifications(@Query("userId") userId: String): Call<List<Notification>>

    @GET("auth/listing-quantity")
    fun getListingQuantity(@Query("listingId") listingId: String): Call<OrderResponse>

    @POST("auth/place-order")
    fun placeOrder(@Body order: Order): Call<OrderResponse>

    @GET("chats")
    fun getChats(@Query("userId") userId: String): Call<List<Chat>>

    @POST("chats")
    fun createChat(@Body chatRequest: ChatRequest): Call<Chat>

    @POST("messages")
    fun sendMessage(@Body message: MessageRequest): Call<Message>

    @GET("messages/{chatId}")
    fun getMessages(@Path("chatId") chatId: String): Call<List<Message>>

    @PUT("messages/{messageId}/status")
    fun updateMessageStatus(
        @Path("messageId") messageId: String,
        @Body status: Map<String, String>
    ): Call<Message>

    @POST("auth/chatbot")
    fun sendChatBotMessage(@Body request: ChatBotRequest): Call<ChatResponse>
}