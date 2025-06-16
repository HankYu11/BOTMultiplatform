package org.hank.botm.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.GameEntity
import org.hank.botm.data.database.model.PlayerEntity
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity

@Database(entities = [PlayerEntity::class, ResultEntity::class, RoundEntity::class, GameEntity::class], version = 1)
@ConstructedBy(AppDatabaseFactory::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getPlayerDao(): PlayerDao
    abstract fun getRoundDao(): RoundDao
    abstract fun getResultDao(): ResultDao
    abstract fun getGameDao(): GameDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseFactory : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}