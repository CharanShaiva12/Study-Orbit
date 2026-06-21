package com.example.data

import kotlinx.coroutines.flow.Flow

class EpiphanyRepository(private val dao: SavedEpiphanyDao) {
    val allSavedEpiphanies: Flow<List<SavedEpiphany>> = dao.getAllSavedEpiphanies()

    suspend fun insert(epiphany: SavedEpiphany) {
        dao.insertEpiphany(epiphany)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteEpiphanyById(id)
    }
}
