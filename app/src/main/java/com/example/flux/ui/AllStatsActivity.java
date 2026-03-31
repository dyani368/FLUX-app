package com.example.flux.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.R;
import com.example.flux.data.local.Habit;
import com.example.flux.data.local.HabitLog;
import com.example.flux.data.local.SleepLog;
import com.example.flux.ui.dashboard.HabitViewModel;
import com.example.flux.ui.dashboard.HeatmapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AllStatsActivity extends AppCompatActivity {

    private HabitViewModel viewModel;
    private List<Habit> habitList = new ArrayList<>();
    private Map<Integer, Integer> logCountMap = new HashMap<>();
    private StatsAdapter adapter;

    // tab views
    private View contentCalendar, contentAchievements;
    private RecyclerView rvAllStats;
    private TextView tabCalendar, tabAllHabits, tabAchievements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_stats);

        viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        contentCalendar     = findViewById(R.id.contentCalendar);
        contentAchievements = findViewById(R.id.contentAchievements);
        rvAllStats          = findViewById(R.id.rvAllStats);
        tabCalendar         = findViewById(R.id.tabCalendar);
        tabAllHabits        = findViewById(R.id.tabAllHabits);
        tabAchievements     = findViewById(R.id.tabAchievements);

        setupTabs();
        setupRecyclerView();
        setupObservers();
        setupHeatmap();
    }

    private void setupTabs() {
        tabCalendar.setOnClickListener(v -> showTab(0));
        tabAllHabits.setOnClickListener(v -> showTab(1));
        tabAchievements.setOnClickListener(v -> showTab(2));
        showTab(0); // default
    }

    private void showTab(int index) {
        contentCalendar.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        rvAllStats.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        contentAchievements.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        tabCalendar.setTextColor(index == 0 ? 0xFFFFFFFF : 0xFF666666);
        tabAllHabits.setTextColor(index == 1 ? 0xFFFFFFFF : 0xFF666666);
        tabAchievements.setTextColor(index == 2 ? 0xFFFFFFFF : 0xFF666666);

        setTabActive(tabCalendar, index == 0);
        setTabActive(tabAllHabits, index == 1);
        setTabActive(tabAchievements, index == 2);
    }

    private void setTabActive(TextView tab, boolean active) {
        tab.setTypeface(null, active
            ? android.graphics.Typeface.BOLD
            : android.graphics.Typeface.NORMAL);
    }

    private void setupRecyclerView() {
        adapter = new StatsAdapter(habitList, logCountMap);
        rvAllStats.setAdapter(adapter);
        rvAllStats.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupHeatmap() {
        TextView tvMonth = findViewById(R.id.tvMonthYear);
        Calendar cal = Calendar.getInstance();
        String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
        tvMonth.setText(month);
    }

    private void setupObservers() {
        viewModel.getAllHabits().observe(this, habits -> {
            habitList.clear();
            habitList.addAll(habits);
            adapter.notifyDataSetChanged();
            updateAchievements();
        });

        viewModel.getAllLogs().observe(this, logs -> {
            logCountMap.clear();
            int total = 0;
            long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            int thisWeek = 0;

            for (HabitLog log : logs) {
                if (!log.undone) {
                    logCountMap.put(log.habitId,
                        logCountMap.getOrDefault(log.habitId, 0) + 1);
                    total++;
                    if (log.timestamp >= weekAgo) thisWeek++;
                }
            }

            // update stat cards
            ((TextView) findViewById(R.id.tvHabitsFinished)).setText(String.valueOf(total));
            ((TextView) findViewById(R.id.tvFinishedWeek)).setText("This week: " + thisWeek);

            int totalHabits = habitList.size();
            int rate = totalHabits == 0 ? 0 : Math.min(100,
                (int)((total / Math.max(1f, totalHabits * 7f)) * 100));
            ((TextView) findViewById(R.id.tvCompletionRate)).setText(rate + "%");
            ((TextView) findViewById(R.id.tvRateHabits)).setText(thisWeek + "/" + totalHabits + " habits");

            // streak calculation
            int streak = calculateStreak(logs);
            ((TextView) findViewById(R.id.tvCurrentStreak)).setText(String.valueOf(streak));
            ((TextView) findViewById(R.id.tvBestStreak)).setText("Best Streak: " + streak);
            ((TextView) findViewById(R.id.tvLongestStreak)).setText(streak + " days");

            // heatmap
            HeatmapView heatmap = findViewById(R.id.heatmapView);
            Map<Integer, Integer> completions = new HashMap<>();
            Map<Integer, Float> sleep = new HashMap<>();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            long monthStart = cal.getTimeInMillis();
            for (HabitLog log : logs) {
                if (!log.undone && log.timestamp >= monthStart) {
                    Calendar lc = Calendar.getInstance();
                    lc.setTimeInMillis(log.timestamp);
                    int day = lc.get(Calendar.DAY_OF_MONTH);
                    completions.put(day, completions.getOrDefault(day, 0) + 1);
                }
            }
            heatmap.setData(completions, sleep,
                Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH));

            adapter.notifyDataSetChanged();
            updateAchievements();
        });

        viewModel.getLastSevenDays().observe(this, sleepLogs -> {
            if (sleepLogs.isEmpty()) return;
            float avg = 0;
            for (SleepLog s : sleepLogs) avg += s.hours;
            avg /= sleepLogs.size();
            ((TextView) findViewById(R.id.tvAvgSleep))
                .setText(String.format(Locale.getDefault(), "%.1f hrs", avg));
        });
    }

    private int calculateStreak(List<HabitLog> logs) {
        if (logs.isEmpty()) return 0;
        long today = getStartOfDay(System.currentTimeMillis());
        int streak = 0;
        long checkDay = today;
        while (true) {
            long finalCheckDay = checkDay;
            long nextDay = checkDay + 86400000L;
            boolean hasLog = logs.stream().anyMatch(l ->
                !l.undone && l.timestamp >= finalCheckDay && l.timestamp < nextDay);
            if (!hasLog) break;
            streak++;
            checkDay -= 86400000L;
        }
        return streak;
    }

    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void updateAchievements() {
        if (habitList.isEmpty() || logCountMap.isEmpty()) return;
        Habit best = null;
        int max = 0;
        for (Habit h : habitList) {
            int c = logCountMap.getOrDefault(h.id, 0);
            if (c > max) { max = c; best = h; }
        }
        if (best != null)
            ((TextView) findViewById(R.id.tvBestHabit)).setText(best.name);
    }

    // inner adapter
    static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.VH> {
        private final List<Habit> habits;
        private final Map<Integer, Integer> counts;

        StatsAdapter(List<Habit> habits, Map<Integer, Integer> counts) {
            this.habits = habits;
            this.counts = counts;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit_stat, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Habit habit = habits.get(position);
            int count = counts.getOrDefault(habit.id, 0);
            h.tvName.setText(habit.name);
            h.tvCategory.setText(habit.category
                + (habit.isPaused ? "  · PAUSED" : ""));
            h.tvCount.setText(count + " completions");
            long days = Math.max(1,
                (System.currentTimeMillis() - habit.createdAt) / 86400000L);
            int rate = (int) Math.min(100, (count / (float) days) * 100);
            h.tvRate.setText(rate + "%");
            if (rate >= 70)      h.tvRate.setTextColor(0xFF4CAF50);
            else if (rate >= 40) h.tvRate.setTextColor(0xFFFFB300);
            else                 h.tvRate.setTextColor(0xFFE53935);
        }

        @Override public int getItemCount() { return habits.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCategory, tvRate, tvCount;
            VH(View v) {
                super(v);
                tvName     = v.findViewById(R.id.tvStatHabitName);
                tvCategory = v.findViewById(R.id.tvStatCategory);
                tvRate     = v.findViewById(R.id.tvStatRate);
                tvCount    = v.findViewById(R.id.tvStatCount);
            }
        }
    }
}
