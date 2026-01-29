package com.travle.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_items ORDER BY collectedAt DESC")
    fun getAllCollections(): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(item: CollectionItem)

    @Delete
    suspend fun deleteCollection(item: CollectionItem)

    @Query("DELETE FROM collection_items")
    suspend fun clearAllCollections()
}