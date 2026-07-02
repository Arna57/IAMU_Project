package hr.alg.iamu_project_bp.domain.model

data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean,
) {
    companion object {
        const val NEW_ID: Long = 0L
    }
}
