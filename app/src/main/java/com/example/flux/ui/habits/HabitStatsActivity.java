package com.example.flux.ui.habits;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.R;
import com.example.flux.ui.ThemeManager;
import com.example.flux.ui.dashboard.HabitViewModel;

public class HabitStatsActivity extends AppCompatActivity {

    public static final String EXTRA_HABIT_ID = "habit_id";
    public static final String EXTRA_HABIT_NAME = "habit_name";
    public static final String EXTRA_HABIT_CATEGORY = "habit_category";

    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentTheme = ThemeManager.getTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_stats);

        int habitId = getIntent().getIntExtra(EXTRA_HABIT_ID, -1);
        String habitName = getIntent().getStringExtra(EXTRA_HABIT_NAME);
        String habitCategory = getIntent().getStringExtra(EXTRA_HABIT_CATEGORY);

        ((TextView) findViewById(R.id.tvHabitTitle)).setText(habitName);
        ((TextView) findViewById(R.id.tvCategory)).setText(habitCategory);

        HabitViewModel viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        viewModel.getLogsForHabit(habitId).observe(this, logs -> {
            int total = logs.size();
            ((TextView) findViewById(R.id.tvTotalLogs)).setText(String.valueOf(total));

            // completion rate = logs in last 7 days / 7
            long weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
            long recentCount = logs.stream()
                    .filter(l -> l.timestamp >= weekAgo)
                    .count();
            int rate = (int)((recentCount / 7.0) * 100);
            ((TextView) findViewById(R.id.tvCompletionRate)).setText(rate + "%");
        });

        RecyclerView rv = findViewById(R.id.rvLogs);
        rv.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = ThemeManager.getTheme(this);
        if (!theme.equals(currentTheme)) {
            recreate();
        }
    }
}
