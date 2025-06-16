package org.hank.botm

import androidx.room.Room
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.hank.botm.data.database.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.hank.botm.data.network.applyCommonConfiguration
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/app_database.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager().URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )

    return requireNotNull(documentDirectory?.path())
}

fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        applyCommonConfiguration()
    }
}