package org.hank.botm.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hank.botm.Config
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.sync.ConnectionState
import org.hank.botm.data.sync.SyncManager
import org.hank.botm.data.sync.SyncState
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.GameWithDetails
import org.hank.botm.ui.GameRoom
import org.hank.botm.ui.Lobby
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake GameRepository
    private class FakeGameRepository(
        private val existingGame: Game? = null
    ) : GameRepository {
        override val game: Flow<Game?> = flowOf(existingGame)
        override val gameWithDetails: Flow<GameWithDetails>
            get() = throw NotImplementedError("Not needed for this test")

        override suspend fun createGame(createGame: CreateGame): Result<Unit> = Result.success(Unit)
        override suspend fun clearGame() {}
        override suspend fun checkGameExists(gameId: Int): Result<Unit> = Result.success(Unit)
        override fun subscribeToGameUpdates(gameId: Int) {}
        override fun disconnectFromGameUpdates() {}
    }

    // Fake SyncManager
    private class FakeSyncManager : SyncManager {
        private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        override val syncState: StateFlow<SyncState> = _syncState

        private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
        override val connectionState: StateFlow<ConnectionState> = _connectionState

        var performSyncCalled = false
        var syncResult: Result<Unit> = Result.success(Unit)

        override fun canUseOnline(): Boolean = true

        override suspend fun performSync(): Result<Unit> {
            performSyncCalled = true
            return syncResult
        }

        override suspend fun markGameDirty(gameId: Int) {}
        override suspend fun markRoundDirty(roundId: Int) {}
    }

    private var originalIsEligibleForOnline: Boolean = true

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Save original value
        originalIsEligibleForOnline = Config.isEligibleForOnline
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        // Restore original value
        Config.isEligibleForOnline = originalIsEligibleForOnline
    }

    // ==================== NAVIGATION TESTS ====================

    @Test
    fun `navigates to Lobby when no game exists`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false // Skip sync

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance past minimum splash time (1 second)
        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<Lobby>(viewModel.startDestination.value)
    }

    @Test
    fun `navigates to GameRoom when game exists`() = runTest(testDispatcher) {
        val existingGame = Game(id = 42)
        val fakeRepository = FakeGameRepository(existingGame = existingGame)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false // Skip sync

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance past minimum splash time
        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        val destination = viewModel.startDestination.value
        assertIs<GameRoom>(destination)
        assertEquals(42, destination.gameId)
    }

    @Test
    fun `startDestination is initially null`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Don't advance time - check initial state
        assertNull(viewModel.startDestination.value)
    }

    // ==================== SYNC TESTS ====================

    @Test
    fun `performs sync when eligible for online`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = true

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance to complete initialization
        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSyncManager.performSyncCalled)
    }

    @Test
    fun `skips sync when not eligible for online`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance to complete initialization
        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!fakeSyncManager.performSyncCalled)
    }

    @Test
    fun `navigates even when sync fails`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        fakeSyncManager.syncResult = Result.failure(Exception("Sync failed"))
        Config.isEligibleForOnline = true

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance past sync timeout and splash time
        advanceTimeBy(6000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should still navigate despite sync failure
        assertIs<Lobby>(viewModel.startDestination.value)
    }

    // ==================== MINIMUM SPLASH TIME TESTS ====================

    @Test
    fun `eventually navigates after splash time`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository(existingGame = null)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        // Advance past minimum splash time (1 second) plus buffer
        // Note: TimeSource.Monotonic uses real time in tests, so we just
        // advance the coroutine scheduler and verify eventual navigation
        advanceTimeBy(2000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should have navigated
        assertIs<Lobby>(viewModel.startDestination.value)
    }

    // ==================== GAME ID PRESERVATION TESTS ====================

    @Test
    fun `preserves game ID in GameRoom destination`() = runTest(testDispatcher) {
        val existingGame = Game(id = 12345)
        val fakeRepository = FakeGameRepository(existingGame = existingGame)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        val destination = viewModel.startDestination.value
        assertIs<GameRoom>(destination)
        assertEquals(12345, destination.gameId)
    }

    @Test
    fun `handles negative game IDs - offline games`() = runTest(testDispatcher) {
        val offlineGame = Game(id = -5) // Offline-generated negative ID
        val fakeRepository = FakeGameRepository(existingGame = offlineGame)
        val fakeSyncManager = FakeSyncManager()
        Config.isEligibleForOnline = false

        val viewModel = SplashViewModel(fakeRepository, fakeSyncManager)

        advanceTimeBy(1500)
        testDispatcher.scheduler.advanceUntilIdle()

        val destination = viewModel.startDestination.value
        assertIs<GameRoom>(destination)
        assertEquals(-5, destination.gameId)
    }
}
