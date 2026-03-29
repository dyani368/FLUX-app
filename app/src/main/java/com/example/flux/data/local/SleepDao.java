package com.example.flux.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SleepDao {

    @Insert
    void insert(SleepLog sleepLog);

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC")
    LiveData<List<SleepLog>> getAllSleepLogs();

    @Query("SELECT * FROM sleep_logs WHERE date >= :from AND date <= :to")
    List<SleepLog> getLogsBetween(long from, long to);

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC LIMIT 7")
    LiveData<List<SleepLog>> getLastSevenDays();

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC")
    List<SleepLog> getAllSleepLogsSync();
}