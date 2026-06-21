package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_epiphanies")
data class SavedEpiphany(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val prompt: String,
    val coreSpark: String,
    val analogy: String,
    val cheatcardItems: String, // Stored as a semple semicolon-separated or line-separated list of items
    val timestamp: Long = System.currentTimeMillis()
)
