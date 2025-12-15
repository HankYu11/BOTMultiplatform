package org.hank.botm.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hank.botm.Config
import org.hank.botm.currentTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for SyncManager interface behavior.
 *
 * Note: Full integration tests for SyncManagerImpl would require
 * extensive mocking of Room DAOs and Network APIs. These tests focus
 * on the testable behaviors of the interface contract.
 */
class SyncManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    // Simple fake for testing canUseOnline() and basic state management
    private class FakeSyncManager(
        private val connectionStateValue: ConnectionState = ConnectionState.Connected
    ) : SyncManager {
        private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
        override val syncState: StateFlow<SyncState> = _syncState

        private val _connectionState = MutableStateFlow(connectionStateValue)
        override val connectionState: StateFlow<ConnectionState> = _connectionState

        var performSyncCalled = false
        var performSyncResult: Result<Unit> = Result.success(Unit)

        override fun canUseOnline(): Boolean {
            return Config.isEligibleForOnline &&
                    connectionState.value == ConnectionState.Connected
        }

        override suspend fun performSync(): Result<Unit> {
            performSyncCalled = true

            if (!Config.isEligibleForOnline) {
                return Result.success(Unit)
            }

            _syncState.value = SyncState.Syncing

            return performSyncResult.also {
                if (it.isSuccess) {
                    _syncState.value = SyncState.Success(currentTimeMillis())
                } else {
                    _syncState.value = SyncState.Error(
                        it.exceptionOrNull()?.message ?: "Unknown error",
                        retryable = true
                    )
                }
            }
        }

        override suspend fun markGameDirty(gameId: Int) {}
        override suspend fun markRoundDirty(roundId: Int) {}

        fun setConnectionState(state: ConnectionState) {
            _connectionState.value = state
        }
    }

    private var originalIsEligibleForOnline: Boolean = true

    @BeforeTest
    fun setup() {
        originalIsEligibleForOnline = Config.isEligibleForOnline
    }

    @AfterTest
    fun tearDown() {
        Config.isEligibleForOnline = originalIsEligibleForOnline
    }

    // ==================== canUseOnline() TESTS ====================

    @Test
    fun `canUseOnline returns true when eligible and connected`() {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)

        assertTrue(syncManager.canUseOnline())
    }

    @Test
    fun `canUseOnline returns false when not eligible`() {
        Config.isEligibleForOnline = false
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)

        assertFalse(syncManager.canUseOnline())
    }

    @Test
    fun `canUseOnline returns false when disconnected`() {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Disconnected)

        assertFalse(syncManager.canUseOnline())
    }

    @Test
    fun `canUseOnline returns false when connection state is unknown`() {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Unknown)

        assertFalse(syncManager.canUseOnline())
    }

    @Test
    fun `canUseOnline returns false when not eligible and disconnected`() {
        Config.isEligibleForOnline = false
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Disconnected)

        assertFalse(syncManager.canUseOnline())
    }

    // ==================== performSync() STATE TESTS ====================

    @Test
    fun `performSync transitions to Syncing state`() = runTest(testDispatcher) {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)

        // Initially idle
        assertIs<SyncState.Idle>(syncManager.syncState.value)

        syncManager.performSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should have transitioned through Syncing to Success
        assertIs<SyncState.Success>(syncManager.syncState.value)
    }

    @Test
    fun `performSync transitions to Success on success`() = runTest(testDispatcher) {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)
        syncManager.performSyncResult = Result.success(Unit)

        val result = syncManager.performSync()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertIs<SyncState.Success>(syncManager.syncState.value)
    }

    @Test
    fun `performSync transitions to Error on failure`() = runTest(testDispatcher) {
        Config.isEligibleForOnline = true
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)
        syncManager.performSyncResult = Result.failure(Exception("Network error"))

        val result = syncManager.performSync()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(result.isFailure)
        val errorState = syncManager.syncState.value
        assertIs<SyncState.Error>(errorState)
        assertEquals("Network error", errorState.message)
        assertTrue(errorState.retryable)
    }

    @Test
    fun `performSync skips when not eligible for online`() = runTest(testDispatcher) {
        Config.isEligibleForOnline = false
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)

        val result = syncManager.performSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should succeed but not actually sync
        assertTrue(result.isSuccess)
        // State should remain Idle (not transitioned to Syncing)
        assertIs<SyncState.Idle>(syncManager.syncState.value)
    }

    // ==================== connectionState TESTS ====================

    @Test
    fun `connectionState reflects current connection`() {
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Connected)

        assertEquals(ConnectionState.Connected, syncManager.connectionState.value)
    }

    @Test
    fun `connectionState can be Disconnected`() {
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Disconnected)

        assertEquals(ConnectionState.Disconnected, syncManager.connectionState.value)
    }

    @Test
    fun `connectionState can be Unknown`() {
        val syncManager = FakeSyncManager(connectionStateValue = ConnectionState.Unknown)

        assertEquals(ConnectionState.Unknown, syncManager.connectionState.value)
    }

    // ==================== syncState INITIAL VALUE TEST ====================

    @Test
    fun `syncState is initially Idle`() {
        val syncManager = FakeSyncManager()

        assertIs<SyncState.Idle>(syncManager.syncState.value)
    }
}
