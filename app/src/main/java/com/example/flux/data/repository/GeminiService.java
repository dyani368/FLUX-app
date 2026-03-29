package com.example.flux.data.repository;

import android.os.Handler;
import android.os.Looper;

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

    private static final String API_KEY = "AIzaSyA2ZGS2Of_66HvSjtCuONU0Vct4iNwofhQ";
    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash:generateContent?key=" + API_KEY;

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
                " habits this week. Their average sleep was " + avgSleep + " hours. " +
                "Their burnout index is " + (int) burnoutScore + " out of 100. " +
                "Habits they struggled with: " + missedHabits + ". " +
                "Give a personalised, motivating 3-sentence behavioural insight. " +
                "Be specific to their data. Be warm, not preachy.";

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
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
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
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError("Parse error"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Request build error");
        }
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

        try {
            JSONObject part = new JSONObject().put("text", prompt);
            JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(part));
            JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

            RequestBody requestBody = RequestBody.create(
                body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder().url(URL).post(requestBody).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Network error"));
                }
                @Override public void onResponse(Call call, Response response)
                        throws java.io.IOException {
                    try {
                        String raw = response.body().string();
                        JSONObject json = new JSONObject(raw);
                        String text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                        new Handler(Looper.getMainLooper()).post(() ->
                            callback.onResult(text));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Parse error"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Request error");
        }
    }
}
