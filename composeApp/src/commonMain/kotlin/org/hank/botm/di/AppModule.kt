package org.hank.botm.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.di.feature.gameFeatureModule
import org.hank.botm.platformModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection.
 * This function sets up the Koin DI container with all the necessary modules.
 * 
 * This is an example of how the DI setup could be improved by organizing modules by feature.
 */
fun initImprovedKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            coreModule() + featureModules() + platformModule()
        )
    }
}

/**
 * Core module containing dependencies that are used across multiple features.
 */
private fun coreModule(): Module = module {
    // Dispatchers
    single(named("IoDispatcher")) { Dispatchers.IO }
    single(named("MainDispatcher")) { Dispatchers.Main }
}

/**
 * Combine all feature modules.
 */
private fun featureModules(): List<Module> = listOf(
    gameFeatureModule(),
    // Add other feature modules here
)