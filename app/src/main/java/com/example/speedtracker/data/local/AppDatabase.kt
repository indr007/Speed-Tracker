package com.example.speedtracker.data.local

import android.content.Context
import androidx.room.*
import com.example.speedtracker.data.model.Trip

@Database(entities = [Trip::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val tripDao: TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speed_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
