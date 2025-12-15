package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Game

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val isDirty: Boolean = false,
    val lastSyncedAt: Long? = null,
    val createdAt: Long = 0L,
)

fun GameEntity.asDomain() = Game(id)
