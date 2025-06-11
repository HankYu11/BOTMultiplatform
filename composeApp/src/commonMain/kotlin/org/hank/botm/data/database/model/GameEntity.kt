package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Game

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isFinished: Boolean = false,
)

fun GameEntity.asDomain() = Game(id)
