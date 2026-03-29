package com.example.flux.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import com.example.flux.data.local.FluxDatabase;
import com.example.flux.data.local.Habit;
import com.example.flux.data.local.HabitDao;
import com.example.flux.data.local.HabitLog;
import com.example.flux.data.local.SleepDao;
import com.example.flux.data.local.SleepLog;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitRepository {

    private final HabitDao habitDao;
    private final SleepDao sleepDao;
    private final ExecutorService executor;

    public HabitRepository(Application application) {
        FluxDatabase db = FluxDatabase.getInstance(application);
        habitDao = db.habitDao();
        sleepDao = db.sleepDao();
        executor = Executors.newSingleThreadExecutor();
    }

    // Habits
    public LiveData<List<Habit>> getAllHabits() {
        return habitDao.getAllHabits();
    }

    public void insertHabit(Habit habit) {
        executor.execute(() -> habitDao.insert(habit));
    }

    public void updateHabit(Habit habit) {
        executor.execute(() -> habitDao.update(habit));
    }

    public void deleteHabit(Habit habit) {
        executor.execute(() -> habitDao.delete(habit));
    }

    public void syncHabitToFirestore(Habit habit, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("name", habit.name);
        data.put("category", habit.category);
        data.put("frequency", habit.frequency);
        data.put("difficulty", habit.difficulty);
        data.put("createdAt", habit.createdAt);
        data.put("isPaused", habit.isPaused);

        db.collection("users").document(userId)
                .collection("habits").document(String.valueOf(habit.id))
                .set(data);
    }

    // Habit logs
    public void logHabit(HabitLog log) {
        executor.execute(() -> habitDao.insertLog(log));
    }

    public LiveData<List<HabitLog>> getTodayLogs(long startOfDay) {
        return habitDao.getTodayLogs(startOfDay);
    }

    public LiveData<List<HabitLog>> getLogsForHabit(int habitId) {
        return habitDao.getLogsForHabit(habitId);
    }

    public LiveData<List<HabitLog>> getAllLogs() {
        return habitDao.getAllLogs();
    }

    // Sleep
    public void insertSleepLog(SleepLog log) {
        executor.execute(() -> sleepDao.insert(log));
    }

    public LiveData<List<SleepLog>> getLastSevenDays() {
        return sleepDao.getLastSevenDays();
    }

    public void exportData(ExportCallback callback) {
        executor.execute(() -> {
            List<Habit> habits = habitDao.getAllHabitsSync();
            List<HabitLog> logs = habitDao.getAllLogsSync();
            List<SleepLog> sleep = sleepDao.getAllSleepLogsSync();
            new Handler(Looper.getMainLooper()).post(() ->
                callback.onReady(habits, logs, sleep));
        });
    }

    public interface ExportCallback {
        void onReady(List<Habit> habits, List<HabitLog> logs, List<SleepLog> sleep);
    }
}
