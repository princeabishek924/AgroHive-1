package com.example.agrohive_1

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class Iso8601Deserializer : JsonDeserializer<Long> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Long {
        val jsonString = json?.asString ?: throw IllegalArgumentException("Timestamp is null")
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(jsonString)?.time ?: throw IllegalArgumentException("Invalid timestamp: $jsonString")
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing timestamp: $jsonString, ${e.message}")
        }
    }
}