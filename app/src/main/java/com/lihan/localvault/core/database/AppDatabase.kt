package com.lihan.localvault.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lihan.localvault.core.database.dao.VaultDao
import com.lihan.localvault.core.database.entity.VaultEntity

@Database(
    entities = [VaultEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract val vaultDao: VaultDao
}
