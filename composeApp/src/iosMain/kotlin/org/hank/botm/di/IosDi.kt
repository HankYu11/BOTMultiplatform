package org.hank.botm.di

import io.ktor.client.HttpClient
import org.hank.botm.createHttpClient
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.getAppDatabase
import org.hank.botm.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppDatabase> {
        val builder = getDatabaseBuilder()
        getAppDatabase(builder)
    }

    single<HttpClient> { createHttpClient() }
}