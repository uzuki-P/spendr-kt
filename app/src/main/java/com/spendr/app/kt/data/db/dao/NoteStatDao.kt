package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendr.app.kt.data.db.entity.NoteStatEntity

@Dao
interface NoteStatDao {

    @Query("SELECT * FROM note_stats ORDER BY use_count DESC, latest_date DESC")
    suspend fun list(): List<NoteStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(noteStat: NoteStatEntity)
}
