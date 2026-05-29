package com.example.data.repository

import com.example.data.database.GameDao
import com.example.data.model.DungeonSave
import com.example.data.model.ScoreHistory
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allScores: Flow<List<ScoreHistory>> = gameDao.getScores()

    suspend fun getSave(): DungeonSave? {
        return gameDao.getSave()
    }

    suspend fun saveGame(save: DungeonSave) {
        gameDao.saveGame(save)
    }

    suspend fun deleteSave() {
        gameDao.deleteSave()
    }

    suspend fun insertScore(score: ScoreHistory) {
        gameDao.insertScore(score)
    }

    suspend fun clearHistory() {
        gameDao.clearHistory()
    }
}
