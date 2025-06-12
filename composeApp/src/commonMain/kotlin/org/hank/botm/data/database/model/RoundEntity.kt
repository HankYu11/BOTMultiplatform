package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Round

@Entity(
    tableName = "round",
    foreignKeys = [ForeignKey(
        entity = GameEntity::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bet: Int = 1,
    val gameId: Int,
)

fun RoundEntity.asDomain() = Round(id, bet, gameId)