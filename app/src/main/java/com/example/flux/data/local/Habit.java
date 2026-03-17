package com.example.flux.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "habits")
public class Habit {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String category;
    public String frequency;
    public int difficulty;
    public long createdAt;
    public boolean isPaused;


}
