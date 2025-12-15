package org.hank.botm.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.hank.botm.Config
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.GameEntity
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.sync.ConnectionState
import org.hank.botm.data.sync.SyncManager
import org.hank.botm.data.sync.SyncState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for GameRepositoryImpl using MockK.
 * Focuses on checkGameExists() which has clear, testable behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    // Mocks
    private lateinit var appDatabase: AppDatabase
    private lateinit var gameDao: GameDao
    private lateinit var playerDao: PlayerDao
    private lateinit var roundDao: RoundDao
    private lateinit var resultDao: ResultDao
    private lateinit var gameApi: GameApi
    private lateinit var syncManager: SyncManager

    private lateinit var repository: GameRepositoryImpl

    private var originalIsEligibleForOnline: Boolean = true

    @BeforeTest
    fun setup() {
        testScope = TestScope(testDispatcher)

        // Create mocks
        appDatabase = mockk(relaxed = true)
        gameDao = mockk(relaxed = true)
        playerDao = mockk(relaxed = true)
        roundDao = mockk(relaxed = true)
        resultDao = mockk(relaxed = true)
        gameApi = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)

        // Default mock behaviors
        every { gameDao.getNewestGame() } returns flowOf(null)
        every { gameDao.getNewestGameWithDetails() } returns flowOf(null)
        every { syncManager.syncState } returns MutableStateFlow(SyncState.Idle)
        every { syncManager.connectionState } returns MutableStateFlow(ConnectionState.Connected)

        // Save original config
        originalIsEligibleForOnline = Config.isEligibleForOnline

        repository = GameRepositoryImpl(
            appDatabase = appDatabase,
            gameDao = gameDao,
            playerDao = playerDao,
            roundDao = roundDao,
            resultDao = resultDao,
            gameApi = gameApi,
            syncManager = syncManager,
            ioDispatcher = testDispatcher,
            applicationScope = testScope.backgroundScope
        )
    }

    @AfterTest
    fun tearDown() {
        Config.isEligibleForOnline = originalIsEligibleForOnline
    }

    // ==================== checkGameExists() TESTS ====================

    @Test
    fun `checkGameExists returns success when server confirms game exists`() = runTest(testDispatcher) {
        // Given: Online mode enabled and connected
        Config.isEligibleForOnline = true
        every { syncManager.canUseOnline() } returns true
        coEvery { gameApi.headGame(42) } returns Unit

        // When
        val result = repository.checkGameExists(42)

        // Then
        assertTrue(result.isSuccess)
        coVerify { gameApi.headGame(42) }
    }

    @Test
    fun `checkGameExists returns success when local game exists by id`() = runTest(testDispatcher) {
        // Given: Offline mode (server check skipped), game exists locally
        Config.isEligibleForOnline = false
        val gameEntity = GameEntity(id = 42, createdAt = 0L)
        coEvery { gameDao.getGameById(42) } returns gameEntity

        // When
        val result = repository.checkGameExists(42)

        // Then
        assertTrue(result.isSuccess)
        coVerify { gameDao.getGameById(42) }
    }

    @Test
    fun `checkGameExists returns failure when game not found anywhere`() = runTest(testDispatcher) {
        // Given: Offline mode, game not found anywhere
        Config.isEligibleForOnline = false
        coEvery { gameDao.getGameById(999) } returns null

        // When
        val result = repository.checkGameExists(999)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("999") == true)
    }

    @Test
    fun `checkGameExists falls back to local when server check fails`() = runTest(testDispatcher) {
        // Given: Online mode, server fails, but local game exists
        Config.isEligibleForOnline = true
        every { syncManager.canUseOnline() } returns true
        coEvery { gameApi.headGame(42) } throws Exception("Network error")
        val gameEntity = GameEntity(id = 42, createdAt = 0L)
        coEvery { gameDao.getGameById(42) } returns gameEntity

        // When
        val result = repository.checkGameExists(42)

        // Then
        assertTrue(result.isSuccess)
        coVerify { gameApi.headGame(42) }
        coVerify { gameDao.getGameById(42) }
    }

    @Test
    fun `checkGameExists skips server when not eligible for online`() = runTest(testDispatcher) {
        // Given: Not eligible for online
        Config.isEligibleForOnline = false
        val gameEntity = GameEntity(id = 42, createdAt = 0L)
        coEvery { gameDao.getGameById(42) } returns gameEntity

        // When
        val result = repository.checkGameExists(42)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { gameApi.headGame(any()) }
        coVerify { gameDao.getGameById(42) }
    }

    @Test
    fun `checkGameExists skips server when not connected`() = runTest(testDispatcher) {
        // Given: Eligible but not connected
        Config.isEligibleForOnline = true
        every { syncManager.canUseOnline() } returns false
        val gameEntity = GameEntity(id = 42, createdAt = 0L)
        coEvery { gameDao.getGameById(42) } returns gameEntity

        // When
        val result = repository.checkGameExists(42)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { gameApi.headGame(any()) }
        coVerify { gameDao.getGameById(42) }
    }

    // ==================== clearGame() TESTS ====================

    @Test
    fun `clearGame delegates to gameDao`() = runTest(testDispatcher) {
        // Given
        coEvery { gameDao.clearGame() } returns Unit

        // When
        repository.clearGame()

        // Then
        coVerify { gameDao.clearGame() }
    }

    // ==================== subscribeToGameUpdates() TESTS ====================

    @Test
    fun `subscribeToGameUpdates skips when not eligible for online`() = runTest(testDispatcher) {
        // Given
        Config.isEligibleForOnline = false

        // When
        repository.subscribeToGameUpdates(42)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: gameApi.getGameFlow should not be called
        coVerify(exactly = 0) { gameApi.getGameFlow(any()) }
    }

    @Test
    fun `subscribeToGameUpdates skips when not connected`() = runTest(testDispatcher) {
        // Given
        Config.isEligibleForOnline = true
        every { syncManager.canUseOnline() } returns false

        // When
        repository.subscribeToGameUpdates(42)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { gameApi.getGameFlow(any()) }
    }
}
