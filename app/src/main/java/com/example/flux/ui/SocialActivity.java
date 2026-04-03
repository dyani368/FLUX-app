package com.example.flux.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.R;
import com.example.flux.domain.model.FeedPost;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SocialActivity extends AppCompatActivity {

    private final List<FeedPost> posts = new ArrayList<>();
    private FeedAdapter adapter;
    private String currentTheme;
    private ListenerRegistration feedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentTheme = ThemeManager.getTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        RecyclerView rv = findViewById(R.id.rvFeed);
        adapter = new FeedAdapter(posts);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadFeedRealTime();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = ThemeManager.getTheme(this);
        if (!theme.equals(currentTheme)) {
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedListener != null) {
            feedListener.remove();
        }
    }

    // ── FAB: compose a manual "win" post ─────────────────────────────────────

    private void setupFab() {
        ExtendedFloatingActionButton fab = findViewById(R.id.fabShare);
        fab.setOnClickListener(v -> showComposeDialog());
    }

    private void showComposeDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be signed in to post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build dialog view
        View dialogView = LayoutInflater.from(this).inflate(
                android.R.layout.simple_list_item_2, null, false);

        // Custom layout via inline AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share a Win 🎉");

        // Simple two-field layout built programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int dp16 = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(dp16, dp16, dp16, 0);

        // Habit name or achievement label
        TextView labelHabit = new TextView(this);
        labelHabit.setText("What did you accomplish?");
        labelHabit.setTextSize(12);
        labelHabit.setPadding(0, 0, 0, (int)(4 * getResources().getDisplayMetrics().density));
        layout.addView(labelHabit);

        EditText etHabit = new EditText(this);
        etHabit.setHint("e.g. Morning run, 10 min meditation…");
        etHabit.setSingleLine(true);
        layout.addView(etHabit);

        // Optional note
        int dp8 = (int)(8 * getResources().getDisplayMetrics().density);
        TextView labelNote = new TextView(this);
        labelNote.setText("Add a note (optional)");
        labelNote.setTextSize(12);
        labelNote.setPadding(0, dp8, 0, (int)(4 * getResources().getDisplayMetrics().density));
        layout.addView(labelNote);

        EditText etNote = new EditText(this);
        etNote.setHint("How did it go?");
        etNote.setMaxLines(3);
        etNote.setMinLines(2);
        layout.addView(etNote);

        builder.setView(layout);

        builder.setPositiveButton("Post", (dialog, which) -> {
            String habit = etHabit.getText().toString().trim();
            String note  = etNote.getText().toString().trim();

            if (TextUtils.isEmpty(habit)) {
                Toast.makeText(this, "Please enter what you accomplished.", Toast.LENGTH_SHORT).show();
                return;
            }

            String verdict = TextUtils.isEmpty(note)
                    ? "Shared a manual check-in ✓"
                    : note;

            postToFeed(user.getUid(), user.getEmail(), habit, verdict, "manual");
            Toast.makeText(this, "Posted! Keep it up 💪", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ── Real-time feed loader ─────────────────────────────────────────────────

    private void loadFeedRealTime() {
        TextView tvEmpty = findViewById(R.id.tvEmptyFeed);

        feedListener = FirebaseFirestore.getInstance()
            .collection("community_feed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null || snapshot == null) return;

                posts.clear();
                for (var doc : snapshot.getDocuments()) {
                    FeedPost post = doc.toObject(FeedPost.class);
                    if (post != null) posts.add(post);
                }
                adapter.notifyDataSetChanged();

                if (tvEmpty != null) {
                    tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
    }

    // ── Static helper — called by MainActivity after AI photo verification ────

    /** Posts an AI-verified completion to the shared community feed. */
    public static void postToFeed(String userId, String userEmail,
                                   String habitName, String aiVerdict) {
        postToFeed(userId, userEmail, habitName, aiVerdict, "ai_verified");
    }

    /** Posts to the community feed with an explicit post type. */
    public static void postToFeed(String userId, String userEmail,
                                   String habitName, String aiVerdict, String postType) {
        FeedPost post = new FeedPost();
        post.userId    = userId;
        post.userEmail = maskEmail(userEmail);
        post.habitName = habitName;
        post.aiVerdict = aiVerdict;
        post.timestamp = System.currentTimeMillis();
        post.postType  = postType;

        FirebaseFirestore.getInstance()
            .collection("community_feed")
            .add(post);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "anonymous";
        String[] parts = email.split("@");
        String name = parts[0];
        return name.substring(0, Math.min(3, name.length())) + "***@" + parts[1];
    }

    // ── FeedAdapter ───────────────────────────────────────────────────────────

    static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.VH> {
        private final List<FeedPost> posts;
        FeedAdapter(List<FeedPost> posts) { this.posts = posts; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            FeedPost p = posts.get(position);
            h.tvEmail.setText(p.userEmail != null ? p.userEmail : "someone");

            // Show badge for AI-verified vs manual
            boolean isAI = "ai_verified".equals(p.postType);
            String badge = isAI ? "📸 " : "✓ ";
            h.tvHabit.setText(badge + (p.habitName != null ? p.habitName : "a habit"));

            h.tvVerdict.setText(p.aiVerdict != null ? p.aiVerdict : "");

            String time = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(new Date(p.timestamp));
            h.tvTime.setText(time);
        }

        @Override public int getItemCount() { return posts.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmail, tvHabit, tvVerdict, tvTime;
            VH(View v) {
                super(v);
                tvEmail   = v.findViewById(R.id.tvUserEmail);
                tvHabit   = v.findViewById(R.id.tvHabitName);
                tvVerdict = v.findViewById(R.id.tvAiVerdict);
                tvTime    = v.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
