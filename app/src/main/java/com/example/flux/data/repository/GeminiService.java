package com.example.flux.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.flux.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {

    private static final String TAG = "GeminiService";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    
    // Updated to gemini-2.0-flash with v1beta endpoint
    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient();

    public interface GeminiCallback {
        void onResult(String insight);
        void onError(String error);
    }

    public void getWeeklyInsight(int completedHabits, int totalHabits,
                                 float avgSleep, float burnoutScore,
                                 String missedHabits, GeminiCallback callback) {
        String prompt = "You are a warm, supportive habit coach. " +
                "The user completed " + completedHabits + " out of " + totalHabits +
                " habit instances this week. Their average sleep was " + avgSleep + " hours. " +
                "Their burnout index is " + (int) burnoutScore + " out of 100. " +
                "Habits they struggled with: " + missedHabits + ". " +
                "Give a personalised, motivating 3-sentence behavioural insight. " +
                "Be specific to their data. Be warm, not preachy.";

        sendRequest(prompt, callback);
    }

    public void getDailyTip(float avgSleep, float momentumScore,
                             String weakestHabit, String bestTimeOfDay,
                             GeminiCallback callback) {
        String prompt = "You are a calm, insightful habit coach. " +
            "User data: average sleep = " + avgSleep + " hours, " +
            "momentum score = " + (int) momentumScore + "/100, " +
            "habit they struggle with most: " + weakestHabit + ", " +
            "they tend to be most productive in the: " + bestTimeOfDay + ". " +
            "Give ONE specific, actionable tip for today. " +
            "Maximum 2 sentences. Warm but direct. No generic advice.";

        sendRequest(prompt, callback);
    }

    private void sendRequest(String prompt, GeminiCallback callback) {
        try {
            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject content = new JSONObject()
                    .put("parts", new JSONArray().put(part));
            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(content));

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error", e);
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Network error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API Error Code: " + response.code());
                        Log.e(TAG, "API Response: " + responseBody);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("AI Error " + response.code()));
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String text = json
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");
                        
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onResult(text));
                    } catch (Exception e) {
                        Log.e(TAG, "JSON Parse Error", e);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Invalid response format"));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Request Error", e);
            callback.onError("Request failed");
        }
    }
}
