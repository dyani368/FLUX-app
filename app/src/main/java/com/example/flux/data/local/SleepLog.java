package com.example.flux.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep_logs")
public class SleepLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public float hours;        // e.g. 6.5
    public int energyLevel;    // 1-5
    public long date;          // store as timestamp
}