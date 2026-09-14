package com.spendr.app.kt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendr.app.kt.data.db.entity.MerchantEntity

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchants ORDER BY name LIMIT 20")
    suspend fun list(): List<MerchantEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(merchant: MerchantEntity): Long

    @Query("UPDATE merchants SET updated_at = :updatedAt WHERE name = :name")
    suspend fun touch(name: String, updatedAt: Long): Int
}
