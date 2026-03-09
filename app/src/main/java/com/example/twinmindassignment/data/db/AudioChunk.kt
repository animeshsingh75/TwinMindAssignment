package com.example.twinmindassignment.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.twinmindassignment.model.ChunkStatus

class ChunkStatusConverter {
    @TypeConverter fun fromStatus(value: ChunkStatus): String = value.name
    @TypeConverter fun toStatus(value: String): ChunkStatus = ChunkStatus.valueOf(value)
}

@Entity(
    tableName = "audio_chunks",
    indices = [
        Index(value = ["recordingId", "sequenceNumber"], unique = true),
        Index("recordingId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Recording::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(ChunkStatusConverter::class)
data class AudioChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recordingId") val recordingId: Long,
    @ColumnInfo(name = "sequenceNumber") val sequenceNumber: Int,
    val filePath: String,
    val status: ChunkStatus = ChunkStatus.PENDING,
    val transcript: String? = null,
    val errorMessage: String? = null
)
