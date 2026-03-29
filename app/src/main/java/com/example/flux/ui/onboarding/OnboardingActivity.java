package com.example.flux.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.flux.MainActivity;
import com.example.flux.R;
import com.example.flux.data.local.Habit;
import com.example.flux.ui.ThemeManager;
import com.example.flux.ui.dashboard.HabitViewModel;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        HabitViewModel viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        // Morning Routine pack
        findViewById(R.id.btnOpt1).setOnClickListener(v -> {
            addStarterPack(viewModel, new String[][]{
                    {"Morning Run", "Fitness"},
                    {"Drink Water", "Health"},
                    {"Meditate 10 mins", "Mindfulness"},
                    {"Read 20 pages", "Learning"}
            });
            finishOnboarding();
        });

        // Burnout Recovery pack
        findViewById(R.id.btnOpt2).setOnClickListener(v -> {
            addStarterPack(viewModel, new String[][]{
                    {"Sleep by 11pm", "Health"},
                    {"No screens after 9pm", "Mindfulness"},
                    {"Short walk", "Fitness"},
                    {"Journal 5 mins", "Mindfulness"}
            });
            finishOnboarding();
        });

        // Deep Work pack
        findViewById(R.id.btnOpt3).setOnClickListener(v -> {
            addStarterPack(viewModel, new String[][]{
                    {"2hr Deep Work block", "Work"},
                    {"No social media before noon", "Work"},
                    {"Plan tomorrow", "Work"},
                    {"Review goals", "Learning"}
            });
            finishOnboarding();
        });
    }

    private void addStarterPack(HabitViewModel vm, String[][] habits) {
        for (String[] h : habits) {
            Habit habit = new Habit();
            habit.name = h[0];
            habit.category = h[1];
            habit.frequency = "daily";
            habit.difficulty = 2;
            vm.insertHabit(habit);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_done", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
