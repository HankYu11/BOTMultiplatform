package org.hank.botm

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.network.applyCommonConfiguration

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app_database")

    return Room.databaseBuilder<AppDatabase>(appContext, dbFile.path)
}

fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        applyCommonConfiguration()
    }
}
