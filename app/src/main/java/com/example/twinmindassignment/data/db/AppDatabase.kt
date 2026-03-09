package com.example.twinmindassignment.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AudioChunk::class, Recording::class], version = 1, exportSchema = false)
@TypeConverters(ChunkStatusConverter::class, SummaryStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun recordingDao(): RecordingDao
}
