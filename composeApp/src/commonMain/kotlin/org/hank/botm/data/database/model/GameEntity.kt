package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Game

@Entity(
    tableName = "game",
    indices = [Index(value = ["serverId"])]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // Sync tracking fields
    val serverId: Int? = null,
    val isDirty: Boolean = false,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = 0L,
)

fun GameEntity.asDomain() = Game(id)
