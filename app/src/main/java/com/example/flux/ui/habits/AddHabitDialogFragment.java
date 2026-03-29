package com.example.flux.ui.habits;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.flux.R;
import com.example.flux.data.local.Habit;
import com.example.flux.ui.dashboard.HabitViewModel;

public class AddHabitDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_habit, null);

        EditText etName = view.findViewById(R.id.etHabitName);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        Spinner spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);

        // category options
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Health", "Learning", "Fitness", "Mindfulness", "Work", "Other"});
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        // difficulty options
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"★ Easy", "★★ Medium", "★★★ Hard", "★★★★ Intense", "★★★★★ Extreme"});
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(diffAdapter);

        HabitViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(HabitViewModel.class);

        return new AlertDialog.Builder(requireContext())
                .setTitle("New Habit")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;

                    Habit habit = new Habit();
                    habit.name = name;
                    habit.category = spinnerCategory.getSelectedItem().toString();
                    habit.difficulty = spinnerDifficulty.getSelectedItemPosition() + 1;
                    habit.frequency = "daily";
                    viewModel.insertHabit(habit);
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}