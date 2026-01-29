package com.travle.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey val id: String,
    val destination: String,
    val preferences: String,
    val guide: String,
    val images: String, // JSON字符串存储
    val collectedAt: Long = System.currentTimeMillis()
)