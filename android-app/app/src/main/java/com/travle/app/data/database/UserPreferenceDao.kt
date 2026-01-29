package com.travle.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destination: String,
    val preferences: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences ORDER BY timestamp DESC")
    fun getAllPreferences(): Flow<List<UserPreference>>

    @Query("SELECT * FROM user_preferences WHERE destination LIKE :destination OR preferences LIKE :preferences ORDER BY timestamp DESC LIMIT :limit")
    fun getPreferencesByQuery(destination: String, preferences: String, limit: Int): Flow<List<UserPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(userPreference: UserPreference)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(userPreferences: List<UserPreference>)

    @Delete
    suspend fun deletePreference(userPreference: UserPreference)

    @Query("DELETE FROM user_preferences")
    suspend fun clearAllPreferences()
}