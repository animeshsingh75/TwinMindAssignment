package com.example.twinmindassignment.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.example.twinmindassignment.data.db.AppDatabase
import com.example.twinmindassignment.data.db.AudioChunkDao
import com.example.twinmindassignment.data.db.RecordingDao
import com.example.twinmindassignment.data.network.ApiService
import com.example.twinmindassignment.data.network.Client
import com.example.twinmindassignment.data.repo.SummaryRepo
import com.example.twinmindassignment.data.repo.TranscriptRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "twinmind_db")
            .build()

    @Provides
    fun provideAudioChunkDao(db: AppDatabase): AudioChunkDao = db.audioChunkDao()

    @Provides
    fun provideRecordingDao(db: AppDatabase): RecordingDao = db.recordingDao()

    @Provides
    @Singleton
    fun provideApiService(): ApiService = Client.api

    @Provides
    @Singleton
    fun provideTranscriptRepo(
        apiService: ApiService,
        audioChunkDao: AudioChunkDao
    ): TranscriptRepo = TranscriptRepo(apiService, audioChunkDao)

    @Provides
    @Singleton
    fun provideSummaryRepo(
        apiService: ApiService,
        recordingDao: RecordingDao,
        audioChunkDao: AudioChunkDao
    ): SummaryRepo = SummaryRepo(apiService, recordingDao, audioChunkDao)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
