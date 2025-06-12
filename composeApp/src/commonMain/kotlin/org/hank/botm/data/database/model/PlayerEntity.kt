package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Player

@Entity(
    tableName = "player",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class PlayerEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val balance: Int,
    val gameId: Int,
)

fun PlayerEntity.asDomain() = Player(
    id = id,
    name = name,
    balance = balance,
    gameId = gameId,
)
