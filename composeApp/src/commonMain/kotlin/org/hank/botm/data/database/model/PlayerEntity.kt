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
    indices = [Index(value = ["gameId"]), Index(value = ["serverId"])]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val balance: Int,
    val gameId: Int,
    // Sync tracking fields
    val serverId: Int? = null,
    val isDirty: Boolean = false,
)

fun PlayerEntity.asDomain() = Player(
    id = id,
    name = name,
    balance = balance,
    gameId = gameId,
)
