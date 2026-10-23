package com.lihan.localvault.core.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    private const val DB_NAME = "local_vault.db"

    fun create(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        ).build()
    }
}
