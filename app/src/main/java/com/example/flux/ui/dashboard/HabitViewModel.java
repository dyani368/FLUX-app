package com.example.flux.ui.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.flux.data.local.Habit;
import com.example.flux.data.local.HabitLog;
import com.example.flux.data.local.SleepLog;
import com.example.flux.data.repository.HabitRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.List;

public class HabitViewModel extends AndroidViewModel {

    private final HabitRepository repository;
    private final LiveData<List<Habit>> allHabits;
    private final MutableLiveData<Float> burnoutIndex = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> momentumScore = new MutableLiveData<>(100f);

    public HabitViewModel(@NonNull Application application) {
        super(application);
        repository = new HabitRepository(application);
        allHabits = repository.getAllHabits();
    }

    public LiveData<List<Habit>> getAllHabits() {
        return allHabits;
    }

    public LiveData<Float> getBurnoutIndex() {
        return burnoutIndex;
    }

    public LiveData<Float> getMomentumScore() {
        return momentumScore;
    }

    public void insertHabit(Habit habit) {
        habit.createdAt = System.currentTimeMillis();
        repository.insertHabit(habit);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            repository.syncHabitToFirestore(habit, auth.getCurrentUser().getUid());
        }
    }

    public void deleteHabit(Habit habit) {
        repository.deleteHabit(habit);
    }

    public void togglePause(Habit habit) {
        habit.isPaused = !habit.isPaused;
        repository.updateHabit(habit);
    }

    public void logHabit(int habitId) {
        HabitLog log = new HabitLog();
        log.habitId = habitId;
        log.timestamp = System.currentTimeMillis();
        log.undone = false;
        repository.logHabit(log);
        updateMomentum(5f); // +5 momentum per completion
    }

    public LiveData<List<HabitLog>> getLogsForHabit(int habitId) {
        return repository.getLogsForHabit(habitId);
    }

    public LiveData<List<HabitLog>> getTodayLogs(long startOfDay) {
        return repository.getTodayLogs(startOfDay);
    }

    public LiveData<List<HabitLog>> getAllLogs() {
        return repository.getAllLogs();
    }

    public void insertSleepLog(SleepLog log) {
        repository.insertSleepLog(log);
    }

    public LiveData<List<SleepLog>> getLastSevenDays() {
        return repository.getLastSevenDays();
    }

    // Burnout: 40% missed habits + 40% low sleep + 20% overloaded days
    public void calculateBurnout(int missedHabits, int totalHabits,
                                 int lowSleepDays, int overloadedDays, int totalDays) {
        float missedRatio = totalHabits == 0 ? 0 : (float) missedHabits / totalHabits;
        float sleepRatio = totalDays == 0 ? 0 : (float) lowSleepDays / totalDays;
        float overloadRatio = totalDays == 0 ? 0 : (float) overloadedDays / totalDays;

        float index = (0.4f * missedRatio + 0.4f * sleepRatio + 0.2f * overloadRatio) * 100;
        burnoutIndex.setValue(index);
    }

    // Momentum degrades by 2 per missed day, gains 5 per completion
    private void updateMomentum(float delta) {
        float current = momentumScore.getValue() != null ? momentumScore.getValue() : 100f;
        float updated = Math.min(100f, Math.max(0f, current + delta));
        momentumScore.setValue(updated);
    }

    public long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
