package com.receparslan.travelbook.roomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import com.receparslan.travelbook.model.Location

@Database(entities = [Location::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDAO(): LocationDAO
}