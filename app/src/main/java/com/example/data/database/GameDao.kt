package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DungeonSave
import com.example.data.model.ScoreHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM dungeon_save WHERE id = 1")
    suspend fun getSave(): DungeonSave?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(save: DungeonSave)

    @Query("DELETE FROM dungeon_save WHERE id = 1")
    suspend fun deleteSave()

    @Query("SELECT * FROM score_history ORDER BY score DESC")
    fun getScores(): Flow<List<ScoreHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ScoreHistory)

    @Query("DELETE FROM score_history")
    suspend fun clearHistory()
}
