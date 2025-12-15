package org.hank.botm

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineIdGeneratorTest {

    @BeforeTest
    fun setup() {
        // Reset counter before each test for isolation
        OfflineIdGenerator.reset()
    }

    // ==================== nextId() TESTS ====================

    @Test
    fun `nextId returns negative integers starting from -1`() {
        val firstId = OfflineIdGenerator.nextId()
        assertEquals(-1, firstId)
    }

    @Test
    fun `nextId returns sequential negative IDs`() {
        val id1 = OfflineIdGenerator.nextId()
        val id2 = OfflineIdGenerator.nextId()
        val id3 = OfflineIdGenerator.nextId()

        assertEquals(-1, id1)
        assertEquals(-2, id2)
        assertEquals(-3, id3)
    }

    @Test
    fun `nextId generates unique IDs across multiple calls`() {
        val ids = (1..100).map { OfflineIdGenerator.nextId() }

        // All IDs should be unique
        assertEquals(100, ids.toSet().size, "All generated IDs should be unique")

        // All IDs should be negative
        assertTrue(ids.all { it < 0 }, "All generated IDs should be negative")

        // IDs should be sequential from -1 to -100
        assertEquals(-1, ids.first())
        assertEquals(-100, ids.last())
    }

    // ==================== isOfflineId() TESTS ====================

    @Test
    fun `isOfflineId returns true for negative IDs`() {
        assertTrue(OfflineIdGenerator.isOfflineId(-1))
        assertTrue(OfflineIdGenerator.isOfflineId(-100))
        assertTrue(OfflineIdGenerator.isOfflineId(-999999))
        assertTrue(OfflineIdGenerator.isOfflineId(Int.MIN_VALUE))
    }

    @Test
    fun `isOfflineId returns false for positive IDs`() {
        assertFalse(OfflineIdGenerator.isOfflineId(1))
        assertFalse(OfflineIdGenerator.isOfflineId(100))
        assertFalse(OfflineIdGenerator.isOfflineId(999999))
        assertFalse(OfflineIdGenerator.isOfflineId(Int.MAX_VALUE))
    }

    @Test
    fun `isOfflineId returns false for zero`() {
        assertFalse(OfflineIdGenerator.isOfflineId(0))
    }

    // ==================== reset() TESTS ====================

    @Test
    fun `reset restarts counter from -1`() {
        // Generate some IDs
        OfflineIdGenerator.nextId()
        OfflineIdGenerator.nextId()
        OfflineIdGenerator.nextId()

        // Reset
        OfflineIdGenerator.reset()

        // Should start from -1 again
        val id = OfflineIdGenerator.nextId()
        assertEquals(-1, id)
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    fun `generated IDs are correctly identified as offline IDs`() {
        val id1 = OfflineIdGenerator.nextId()
        val id2 = OfflineIdGenerator.nextId()

        assertTrue(OfflineIdGenerator.isOfflineId(id1))
        assertTrue(OfflineIdGenerator.isOfflineId(id2))
    }

    @Test
    fun `server IDs - positive - are not identified as offline IDs`() {
        // Simulate server IDs
        val serverIds = listOf(1, 42, 100, 9999)

        serverIds.forEach { serverId ->
            assertFalse(
                OfflineIdGenerator.isOfflineId(serverId),
                "Server ID $serverId should not be identified as offline ID"
            )
        }
    }
}
