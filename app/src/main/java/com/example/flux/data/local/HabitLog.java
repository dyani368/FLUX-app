package com.example.flux.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habit_logs")
public class HabitLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int habitId;     // links to Habit
    public long timestamp;  // when it was completed
    public boolean undone;  // for the 10-min undo window
    public String photoUri; // nullable, stores local URI of proof photo
}