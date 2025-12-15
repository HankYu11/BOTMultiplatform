package org.hank.botm.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateGameTest {

    // ==================== isValid() TESTS ====================

    @Test
    fun `isValid returns true when all player names are non-empty`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertTrue(createGame.isValid())
    }

    @Test
    fun `isValid returns false when player1Name is empty`() {
        val createGame = CreateGame(
            player1Name = "",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertFalse(createGame.isValid())
    }

    @Test
    fun `isValid returns false when player2Name is empty`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertFalse(createGame.isValid())
    }

    @Test
    fun `isValid returns false when player3Name is empty`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "",
            player4Name = "David"
        )

        assertFalse(createGame.isValid())
    }

    @Test
    fun `isValid returns false when player4Name is empty`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = ""
        )

        assertFalse(createGame.isValid())
    }

    @Test
    fun `isValid returns false when all names are empty`() {
        val createGame = CreateGame()

        assertFalse(createGame.isValid())
    }

    @Test
    fun `isValid returns true with single character names`() {
        val createGame = CreateGame(
            player1Name = "A",
            player2Name = "B",
            player3Name = "C",
            player4Name = "D"
        )

        assertTrue(createGame.isValid())
    }

    // ==================== getNameByIndex() TESTS ====================

    @Test
    fun `getNameByIndex returns player1Name for index 0`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertEquals("Alice", createGame.getNameByIndex(0))
    }

    @Test
    fun `getNameByIndex returns player2Name for index 1`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertEquals("Bob", createGame.getNameByIndex(1))
    }

    @Test
    fun `getNameByIndex returns player3Name for index 2`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertEquals("Charlie", createGame.getNameByIndex(2))
    }

    @Test
    fun `getNameByIndex returns player4Name for index 3`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertEquals("David", createGame.getNameByIndex(3))
    }

    @Test
    fun `getNameByIndex throws for negative index`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertFailsWith<IllegalArgumentException> {
            createGame.getNameByIndex(-1)
        }
    }

    @Test
    fun `getNameByIndex throws for index greater than 3`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        assertFailsWith<IllegalArgumentException> {
            createGame.getNameByIndex(4)
        }
    }

    // ==================== withNameChanged() TESTS ====================

    @Test
    fun `withNameChanged updates player1Name for index 0`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val updated = createGame.withNameChanged(0, "NewAlice")

        assertEquals("NewAlice", updated.player1Name)
        assertEquals("Bob", updated.player2Name)
        assertEquals("Charlie", updated.player3Name)
        assertEquals("David", updated.player4Name)
    }

    @Test
    fun `withNameChanged updates player2Name for index 1`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val updated = createGame.withNameChanged(1, "NewBob")

        assertEquals("Alice", updated.player1Name)
        assertEquals("NewBob", updated.player2Name)
        assertEquals("Charlie", updated.player3Name)
        assertEquals("David", updated.player4Name)
    }

    @Test
    fun `withNameChanged updates player3Name for index 2`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val updated = createGame.withNameChanged(2, "NewCharlie")

        assertEquals("Alice", updated.player1Name)
        assertEquals("Bob", updated.player2Name)
        assertEquals("NewCharlie", updated.player3Name)
        assertEquals("David", updated.player4Name)
    }

    @Test
    fun `withNameChanged updates player4Name for index 3`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val updated = createGame.withNameChanged(3, "NewDavid")

        assertEquals("Alice", updated.player1Name)
        assertEquals("Bob", updated.player2Name)
        assertEquals("Charlie", updated.player3Name)
        assertEquals("NewDavid", updated.player4Name)
    }

    @Test
    fun `withNameChanged returns unchanged instance for invalid index`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val unchanged = createGame.withNameChanged(5, "Invalid")

        assertEquals(createGame, unchanged)
    }

    @Test
    fun `withNameChanged returns unchanged instance for negative index`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val unchanged = createGame.withNameChanged(-1, "Invalid")

        assertEquals(createGame, unchanged)
    }

    @Test
    fun `withNameChanged creates a new instance - immutability`() {
        val original = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val updated = original.withNameChanged(0, "NewAlice")

        // Original should be unchanged
        assertEquals("Alice", original.player1Name)
        // Updated should have new value
        assertEquals("NewAlice", updated.player1Name)
    }

    // ==================== toDto() TESTS ====================

    @Test
    fun `toDto creates DTO with correct player names order`() {
        val createGame = CreateGame(
            player1Name = "Alice",
            player2Name = "Bob",
            player3Name = "Charlie",
            player4Name = "David"
        )

        val dto = createGame.toDto()

        assertEquals(listOf("Alice", "Bob", "Charlie", "David"), dto.playerNames)
    }

    @Test
    fun `toDto handles empty names`() {
        val createGame = CreateGame()

        val dto = createGame.toDto()

        assertEquals(listOf("", "", "", ""), dto.playerNames)
    }

    @Test
    fun `toDto preserves special characters in names`() {
        val createGame = CreateGame(
            player1Name = "玩家1",
            player2Name = "Player-2",
            player3Name = "P@yer#3",
            player4Name = "Player 4"
        )

        val dto = createGame.toDto()

        assertEquals(listOf("玩家1", "Player-2", "P@yer#3", "Player 4"), dto.playerNames)
    }
}
