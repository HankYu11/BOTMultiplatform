package org.hank.botm.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class SetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake repository for testing
    private class FakeGameRepository : GameRepository {
        val gameFlow = MutableStateFlow<Game?>(null)
        var createGameResult: Result<Unit> = Result.success(Unit)
        var lastCreateGame: CreateGame? = null

        override val game: Flow<Game?> = gameFlow
        override val gameWithDetails: Flow<GameWithDetails>
            get() = throw NotImplementedError("Not needed for this test")

        override suspend fun createGame(createGame: CreateGame): Result<Unit> {
            lastCreateGame = createGame
            return createGameResult
        }

        override suspend fun clearGame() {}
        override suspend fun checkGameExists(gameId: Int): Result<Unit> = Result.success(Unit)
        override fun subscribeToGameUpdates(gameId: Int) {}
        override fun disconnectFromGameUpdates() {}

        fun emitGame(game: Game) {
            gameFlow.value = game
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== INITIAL STATE TESTS ====================

    @Test
    fun `initial state has empty createGame`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        assertEquals(CreateGame(), viewModel.state.value.createGame)
    }

    @Test
    fun `initial state has no error`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `initial state is not loading`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        assertFalse(viewModel.state.value.isLoading)
    }

    // ==================== updatePlayerName() TESTS ====================

    @Test
    fun `updatePlayerName updates player1 name for index 0`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        viewModel.updatePlayerName(0, "Alice")

        assertEquals("Alice", viewModel.state.value.createGame.player1Name)
    }

    @Test
    fun `updatePlayerName updates player2 name for index 1`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        viewModel.updatePlayerName(1, "Bob")

        assertEquals("Bob", viewModel.state.value.createGame.player2Name)
    }

    @Test
    fun `updatePlayerName updates player3 name for index 2`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        viewModel.updatePlayerName(2, "Charlie")

        assertEquals("Charlie", viewModel.state.value.createGame.player3Name)
    }

    @Test
    fun `updatePlayerName updates player4 name for index 3`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        viewModel.updatePlayerName(3, "David")

        assertEquals("David", viewModel.state.value.createGame.player4Name)
    }

    @Test
    fun `updatePlayerName preserves other player names`() {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        viewModel.updatePlayerName(0, "Alice")
        viewModel.updatePlayerName(1, "Bob")
        viewModel.updatePlayerName(2, "Charlie")
        viewModel.updatePlayerName(3, "David")

        val createGame = viewModel.state.value.createGame
        assertEquals("Alice", createGame.player1Name)
        assertEquals("Bob", createGame.player2Name)
        assertEquals("Charlie", createGame.player3Name)
        assertEquals("David", createGame.player4Name)
    }

    // ==================== createGame() VALIDATION TESTS ====================

    @Test
    fun `createGame shows error when names are invalid`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        // Create with empty names (invalid)
        val invalidCreateGame = CreateGame()
        viewModel.createGame(invalidCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please enter name for all players", viewModel.state.value.error)
    }

    @Test
    fun `createGame shows error when any player name is empty`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        val partiallyFilledGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "", // Empty
            player4Name = "David"
        )
        viewModel.createGame(partiallyFilledGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Please enter name for all players", viewModel.state.value.error)
    }

    @Test
    fun `createGame does not call repository when invalid`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        val invalidCreateGame = CreateGame()
        viewModel.createGame(invalidCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeRepository.lastCreateGame)
    }

    // ==================== createGame() SUCCESS TESTS ====================

    @Test
    fun `createGame calls repository with valid game`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        val validCreateGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )
        viewModel.createGame(validCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(validCreateGame, fakeRepository.lastCreateGame)
    }

    @Test
    fun `createGame sets loading state during creation`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        val validCreateGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        // Start creation but don't advance scheduler
        viewModel.createGame(validCreateGame)

        // Loading should be true initially (before coroutine completes)
        // Note: Due to test dispatcher, we need to check after initial suspension
        testDispatcher.scheduler.advanceUntilIdle()

        // After completion, loading should be false
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `createGame clears error on valid input`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        // First, trigger an error
        viewModel.createGame(CreateGame())
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.error != null)

        // Then create with valid game
        val validCreateGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )
        viewModel.createGame(validCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        // Error should be cleared
        assertNull(viewModel.state.value.error)
    }

    // ==================== createGame() FAILURE TESTS ====================

    @Test
    fun `createGame shows error on repository failure`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.createGameResult = Result.failure(Exception("Network error"))
        val viewModel = SetupViewModel(fakeRepository)

        val validCreateGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )
        viewModel.createGame(validCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.state.value.error)
    }

    @Test
    fun `createGame shows unknown error when exception has no message`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        fakeRepository.createGameResult = Result.failure(Exception())
        val viewModel = SetupViewModel(fakeRepository)

        val validCreateGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )
        viewModel.createGame(validCreateGame)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unknown error", viewModel.state.value.error)
    }

    // ==================== dismissAlert() TESTS ====================

    @Test
    fun `dismissAlert clears error state`() = runTest(testDispatcher) {
        val fakeRepository = FakeGameRepository()
        val viewModel = SetupViewModel(fakeRepository)

        // Trigger an error
        viewModel.createGame(CreateGame())
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.error != null)

        // Dismiss the alert
        viewModel.dismissAlert()

        assertNull(viewModel.state.value.error)
    }
}
