package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Round

@Entity(
    tableName = "round",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["gameId"]), Index(value = ["serverId"])]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bet: Int = 1,
    val gameId: Int,
    // Sync tracking fields
    val serverId: Int? = null,
    val isDirty: Boolean = false,
)

fun RoundEntity.asDomain() = Round(id, bet, gameId)