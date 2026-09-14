package com.spendr.app.kt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_stats",
    primaryKeys = ["note", "paid_amount"],
    indices = [Index("paid_amount")],
)
data class NoteStatEntity(
    val note: String,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "use_count") val useCount: Long,
    @ColumnInfo(name = "latest_date") val latestDate: Long,
)
