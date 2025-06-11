package org.hank.botm

import com.example.bigoldtwo.data.database.AppDatabase
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> {
        val builder = getDatabaseBuilder()
        getAppDatabase(builder)
    }
}

actual val client: HttpClient
    get() = HttpClient() {
    }