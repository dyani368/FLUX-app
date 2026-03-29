package com.example.flux;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.data.local.Habit;
import com.example.flux.data.local.HabitLog;
import com.example.flux.data.repository.GeminiService;
import com.example.flux.data.repository.PhotoLogHelper;
import com.example.flux.ui.AllStatsActivity;
import com.example.flux.ui.NotificationHelper;
import com.example.flux.ui.SettingsActivity;
import com.example.flux.ui.SocialActivity;
import com.example.flux.ui.ThemeManager;
import com.example.flux.ui.dashboard.HabitAdapter;
import com.example.flux.ui.dashboard.HabitViewModel;
import com.example.flux.ui.dashboard.HeatmapView;
import com.example.flux.ui.dashboard.ParticleView;
import com.example.flux.ui.habits.AddHabitDialogFragment;
import com.example.flux.ui.habits.HabitStatsActivity;
import com.example.flux.ui.sleep.SleepActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private Habit pendingPhotoHabit;
    private Uri pendingPhotoUri;
    private PhotoLogHelper photoLogHelper = new PhotoLogHelper();

    private HabitViewModel viewModel;
    private HabitAdapter adapter;
    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentTheme = ThemeManager.getTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        setupGreeting();
        setupRecyclerView();
        setupObservers();
        applyMinimalMode();
        setupGeminiInsight();
        setupAddButton();
        setupBottomNav();
        loadDailyTip();

        findViewById(R.id.btnSettingsShortcut).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        NotificationHelper.createChannel(this);
        SharedPreferences prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("reminders_on", true)) {
            NotificationHelper.scheduleDailyReminder(this, 20, 0); // 8pm default
        }
    }

    private void applyMinimalMode() {
        SharedPreferences prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);
        boolean minimal = prefs.getBoolean("minimal_mode", false);

        int visible = minimal ? View.GONE : View.VISIBLE;

        // hide these in minimal mode
        findViewById(R.id.cardAiInsight).setVisibility(visible);
        findViewById(R.id.cardDailyTip).setVisibility(visible);

        // hide momentum row inside burnout card
        findViewById(R.id.tvMomentum).setVisibility(visible);

        // hide bottom nav community tab
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.getMenu().findItem(R.id.nav_social).setVisible(!minimal);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = ThemeManager.getTheme(this);
        if (!theme.equals(currentTheme)) {
            recreate();
        }
    }

    private void setupGreeting() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        TextView tvDate = findViewById(R.id.tvDate);

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 12) tvGreeting.setText("Good morning");
        else if (hour < 17) tvGreeting.setText("Good afternoon");
        else tvGreeting.setText("Good evening");

        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(new Date());

        // show user email if logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String subtitle = date;
        if (user != null && user.getEmail() != null) {
            subtitle = date + "  ·  " + user.getEmail().split("@")[0];
        }
        tvDate.setText(subtitle);
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvHabits);
        adapter = new HabitAdapter(
                habit -> {
                    viewModel.logHabit(habit.id);
                    Snackbar.make(rv, habit.name + " logged!", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> { /* undo logic */ })
                            .show();
                },
                habit -> {
                    Intent intent = new Intent(this, HabitStatsActivity.class);
                    intent.putExtra(HabitStatsActivity.EXTRA_HABIT_ID, habit.id);
                    intent.putExtra(HabitStatsActivity.EXTRA_HABIT_NAME, habit.name);
                    intent.putExtra(HabitStatsActivity.EXTRA_HABIT_CATEGORY, habit.category);
                    startActivity(intent);
                },
                habit -> {
                    pendingPhotoHabit = habit;
                    openCamera();
                },
                habit -> {
                    // long press = pause/resume
                    viewModel.togglePause(habit);
                    String msg = habit.isPaused
                            ? habit.name + " paused"
                            : habit.name + " resumed";
                    Snackbar.make(findViewById(R.id.rvHabits), msg, Snackbar.LENGTH_SHORT).show();
                }
        );
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Swipe to log or delete
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        Habit habit = adapter.getHabitAt(position);

                        if (direction == ItemTouchHelper.RIGHT) {
                            // log the habit
                            viewModel.logHabit(habit.id);
                            adapter.notifyItemChanged(position);

                            SharedPreferences prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);
                            if (prefs.getBoolean("animations_on", true)) {
                                ParticleView pv = findViewById(R.id.particleView);
                                pv.burst(viewHolder.itemView.getX() + viewHolder.itemView.getWidth() / 2f,
                                         viewHolder.itemView.getY() + viewHolder.itemView.getHeight() / 2f);
                            }

                            Snackbar.make(findViewById(R.id.rvHabits),
                                            habit.name + " logged! ✓", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> {})
                                    .show();
                        } else {
                            // delete the habit
                            viewModel.deleteHabit(habit);
                            Snackbar.make(findViewById(R.id.rvHabits),
                                            habit.name + " deleted", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> viewModel.insertHabit(habit))
                                    .show();
                        }
                    }
                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rv);
    }

    private void openCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = File.createTempFile(
                        "flux_proof_", ".jpg", getCacheDir());
                pendingPhotoUri = FileProvider.getUriForFile(
                        this, getPackageName() + ".provider", photoFile);
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, pendingPhotoUri);
                startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (pendingPhotoHabit != null && pendingPhotoUri != null) {
                verifyPhotoWithAI(pendingPhotoHabit, pendingPhotoUri);
            }
        }
    }

    private void verifyPhotoWithAI(Habit habit, Uri photoUri) {
        // show loading snackbar
        Snackbar loading = Snackbar.make(findViewById(R.id.rvHabits),
                "🔍 AI is verifying your photo...", Snackbar.LENGTH_INDEFINITE);
        loading.show();

        photoLogHelper.verifyHabitPhoto(this, photoUri, habit.name,
                new PhotoLogHelper.PhotoVerifyCallback() {
                    @Override
                    public void onVerified(boolean confirmed, String message) {
                        loading.dismiss();
                        if (confirmed) {
                            viewModel.logHabit(habit.id);

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                SocialActivity.postToFeed(
                                    user.getUid(), user.getEmail(), habit.name, message);
                            }
                            
                            SharedPreferences prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);
                            if (prefs.getBoolean("animations_on", true)) {
                                ParticleView pv = findViewById(R.id.particleView);
                                pv.burst(getWindow().getDecorView().getWidth() / 2f,
                                         getWindow().getDecorView().getHeight() / 2f);
                            }
                            
                            Snackbar.make(findViewById(R.id.rvHabits),
                                    "✓ " + message, Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar.make(findViewById(R.id.rvHabits),
                                    "❌ " + message, Snackbar.LENGTH_LONG)
                                    .setAction("Log Anyway", v -> viewModel.logHabit(habit.id))
                                    .show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        loading.dismiss();
                        Snackbar.make(findViewById(R.id.rvHabits),
                                "Couldn't verify photo, logging anyway",
                                Snackbar.LENGTH_SHORT).show();
                        viewModel.logHabit(habit.id);
                    }
                });
    }

    private void setupObservers() {
        viewModel.getAllHabits().observe(this, habits -> adapter.setHabits(habits));

        viewModel.getTodayLogs(viewModel.getStartOfToday()).observe(this, logs -> {
            Set<Integer> loggedIds = new HashSet<>();
            for (HabitLog log : logs) {
                if (!log.undone) loggedIds.add(log.habitId);
            }
            adapter.setLoggedTodayIds(loggedIds);
        });

        viewModel.getAllLogs().observe(this, logs -> {
            HeatmapView heatmap = findViewById(R.id.heatmapView);
            Map<Integer, Integer> completions = new HashMap<>();
            Map<Integer, Float> sleep = new HashMap<>();

            Calendar cal = Calendar.getInstance();
            // get current month boundaries
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long monthStart = cal.getTimeInMillis();

            for (HabitLog log : logs) {
                if (log.timestamp >= monthStart) {
                    Calendar logCal = Calendar.getInstance();
                    logCal.setTimeInMillis(log.timestamp);
                    int day = logCal.get(Calendar.DAY_OF_MONTH);
                    completions.put(day, completions.getOrDefault(day, 0) + 1);
                }
            }

            int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
            heatmap.setData(completions, sleep, daysInMonth);
        });

        viewModel.getBurnoutIndex().observe(this, burnout -> {
            TextView tvStatus = findViewById(R.id.tvBurnoutStatus);
            android.widget.ProgressBar pb = findViewById(R.id.burnoutProgress);
            pb.setProgress(burnout.intValue());

            if (burnout < 33) {
                tvStatus.setText("Normal");
                tvStatus.setTextColor(0xFF4CAF50);
            } else if (burnout < 66) {
                tvStatus.setText("Strained");
                tvStatus.setTextColor(0xFFFFB300);
            } else {
                tvStatus.setText("At Risk");
                tvStatus.setTextColor(0xFFE53935);
            }
        });

        viewModel.getMomentumScore().observe(this, momentum -> {
            TextView tvMomentum = findViewById(R.id.tvMomentum);
            TextView skeleton = findViewById(R.id.tvMomentumSkeleton);

            if (skeleton != null) {
                skeleton.setVisibility(View.GONE);
            }
            if (tvMomentum != null) {
                tvMomentum.setVisibility(View.VISIBLE);
                tvMomentum.setText(String.format(Locale.getDefault(), "%.0f / 100", momentum));
            }
        });
    }

    private void setupGeminiInsight() {
        GeminiService gemini = new GeminiService();
        TextView tvInsight = findViewById(R.id.tvAiInsight);

        findViewById(R.id.btnGetInsight).setOnClickListener(v -> {
            tvInsight.setText("Analysing your week...");

            float burnout = viewModel.getBurnoutIndex().getValue() != null
                    ? viewModel.getBurnoutIndex().getValue() : 0f;
            float momentum = viewModel.getMomentumScore().getValue() != null
                    ? viewModel.getMomentumScore().getValue() : 100f;

            gemini.getWeeklyInsight(
                    5, 7,       // completed, total habits
                    6.5f,       // avg sleep
                    burnout,
                    "workout, reading",
                    new GeminiService.GeminiCallback() {
                        @Override public void onResult(String insight) {
                            tvInsight.setText(insight);
                        }
                        @Override public void onError(String error) {
                            tvInsight.setText("Couldn't generate insight. Try again.");
                        }
                    });
        });
    }

    private void setupAddButton() {
        findViewById(R.id.btnAddHabit).setOnClickListener(v -> {
            new AddHabitDialogFragment()
                    .show(getSupportFragmentManager(), "add_habit");
        });

        findViewById(R.id.fabSleep).setOnClickListener(v -> {
            startActivity(new Intent(this, SleepActivity.class));
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_sleep) {
                startActivity(new Intent(this, SleepActivity.class));
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, AllStatsActivity.class));
            } else if (id == R.id.nav_social) {
                startActivity(new Intent(this, SocialActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            return true;
        });
    }

    private void loadDailyTip() {
        TextView tvTip = findViewById(R.id.tvDailyTip);
        GeminiService gemini = new GeminiService();

        float momentum = viewModel.getMomentumScore().getValue() != null
                ? viewModel.getMomentumScore().getValue() : 100f;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeOfDay = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";

        gemini.getDailyTip(6.5f, momentum, "workout", timeOfDay,
                new GeminiService.GeminiCallback() {
                    @Override public void onResult(String tip) { tvTip.setText(tip); }
                    @Override public void onError(String e) {
                        tvTip.setText("Rest well tonight — consistency beats intensity every time.");
                    }
                });
    }
}
