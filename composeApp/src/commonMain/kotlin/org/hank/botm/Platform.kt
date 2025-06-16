package org.hank.botm

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.hank.botm.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun getAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}