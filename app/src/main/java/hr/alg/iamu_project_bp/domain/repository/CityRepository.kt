package hr.alg.iamu_project_bp.domain.repository

import hr.alg.iamu_project_bp.domain.model.City

interface CityRepository {
    fun interface OnCitiesChangedListener {
        fun onCitiesChanged()
    }

    suspend fun insert(city: City): Long

    suspend fun getAll(): List<City>

    suspend fun getById(id: Long): City?

    suspend fun update(city: City): Boolean

    suspend fun delete(id: Long): Boolean

    fun addOnCitiesChangedListener(listener: OnCitiesChangedListener)

    fun removeOnCitiesChangedListener(listener: OnCitiesChangedListener)
}
