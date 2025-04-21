package com.receparslan.travelbook.roomDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.receparslan.travelbook.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDAO {
    @Query("SELECT * FROM locations")
    fun getAll(): Flow<List<Location>>

    @Query("SELECT * FROM locations WHERE id IN (:locationID)")
    fun loadByID(locationID: Int): Flow<Location>

    @Insert
    suspend fun insert(location: Location)

    @Delete
    suspend fun delete(location: Location)
}