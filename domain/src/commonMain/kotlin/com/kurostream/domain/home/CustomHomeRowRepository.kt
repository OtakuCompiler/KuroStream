package com.kurostream.domain.home

interface CustomHomeRowRepository {
    suspend fun getRows(): List<CustomHomeRow>
    suspend fun saveRows(rows: List<CustomHomeRow>)
}
