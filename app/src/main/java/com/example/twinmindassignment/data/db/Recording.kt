package com.example.twinmindassignment.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class SummaryStatus { NONE, PENDING, GENERATING, DONE, ERROR }

class SummaryStatusConverter {
    @TypeConverter fun fromStatus(value: SummaryStatus): String = value.name
    @TypeConverter fun toStatus(value: String): SummaryStatus = SummaryStatus.valueOf(value)
}

@Entity(tableName = "recordings")
@TypeConverters(SummaryStatusConverter::class)
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val summary: String? = null,
    val summaryStatus: SummaryStatus = SummaryStatus.NONE,
    val summaryError: String? = null
)
