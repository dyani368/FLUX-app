package com.example.flux.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Habit.class, HabitLog.class, SleepLog.class}, version = 2, exportSchema = false)
public abstract class FluxDatabase extends RoomDatabase {

    private static FluxDatabase instance;

    public abstract HabitDao habitDao();
    public abstract SleepDao sleepDao();

    public static synchronized FluxDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FluxDatabase.class,
                            "flux_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}