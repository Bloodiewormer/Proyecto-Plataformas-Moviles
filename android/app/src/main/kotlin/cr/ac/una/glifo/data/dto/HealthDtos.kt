package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

data class HealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("service") val service: String,
    @SerializedName("timestamp") val timestamp: String
)
