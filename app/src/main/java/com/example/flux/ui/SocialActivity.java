package com.example.flux.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flux.R;
import com.example.flux.domain.model.FeedPost;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SocialActivity extends AppCompatActivity {

    private List<FeedPost> posts = new ArrayList<>();
    private FeedAdapter adapter;
    private String currentTheme;

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

        seedDemoPosts();
    }

    private void seedDemoPosts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // check if already seeded
        db.collection("community_feed").limit(1).get()
            .addOnSuccessListener(snapshot -> {
                if (!snapshot.isEmpty()) {
                    loadFeed();
                    return; // already has posts
                }

                String[][] demos = {
                    {"Morning Run", "Confirmed! That looks like a great outdoor run. Keep it up!"},
                    {"Drink Water", "Verified — staying hydrated is one of the best things you can do."},
                    {"Meditate 10 mins", "Confirmed! A peaceful space and a calm mind — well done."},
                    {"Read 20 pages", "Verified — looks like a great reading session!"},
                    {"Deep Work Block", "Confirmed! Focused work environment detected. Impressive."},
                    {"Morning Run", "Verified — early morning energy is unmatched!"},
                    {"Journal 5 mins", "Confirmed! Reflection is a powerful habit. Great work."}
                };

                String[] users = {
                    "rat***@gmail.com",
                    "har***@gmail.com",
                    "ksh***@gmail.com",
                    "ana***@gmail.com",
                    "pri***@gmail.com"
                };

                long now = System.currentTimeMillis();
                long hour = 60 * 60 * 1000L;

                for (int i = 0; i < demos.length; i++) {
                    FeedPost post = new FeedPost();
                    post.habitName = demos[i][0];
                    post.aiVerdict = demos[i][1];
                    post.userEmail = users[i % users.length];
                    post.userId = "demo_" + i;
                    post.timestamp = now - (i * 2 * hour); // spread over past hours
                    db.collection("community_feed").add(post);
                }
                loadFeed();
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = ThemeManager.getTheme(this);
        if (!theme.equals(currentTheme)) {
            recreate();
        }
    }

    private void loadFeed() {
        FirebaseFirestore.getInstance()
            .collection("community_feed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener(snapshot -> {
                posts.clear();
                for (var doc : snapshot.getDocuments()) {
                    FeedPost post = doc.toObject(FeedPost.class);
                    if (post != null) posts.add(post);
                }
                adapter.notifyDataSetChanged();
            });
    }

    // post to community feed (call this after AI verifies a photo)
    public static void postToFeed(String userId, String userEmail,
                                   String habitName, String aiVerdict) {
        FeedPost post = new FeedPost();
        post.userId = userId;
        post.userEmail = maskEmail(userEmail);
        post.habitName = habitName;
        post.aiVerdict = aiVerdict;
        post.timestamp = System.currentTimeMillis();

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
            h.tvEmail.setText(p.userEmail);
            h.tvHabit.setText("✓ " + p.habitName);
            h.tvVerdict.setText(p.aiVerdict);
            String time = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(new Date(p.timestamp));
            h.tvTime.setText(time);
        }

        @Override public int getItemCount() { return posts.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmail, tvHabit, tvVerdict, tvTime;
            VH(View v) {
                super(v);
                tvEmail  = v.findViewById(R.id.tvUserEmail);
                tvHabit  = v.findViewById(R.id.tvHabitName);
                tvVerdict = v.findViewById(R.id.tvAiVerdict);
                tvTime   = v.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
