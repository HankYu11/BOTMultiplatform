package org.hank.botm

import io.ktor.client.HttpClient
import org.hank.botm.data.database.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> {
        val builder = getDatabaseBuilder(context = get())
        getAppDatabase(builder)
    }

    single<HttpClient> { createHttpClient() }
}