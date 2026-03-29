package com.example.flux.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface HabitDao {

    @Insert
    void insert(Habit habit);

    @Update
    void update(Habit habit);

    @Delete
    void delete(Habit habit);

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    LiveData<List<Habit>> getAllHabits();

    @Query("SELECT * FROM habits WHERE id = :id")
    LiveData<Habit> getHabitById(int id);

    @Insert
    void insertLog(HabitLog log);

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY timestamp DESC")
    LiveData<List<HabitLog>> getLogsForHabit(int habitId);

    @Query("SELECT * FROM habit_logs WHERE timestamp >= :startOfDay")
    LiveData<List<HabitLog>> getTodayLogs(long startOfDay);

    @Query("SELECT * FROM habit_logs WHERE timestamp >= :from AND timestamp <= :to")
    List<HabitLog> getLogsBetween(long from, long to);

    @Query("SELECT * FROM habit_logs WHERE undone = 0")
    LiveData<List<HabitLog>> getAllLogs();

    @Query("SELECT * FROM habit_logs WHERE undone = 0")
    List<HabitLog> getAllLogsSync();

    @Query("SELECT * FROM habits")
    List<Habit> getAllHabitsSync();
}
