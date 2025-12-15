package org.hank.botm.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.GameWithDetails
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake repository for testing
    private class FakeGameRepository : GameRepository {
        var checkGameExistsResult: Result<Unit> = Result.success(Unit)
        var lastCheckedGameId: Int? = null

        override val game: Flow<Game?> = emptyFlow()
        override val gameWithDetails: Flow<GameWithDetails> = emptyFlow()

        override suspend fun createGame(createGame: CreateGame): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun clearGame() {}

        override suspend fun checkGameExists(gameId: Int): Result<Unit> {
            lastCheckedGameId = gameId
            return checkGameExistsResult
        }

        override fun subscribeToGameUpdates(gameId: Int) {}
        override fun disconnectFromGameUpdates() {}
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== DIALOG STATE TESTS ====================

    @Test
    fun `initial state has showJoinGameDialog false`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = LobbyViewModel(fakeRepository)

        assertFalse(viewModel.state.value.showJoinGameDialog)
    }

    @Test
    fun `showJoinGameDialog sets dialog visibility to true`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = LobbyViewModel(fakeRepository)

        viewModel.showJoinGameDialog()

        assertTrue(viewModel.state.value.showJoinGameDialog)
    }

    @Test
    fun `hideJoinGameDialog sets dialog visibility to false`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = LobbyViewModel(fakeRepository)

        // First show the dialog
        viewModel.showJoinGameDialog()
        assertTrue(viewModel.state.value.showJoinGameDialog)

        // Then hide it
        viewModel.hideJoinGameDialog()
        assertFalse(viewModel.state.value.showJoinGameDialog)
    }

    // ==================== JOIN GAME TESTS ====================

    @Test
    fun `joinGame navigates to room on success`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.checkGameExistsResult = Result.success(Unit)
        val viewModel = LobbyViewModel(fakeRepository)

        viewModel.joinGame(42)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify game ID was checked
        assertEquals(42, fakeRepository.lastCheckedGameId)

        // Verify navigation was triggered
        val navigatedGameId = viewModel.navigateToRoom.first()
        assertEquals(42, navigatedGameId)
    }

    @Test
    fun `joinGame shows error on failure`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.checkGameExistsResult = Result.failure(Exception("Game not found"))
        val viewModel = LobbyViewModel(fakeRepository)

        viewModel.joinGame(999)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify game ID was checked
        assertEquals(999, fakeRepository.lastCheckedGameId)

        // Verify error state is set
        assertEquals("Game with id 999 does not exist", viewModel.state.value.error)
    }

    @Test
    fun `joinGame error message contains game ID`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.checkGameExistsResult = Result.failure(Exception("Not found"))
        val viewModel = LobbyViewModel(fakeRepository)

        viewModel.joinGame(12345)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.error?.contains("12345") == true)
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    fun `initial state has no error`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = LobbyViewModel(fakeRepository)

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `dismissError clears error state`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.checkGameExistsResult = Result.failure(Exception("Error"))
        val viewModel = LobbyViewModel(fakeRepository)

        // Trigger an error
        viewModel.joinGame(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error exists
        assertTrue(viewModel.state.value.error != null)

        // Dismiss the error
        viewModel.dismissError()

        // Verify error is cleared
        assertNull(viewModel.state.value.error)
    }
}
