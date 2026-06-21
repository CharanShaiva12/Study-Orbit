package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEpiphanyDao {
    @Query("SELECT * FROM saved_epiphanies ORDER BY timestamp DESC")
    fun getAllSavedEpiphanies(): Flow<List<SavedEpiphany>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpiphany(savedEpiphany: SavedEpiphany)

    @Query("DELETE FROM saved_epiphanies WHERE id = :id")
    suspend fun deleteEpiphanyById(id: Int)
}
