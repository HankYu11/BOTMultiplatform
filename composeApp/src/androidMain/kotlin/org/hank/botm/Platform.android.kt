package org.hank.botm

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.hank.botm.data.database.ALL_MIGRATIONS
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.network.applyCommonConfiguration
import java.util.concurrent.atomic.AtomicInteger

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual object OfflineIdGenerator {
    private val idCounter = AtomicInteger(0)

    actual fun nextId(): Int = idCounter.decrementAndGet()

    actual fun isOfflineId(id: Int): Boolean = id < 0

    actual fun reset() {
        idCounter.set(0)
    }
}

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app_database")

    return Room.databaseBuilder<AppDatabase>(appContext, dbFile.path)
        .addMigrations(*ALL_MIGRATIONS)
}

fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        applyCommonConfiguration()
    }
}
