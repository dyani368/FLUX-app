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
import com.example.flux.ui.dashboard.HabitViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllStatsActivity extends AppCompatActivity {

    private HabitViewModel viewModel;
    private List<Habit> habitList = new ArrayList<>();
    private Map<Integer, Integer> logCountMap = new HashMap<>();
    private StatsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_stats);

        viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        RecyclerView rv = findViewById(R.id.rvAllStats);
        adapter = new StatsAdapter(habitList, logCountMap);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // observe habits
        viewModel.getAllHabits().observe(this, habits -> {
            habitList.clear();
            habitList.addAll(habits);
            ((TextView) findViewById(R.id.tvTotalHabits))
                .setText(String.valueOf(habits.size()));
            updateBestHabit();
            adapter.notifyDataSetChanged();
        });

        // observe all logs
        viewModel.getAllLogs().observe(this, logs -> {
            logCountMap.clear();
            int total = 0;
            for (HabitLog log : logs) {
                if (!log.undone) {
                    logCountMap.put(log.habitId,
                        logCountMap.getOrDefault(log.habitId, 0) + 1);
                    total++;
                }
            }
            ((TextView) findViewById(R.id.tvTotalCompletions))
                .setText(String.valueOf(total));

            // subtitle
            long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            long thisWeekCount = 0;
            for (HabitLog log : logs) {
                if (!log.undone && log.timestamp >= weekAgo) {
                    thisWeekCount++;
                }
            }
            ((TextView) findViewById(R.id.tvStatsSubtitle))
                .setText(thisWeekCount + " completions this week");

            updateBestHabit();
            adapter.notifyDataSetChanged();
        });
    }

    private void updateBestHabit() {
        if (habitList.isEmpty() || logCountMap.isEmpty()) return;
        Habit best = null;
        int max = 0;
        for (Habit h : habitList) {
            int count = logCountMap.getOrDefault(h.id, 0);
            if (count > max) { max = count; best = h; }
        }
        if (best != null) {
            ((TextView) findViewById(R.id.tvBestHabit)).setText(best.name);
        }
    }

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

            // completion rate based on days since created
            long daysSince = Math.max(1,
                (System.currentTimeMillis() - habit.createdAt)
                / (1000 * 60 * 60 * 24));
            int rate = (int) Math.min(100, (count / (float) daysSince) * 100);
            h.tvRate.setText(rate + "%");

            // color code the rate
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
