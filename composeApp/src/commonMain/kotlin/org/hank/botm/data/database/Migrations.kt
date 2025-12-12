package org.hank.botm.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration from version 1 to 2: Add sync tracking fields to all entities.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Add sync columns to game table
        connection.execSQL("ALTER TABLE game ADD COLUMN serverId INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE game ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE game ADD COLUMN lastSyncedAt INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE game ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_game_serverId ON game(serverId)")

        // Add sync columns to player table
        connection.execSQL("ALTER TABLE player ADD COLUMN serverId INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE player ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_player_serverId ON player(serverId)")

        // Add sync columns to round table
        connection.execSQL("ALTER TABLE round ADD COLUMN serverId INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE round ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_round_serverId ON round(serverId)")

        // Add sync columns to result table
        connection.execSQL("ALTER TABLE result ADD COLUMN serverId INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE result ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_result_serverId ON result(serverId)")
    }
}

/**
 * All migrations to apply to the database.
 */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
