package com.dozy.mobile.feature.story.data.database

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "stories",
    indices = [Index(value = ["jobId"]), Index(value = ["requestKey"], unique = true)],
)
data class StoryEntity(
    @PrimaryKey val id: String,
    val jobId: String? = null,
    val requestKey: String? = null,
    val result: String? = null,
    val summary: String? = null,
    val notificationDelivered: Boolean = false,
    val createdOrder: Long = 0,
)
