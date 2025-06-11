package org.hank.botm

import com.example.bigoldtwo.data.database.AppDatabase
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.repository.GameRepositoryImpl
import org.hank.botm.data.repository.PlayerRepository
import org.hank.botm.data.repository.PlayerRepositoryImpl
import org.hank.botm.data.repository.ResultRepository
import org.hank.botm.data.repository.ResultRepositoryImpl
import org.hank.botm.data.repository.RoundRepository
import org.hank.botm.data.repository.RoundRepositoryImpl
import org.hank.botm.domain.usecase.GetGameResultDataUseCase
import org.hank.botm.domain.usecase.GetRoundResultUseCase
import org.hank.botm.domain.usecase.InsertResultsUseCase
import org.hank.botm.ui.viewmodel.HomeViewModel
import org.hank.botm.ui.viewmodel.SetupViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            commonModule() + platformModule()
        )
    }
}

fun initKoinIos() = initKoin(appDeclaration = {})

fun commonModule(): Module = module {
    includes(daoModule(), repositoryModule(), dispatcherModule(), usecaseModule(), viewModelModule())
}

private fun daoModule(): Module = module {
    single<GameDao> { get<AppDatabase>().getGameDao() }
    single<PlayerDao> { get<AppDatabase>().getPlayerDao() }
    single<ResultDao> { get<AppDatabase>().getResultDao() }
    single<RoundDao> { get<AppDatabase>().getRoundDao() }
}

private fun repositoryModule(): Module = module {
    single<GameRepository> { GameRepositoryImpl(get(), get(named("IoDispatcher"))) }
    single<PlayerRepository> { PlayerRepositoryImpl(get(), get(named("IoDispatcher"))) }
    single<ResultRepository> { ResultRepositoryImpl(get(), get(named("IoDispatcher"))) }
    single<RoundRepository> { RoundRepositoryImpl(get(), get(), get(), get(named("IoDispatcher"))) }
}

private fun usecaseModule(): Module = module {
    factoryOf(::GetGameResultDataUseCase)
    factoryOf(::GetRoundResultUseCase)
    factoryOf(::InsertResultsUseCase)
}

private fun viewModelModule(): Module = module {
    viewModel { AppViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { SetupViewModel(get(), get()) }
}

private fun dispatcherModule(): Module = module {
    single(named("IoDispatcher")) { Dispatchers.IO }
    single(named("MainDispatcher")) { Dispatchers.Main }
}

expect fun platformModule(): Module
expect val client: HttpClient