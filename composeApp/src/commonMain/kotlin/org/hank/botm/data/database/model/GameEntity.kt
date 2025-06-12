package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Game

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey
    val id: Int = 0,
)

fun GameEntity.asDomain() = Game(id)
