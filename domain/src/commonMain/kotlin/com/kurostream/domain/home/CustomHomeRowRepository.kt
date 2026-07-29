package com.kurostream.domain.home

import com.kurostream.domain.result.Result

interface CustomHomeRowRepository {
    suspend fun getRows(): List<CustomHomeRow>
    suspend fun saveRows(rows: List<CustomHomeRow>)
    suspend fun createRow(row: CustomHomeRow): Result<Unit>
    suspend fun updateRow(row: CustomHomeRow): Result<Unit>
    suspend fun deleteRow(rowId: String): Result<Unit>
    suspend fun reorderRows(rowIds: List<String>): Result<Unit>
    suspend fun generatePreview(row: CustomHomeRow): Result<RowPreview>
}
