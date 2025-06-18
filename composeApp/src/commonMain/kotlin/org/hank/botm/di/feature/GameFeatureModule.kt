package org.hank.botm.di.feature

import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.DatabaseProvider
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.api.GameApiImpl
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.repository.GameRepositoryImpl
import org.hank.botm.domain.usecase.CreateRoundUseCase
import org.hank.botm.ui.viewmodel.HomeViewModel
import org.hank.botm.ui.viewmodel.SetupViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Game feature.
 * This module provides all dependencies related to the Game feature.
 * 
 * This is an example of how to organize dependencies by feature rather than by layer.
 */
fun gameFeatureModule(): Module = module {
    // Database
    single { DatabaseProvider(get()) }
    
    // DAOs - these could be provided by the DatabaseProvider instead
    single { get<AppDatabase>().getGameDao() }
    single { get<AppDatabase>().getPlayerDao() }
    single { get<AppDatabase>().getRoundDao() }
    single { get<AppDatabase>().getResultDao() }
    
    // API
    single<GameApi> { GameApiImpl(get()) }
    
    // Repository
    single<GameRepository> {
        GameRepositoryImpl(
            appDatabase = get(),
            gameDao = get(),
            playerDao = get(),
            roundDao = get(),
            resultDao = get(),
            gameApi = get(),
            ioDispatcher = get(named("IoDispatcher"))
        )
    }
    
    // Use Cases
    factory {
        CreateRoundUseCase(
            roundRepository = get(),
            ioDispatcher = get(named("IoDispatcher"))
        )
    }
    
    // ViewModels
    factory { (savedStateHandle: androidx.lifecycle.SavedStateHandle) ->
        HomeViewModel(
            savedStateHandle = savedStateHandle,
            gameRepository = get(),
            roundRepository = get()
        )
    }
    
    factory { SetupViewModel(get()) }
}

/**
 * This is an improved version of the Game feature module that uses the DatabaseProvider
 * to reduce the number of dependencies injected into the repository.
 * 
 * Note: This is just an example and would require changes to the GameRepositoryImpl class.
 */
fun improvedGameFeatureModule(): Module = module {
    // Database
    single { DatabaseProvider(get()) }
    
    // API
    single<GameApi> { GameApiImpl(get()) }
    
    // Repository - This would require changes to GameRepositoryImpl
    // single<GameRepository> {
    //     GameRepositoryImpl(
    //         databaseProvider = get(),
    //         gameApi = get(),
    //         ioDispatcher = get(named("IoDispatcher"))
    //     )
    // }
    
    // Use Cases
    factory {
        CreateRoundUseCase(
            roundRepository = get(),
            ioDispatcher = get(named("IoDispatcher"))
        )
    }
    
    // ViewModels
    factory { (savedStateHandle: androidx.lifecycle.SavedStateHandle) ->
        HomeViewModel(
            savedStateHandle = savedStateHandle,
            gameRepository = get(),
            roundRepository = get()
        )
    }
    
    factory { SetupViewModel(get()) }
}