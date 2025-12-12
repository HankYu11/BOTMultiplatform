package org.hank.botm.domain.usecase

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hank.botm.data.repository.RoundRepository
import org.hank.botm.domain.GameConstants
import org.hank.botm.domain.model.Result
import org.hank.botm.ui.model.PlayerResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRoundUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake repository that captures calls for verification
    private class FakeRoundRepository : RoundRepository {
        var lastGameId: Int? = null
        var lastBet: Int? = null
        var lastResults: List<Result>? = null
        var shouldFail = false
        var failureException: Exception = Exception("Test failure")

        override suspend fun insertRound(
            gameId: Int,
            bet: Int,
            results: List<Result>
        ): kotlin.Result<Unit> {
            lastGameId = gameId
            lastBet = bet
            lastResults = results
            return if (shouldFail) {
                kotlin.Result.failure(failureException)
            } else {
                kotlin.Result.success(Unit)
            }
        }
    }

    // ==================== PROFIT CALCULATION TESTS ====================

    @Test
    fun `winner receives sum of all losers losses`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "Player1", playerId = 1, cardsInHand = "5", isWinner = true),
            PlayerResult(playerName = "Player2", playerId = 2, cardsInHand = "3", isWinner = false),
            PlayerResult(playerName = "Player3", playerId = 3, cardsInHand = "4", isWinner = false),
            PlayerResult(playerName = "Player4", playerId = 4, cardsInHand = "2", isWinner = false),
        )
        val bet = 2

        // Losers' total = (3 + 4 + 2) * 2 = 18
        // Winner should get +18
        // Loser2: -3 * 2 = -6
        // Loser3: -4 * 2 = -8
        // Loser4: -2 * 2 = -4

        val result = useCase(gameId = 1, bet = bet, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        assertEquals(4, results.size)
        assertEquals(18, results.find { it.playerId == 1 }?.profit)  // Winner
        assertEquals(-6, results.find { it.playerId == 2 }?.profit)  // Loser
        assertEquals(-8, results.find { it.playerId == 3 }?.profit)  // Loser
        assertEquals(-4, results.find { it.playerId == 4 }?.profit)  // Loser
    }

    @Test
    fun `profit calculation with bet of 1`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "7", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "1", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // Total losers' cards = 5 + 7 + 1 = 13
        assertEquals(13, results.find { it.playerId == 1 }?.profit)   // Winner
        assertEquals(-5, results.find { it.playerId == 2 }?.profit)
        assertEquals(-7, results.find { it.playerId == 3 }?.profit)
        assertEquals(-1, results.find { it.playerId == 4 }?.profit)
    }

    @Test
    fun `profit calculation with high bet multiplier`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "10", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "10", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "10", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 100, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // Total = (10 + 10 + 10) * 100 = 3000
        assertEquals(3000, results.find { it.playerId == 1 }?.profit)
        assertEquals(-1000, results.find { it.playerId == 2 }?.profit)
        assertEquals(-1000, results.find { it.playerId == 3 }?.profit)
        assertEquals(-1000, results.find { it.playerId == 4 }?.profit)
    }

    @Test
    fun `profit sum equals zero (zero-sum game)`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "3", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "8", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 7, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // Verify zero-sum: all profits should sum to 0
        val totalProfit = results.sumOf { it.profit }
        assertEquals(0, totalProfit, "Game should be zero-sum")
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `all losers have zero cards`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "0", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "0", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "0", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 10, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // All should be 0
        results.forEach {
            assertEquals(0, it.profit, "All profits should be 0 when losers have no cards")
        }
    }

    @Test
    fun `winner cards in hand value is ignored in calculation`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        // Winner has cards in hand but should be ignored (winner always gets 0 cards)
        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "13", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // Winner's profit should be sum of losers' cards only (5+5+5=15), not including their own
        assertEquals(15, results.find { it.playerId == 1 }?.profit)
    }

    @Test
    fun `maximum cards scenario (all losers have 13 cards)`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "13", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "13", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "13", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 10, playerResults = playerResults)

        assertTrue(result.isSuccess)
        val results = fakeRepository.lastResults!!

        // (13 + 13 + 13) * 10 = 390
        assertEquals(390, results.find { it.playerId == 1 }?.profit)
        assertEquals(-130, results.find { it.playerId == 2 }?.profit)
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    fun `fails when player count is less than 4`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("${GameConstants.PLAYER_COUNT}") == true)
    }

    @Test
    fun `fails when player count is more than 4`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "E", playerId = 5, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }

    @Test
    fun `fails when cardsInHand is not a valid number`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "abc", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("not a number") == true)
    }

    @Test
    fun `fails when cardsInHand is empty string`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = "", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `fails when cardsInHand contains whitespace`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = listOf(
            PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
            PlayerResult(playerName = "B", playerId = 2, cardsInHand = " 5 ", isWinner = false),
            PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
            PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
        )

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        // toIntOrNull returns null for " 5 " (with spaces)
        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when empty player list`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val result = useCase(gameId = 1, bet = 1, playerResults = emptyList())

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
    }

    // ==================== REPOSITORY INTEGRATION TESTS ====================

    @Test
    fun `passes correct gameId to repository`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = createValidPlayerResults()

        useCase(gameId = 42, bet = 1, playerResults = playerResults)

        assertEquals(42, fakeRepository.lastGameId)
    }

    @Test
    fun `passes correct bet to repository`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)

        val playerResults = createValidPlayerResults()

        useCase(gameId = 1, bet = 99, playerResults = playerResults)

        assertEquals(99, fakeRepository.lastBet)
    }

    @Test
    fun `propagates repository failure`() = runTest(testDispatcher) {
        val fakeRepository = FakeRoundRepository()
        fakeRepository.shouldFail = true
        fakeRepository.failureException = RuntimeException("Network error")

        val useCase = CreateRoundUseCase(fakeRepository, testDispatcher)
        val playerResults = createValidPlayerResults()

        val result = useCase(gameId = 1, bet = 1, playerResults = playerResults)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    // ==================== HELPER FUNCTIONS ====================

    private fun createValidPlayerResults(): List<PlayerResult> = listOf(
        PlayerResult(playerName = "A", playerId = 1, cardsInHand = "0", isWinner = true),
        PlayerResult(playerName = "B", playerId = 2, cardsInHand = "5", isWinner = false),
        PlayerResult(playerName = "C", playerId = 3, cardsInHand = "5", isWinner = false),
        PlayerResult(playerName = "D", playerId = 4, cardsInHand = "5", isWinner = false),
    )
}
