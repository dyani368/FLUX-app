package com.example.flux.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.R;
import com.example.flux.data.local.Habit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    public interface HabitClickListener {
        void onHabitClick(Habit habit);
    }

    public interface HabitLongClickListener {
        void onHabitLongClick(Habit habit);
    }

    public interface HabitPhotoListener {
        void onPhotoClick(Habit habit);
    }

    // Filter constants — match what the chips set
    public static final String FILTER_ALL       = "all";
    public static final String FILTER_MORNING   = "Morning";
    public static final String FILTER_AFTERNOON = "Afternoon";
    public static final String FILTER_EVENING   = "Evening";

    private List<Habit> allHabits   = new ArrayList<>();  // full unfiltered list
    private List<Habit> habits      = new ArrayList<>();  // currently displayed
    private Set<Integer> loggedTodayIds = new HashSet<>();
    private final HabitClickListener clickListener;
    private final HabitClickListener statsListener;
    private final HabitPhotoListener photoListener;
    private final HabitLongClickListener longClickListener;

    private boolean minimalMode = false;
    private String currentFilter = FILTER_ALL;

    public HabitAdapter(HabitClickListener clickListener,
                        HabitClickListener statsListener,
                        HabitPhotoListener photoListener,
                        HabitLongClickListener longClickListener) {
        this.clickListener     = clickListener;
        this.statsListener     = statsListener;
        this.photoListener     = photoListener;
        this.longClickListener = longClickListener;
    }

    /** Called by ViewModel observer with the full (unfiltered) list. */
    public void setHabits(List<Habit> habits) {
        this.allHabits = habits != null ? habits : new ArrayList<>();
        applyFilter();
    }

    /** Called by chip buttons. filter is FILTER_ALL / FILTER_MORNING / etc. */
    public void setFilter(String filter) {
        this.currentFilter = filter;
        applyFilter();
    }

    private void applyFilter() {
        if (currentFilter == null || currentFilter.equals(FILTER_ALL)) {
            habits = new ArrayList<>(allHabits);
        } else {
            habits = new ArrayList<>();
            for (Habit h : allHabits) {
                // Match against the habit's frequency field (e.g. "Morning", "Afternoon", "Evening")
                // or against category if frequency isn't set
                String freq = h.frequency != null ? h.frequency : "";
                String cat  = h.category  != null ? h.category  : "";
                if (freq.equalsIgnoreCase(currentFilter) || cat.equalsIgnoreCase(currentFilter)) {
                    habits.add(h);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setLoggedTodayIds(Set<Integer> loggedIds) {
        this.loggedTodayIds = loggedIds;
        notifyDataSetChanged();
    }

    public void setMinimalMode(boolean minimal) {
        this.minimalMode = minimal;
        notifyDataSetChanged();
    }

    public Habit getHabitAt(int position) {
        return habits.get(position);
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habits.get(position);
        holder.tvName.setText(habit.name);
        holder.tvCategory.setText(habit.category);

        String diffText = "";
        for (int i = 0; i < habit.difficulty; i++) diffText += "★";
        holder.tvDifficulty.setText(diffText);

        boolean isLogged = loggedTodayIds.contains(habit.id);
        holder.viewStatusBar.setBackgroundColor(isLogged ? 0xFF4CAF50 : 0xFF333333);

        if (habit.isPaused) {
            holder.itemView.setAlpha(0.5f);
            holder.tvCategory.setText(habit.category + " (Paused)");
            holder.tvPausedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.tvPausedBadge.setVisibility(View.GONE);
        }

        holder.tvLoggedBadge.setVisibility(isLogged ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> clickListener.onHabitClick(habit));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onHabitLongClick(habit);
            return true;
        });

        holder.btnStats.setOnClickListener(v -> statsListener.onHabitClick(habit));
        holder.btnCamera.setOnClickListener(v -> photoListener.onPhotoClick(habit));

        // Minimal Mode Overrides
        if (minimalMode) {
            holder.viewStatusBar.setBackgroundColor(0xFF333333);
            holder.tvName.setTextColor(0xFFFFFFFF);
            holder.tvCategory.setTextColor(0xFF555555);
            holder.tvDifficulty.setTextColor(0xFF444444);
            holder.btnCamera.setVisibility(View.GONE);
            holder.btnStats.setVisibility(View.GONE);
        } else {
            holder.tvName.setTextColor(0xFFFFFFFF);
            holder.tvCategory.setTextColor(0xFF888888);
            holder.tvDifficulty.setTextColor(0xFFB8A2D1);
            holder.btnCamera.setVisibility(View.VISIBLE);
            holder.btnStats.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvDifficulty, tvPausedBadge, tvLoggedBadge;
        View viewStatusBar, btnStats, btnCamera;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tvHabitName);
            tvCategory    = itemView.findViewById(R.id.tvHabitCategory);
            tvDifficulty  = itemView.findViewById(R.id.tvDifficulty);
            tvPausedBadge = itemView.findViewById(R.id.tvPausedBadge);
            tvLoggedBadge = itemView.findViewById(R.id.tvLoggedBadge);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
            btnStats      = itemView.findViewById(R.id.btnHabitStats);
            btnCamera     = itemView.findViewById(R.id.btnHabitPhoto);
        }
    }
}
