package hr.alg.iamu_project_bp.data.network.dto

import com.google.gson.annotations.SerializedName

data class GeocodingResponseDto(
    @SerializedName("results") val results: List<ResultDto>?,
) {
    data class ResultDto(
        @SerializedName("name") val name: String?,
        @SerializedName("latitude") val latitude: Double?,
        @SerializedName("longitude") val longitude: Double?,
        @SerializedName("country") val country: String?,
    )
}
