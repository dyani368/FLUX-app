package com.example.flux.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

    private List<Habit> habits = new ArrayList<>();
    private Set<Integer> loggedTodayIds = new HashSet<>();
    private OnHabitSwipedListener swipeListener;
    private OnHabitClickListener clickListener;
    private OnCameraClickListener cameraListener;
    private OnHabitLongClickListener longClickListener;

    public interface OnHabitSwipedListener {
        void onHabitSwiped(Habit habit);
    }

    public interface OnHabitClickListener {
        void onHabitClick(Habit habit);
    }

    public interface OnCameraClickListener {
        void onCameraClick(Habit habit);
    }

    public interface OnHabitLongClickListener {
        void onHabitLongClick(Habit habit);
    }

    public HabitAdapter(OnHabitSwipedListener swipeListener,
                        OnHabitClickListener clickListener,
                        OnCameraClickListener cameraListener,
                        OnHabitLongClickListener longClickListener) {
        this.swipeListener = swipeListener;
        this.clickListener = clickListener;
        this.cameraListener = cameraListener;
        this.longClickListener = longClickListener;
    }

    public void setHabits(List<Habit> habits) {
        this.habits = habits;
        notifyDataSetChanged();
    }

    public void setLoggedTodayIds(Set<Integer> ids) {
        this.loggedTodayIds = ids;
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
        holder.tvDifficulty.setText("★".repeat(Math.max(0, habit.difficulty)));

        // status bar color + badges
        if (habit.isPaused) {
            holder.viewStatusBar.setBackgroundColor(0xFFFFB300);
            holder.tvPausedBadge.setVisibility(View.VISIBLE);
            holder.tvLoggedBadge.setVisibility(View.GONE);
        } else if (loggedTodayIds.contains(habit.id)) {
            holder.viewStatusBar.setBackgroundColor(0xFF4CAF50);
            holder.tvLoggedBadge.setVisibility(View.VISIBLE);
            holder.tvPausedBadge.setVisibility(View.GONE);
        } else {
            holder.viewStatusBar.setBackgroundColor(0xFFB8A2D1);
            holder.tvPausedBadge.setVisibility(View.GONE);
            holder.tvLoggedBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onHabitClick(habit);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onHabitLongClick(habit);
                return true;
            }
            return false;
        });

        holder.btnCamera.setOnClickListener(v -> {
            if (cameraListener != null) {
                cameraListener.onCameraClick(habit);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habits.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvDifficulty, tvPausedBadge, tvLoggedBadge;
        ImageButton btnCamera;
        View viewStatusBar;

        HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHabitName);
            tvCategory = itemView.findViewById(R.id.tvHabitCategory);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvPausedBadge = itemView.findViewById(R.id.tvPausedBadge);
            tvLoggedBadge = itemView.findViewById(R.id.tvLoggedBadge);
            btnCamera = itemView.findViewById(R.id.btnCamera);
            viewStatusBar = itemView.findViewById(R.id.viewStatusBar);
        }
    }
}
