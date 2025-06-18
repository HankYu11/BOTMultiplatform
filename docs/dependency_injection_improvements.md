# Dependency Injection Improvements

## Current Issues

After analyzing the current dependency injection implementation in the project, I've identified several issues:

1. **Module Organization by Layer Instead of Feature**:
   - The current DI setup organizes modules by layer (daoModule, repositoryModule, viewModelModule, etc.) rather than by feature.
   - This makes it harder to understand which components belong to which feature and can lead to unnecessary dependencies between features.

2. **Excessive Dependencies in Repository Implementations**:
   - `GameRepositoryImpl` has 7 constructor parameters
   - `RoundRepositoryImpl` has 6 constructor parameters
   - This makes these classes harder to test and maintain.

3. **Redundant Dependencies**:
   - Both repositories inject the `AppDatabase` and several DAOs, even though the DAOs can be accessed through the `AppDatabase`.
   - This creates unnecessary coupling and makes the code more verbose.

4. **Unclear Dependency Resolution**:
   - Extensive use of `get()` without specifying what's being injected makes the code harder to understand.
   - For example: `single<GameDao> { get<AppDatabase>().getGameDao() }`

5. **Business Logic in ViewModels**:
   - Business logic like `convertToResult` in `HomeViewModel` should be in a separate use case or domain service.
   - This would improve testability and separation of concerns.

6. **No Proper Scoping**:
   - All dependencies are defined as singletons (`single`), without considering their lifecycle.
   - Some dependencies might be better scoped to a specific feature or screen.

7. **No Clear Separation of Concerns**:
   - The current structure doesn't clearly separate infrastructure, domain, and presentation concerns.

## Best Practices for Koin Dependency Injection

1. **Organize Modules by Feature**:
   - Group related components (repositories, ViewModels, use cases) by feature.
   - This improves maintainability and makes it easier to understand the codebase.

2. **Use Factory Functions for Dependencies**:
   - Create factory functions for dependencies that need complex initialization.
   - This reduces the number of constructor parameters and improves readability.

3. **Apply Proper Scoping**:
   - Use appropriate scopes for dependencies based on their lifecycle.
   - For example, use `viewModel` scope for ViewModels and `single` for repositories.

4. **Implement Use Cases/Interactors**:
   - Extract business logic from ViewModels into use cases.
   - This improves testability and separation of concerns.

5. **Use Qualifiers When Necessary**:
   - Use qualifiers to distinguish between similar dependencies.
   - For example, `named("IoDispatcher")` for different types of dispatchers.

6. **Reduce Constructor Parameters**:
   - Group related dependencies into factory classes or providers.
   - This reduces the number of constructor parameters and improves maintainability.

7. **Make Dependencies Explicit**:
   - Avoid using `get()` without specifying what's being injected.
   - This improves code readability and makes dependencies explicit.

## Step-by-Step Implementation Guide

### 1. Create Feature Modules

Reorganize the DI setup to use feature modules instead of layer modules:

```kotlin
// Feature-based modules
fun commonModule(): Module = module {
    includes(
        coreModule(),
        gameFeatureModule(),
        roundFeatureModule(),
        // Other feature modules
    )
}

private fun coreModule(): Module = module {
    // Core dependencies like database, network client, dispatchers
    single { createAppDatabase() }
    single { createHttpClient() }
    single(named("IoDispatcher")) { Dispatchers.IO }
    single(named("MainDispatcher")) { Dispatchers.Main }
}

private fun gameFeatureModule(): Module = module {
    // Game feature dependencies
    single { get<AppDatabase>().getGameDao() }
    single { get<AppDatabase>().getPlayerDao() }
    single { GameApiImpl(get()) }
    single<GameRepository> { GameRepositoryImpl(get(), get(), get(), get()) }
    factory { GetGameUseCase(get()) }
    factory { CreateGameUseCase(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
}

private fun roundFeatureModule(): Module = module {
    // Round feature dependencies
    single { get<AppDatabase>().getRoundDao() }
    single { get<AppDatabase>().getResultDao() }
    single { RoundApi(get()) }
    single<RoundRepository> { RoundRepositoryImpl(get(), get(), get(), get()) }
    factory { CreateRoundUseCase(get()) }
    // Other round-related dependencies
}
```

### 2. Reduce Constructor Parameters in Repositories

Create factory classes or providers to reduce the number of constructor parameters:

```kotlin
// Database provider
class DatabaseProvider(private val appDatabase: AppDatabase) {
    val gameDao: GameDao = appDatabase.getGameDao()
    val playerDao: PlayerDao = appDatabase.getPlayerDao()
    val roundDao: RoundDao = appDatabase.getRoundDao()
    val resultDao: ResultDao = appDatabase.getResultDao()
}

// Updated repository implementation
class GameRepositoryImpl(
    private val databaseProvider: DatabaseProvider,
    private val gameApi: GameApi,
    private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {
    // Implementation using databaseProvider.gameDao, etc.
}
```

### 3. Extract Business Logic to Use Cases

Create use cases for business logic currently in ViewModels:

```kotlin
class CreateRoundUseCase(private val roundRepository: RoundRepository) {
    suspend operator fun invoke(gameId: Int, bet: Int, playerResults: List<PlayerResult>): Result<Unit> {
        val results = convertToResult(playerResults, bet)
        return roundRepository.insertRound(gameId, bet, results)
    }

    private fun convertToResult(playerResults: List<PlayerResult>, bet: Int): List<Result> {
        // Logic moved from HomeViewModel
    }
}
```

### 4. Update ViewModels to Use Use Cases

Refactor ViewModels to use the new use cases:

```kotlin
class HomeViewModel(
    savedStateHandle: SavedStateHandle,
    private val getGameUseCase: GetGameUseCase,
    private val createRoundUseCase: CreateRoundUseCase,
    private val startNewGameUseCase: StartNewGameUseCase,
) : ViewModel() {
    private val gameId = savedStateHandle.toRoute<Game>().gameId
    
    val gameWithDetails = getGameUseCase(gameId)
    
    // Other properties
    
    fun submitResults(playerResults: List<PlayerResult>) {
        viewModelScope.launch {
            createRoundUseCase(gameId, bet.value, playerResults).let { result ->
                // Handle result
            }
        }
    }
    
    fun startNewGame() {
        viewModelScope.launch {
            startNewGameUseCase()
            _shouldNavToSetup.value = true
        }
    }
    
    // Other methods
}
```

### 5. Implement Proper Error Handling

Add proper error handling in repositories and use cases:

```kotlin
class GameRepositoryImpl(
    private val databaseProvider: DatabaseProvider,
    private val gameApi: GameApi,
    private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {
    override suspend fun refreshGame(id: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            val gameDetails = gameApi.getGame(id)
            // Process data
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(NetworkError("Network error: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(UnknownError("Unknown error: ${e.message}", e))
        }
    }
}
```

### 6. Make Dependencies Explicit

Update the DI setup to make dependencies explicit:

```kotlin
private fun gameFeatureModule(): Module = module {
    single<GameDao> { get<AppDatabase>().getGameDao() }
    single<PlayerDao> { get<AppDatabase>().getPlayerDao() }
    single<DatabaseProvider> { DatabaseProvider(get()) }
    single<GameApi> { GameApiImpl(httpClient = get()) }
    single<GameRepository> { 
        GameRepositoryImpl(
            databaseProvider = get(),
            gameApi = get(),
            ioDispatcher = get(named("IoDispatcher"))
        ) 
    }
    // Other dependencies
}
```

## Conclusion

Implementing these improvements will make the codebase more maintainable, testable, and easier to understand. The key benefits include:

1. Better organization of code by feature
2. Reduced coupling between components
3. Improved testability through proper dependency injection
4. Clearer separation of concerns
5. More explicit dependencies
6. Proper error handling

These changes align with the first item in your improvement tasks checklist: "Implement a proper dependency injection pattern with Koin modules organized by feature."