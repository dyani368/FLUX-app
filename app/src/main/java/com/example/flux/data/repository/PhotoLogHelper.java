package com.example.flux.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PhotoLogHelper {

    private static final String API_KEY = "AIzaSyA2ZGS2Of_66HvSjtCuONU0Vct4iNwofhQ";
    private static final String URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-2.0-flash:generateContent?key=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient();

    public interface PhotoVerifyCallback {
        void onVerified(boolean confirmed, String message);
        void onError(String error);
    }

    public void verifyHabitPhoto(Context context, Uri imageUri,
                                  String habitName, PhotoVerifyCallback callback) {
        try {
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            // compress to reduce payload size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            String prompt = "The user is trying to log the habit: '" + habitName + "'. " +
                "Look at this photo and determine if it is evidence of completing this habit. " +
                "Reply in this exact JSON format: " +
                "{\"confirmed\": true/false, \"message\": \"one warm encouraging sentence\"}. " +
                "Be lenient and encouraging. If it's plausible evidence, confirm it.";

            JSONObject textPart = new JSONObject().put("text", prompt);
            JSONObject imagePart = new JSONObject()
                .put("inline_data", new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image));

            JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(textPart).put(imagePart));
            JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(content));

            RequestBody requestBody = RequestBody.create(
                body.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder().url(URL).post(requestBody).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Network error"));
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    try {
                        String raw = response.body().string();
                        JSONObject json = new JSONObject(raw);
                        String text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                        // strip markdown backticks if present
                        text = text.replace("```json", "").replace("```", "").trim();
                        JSONObject result = new JSONObject(text);
                        boolean confirmed = result.getBoolean("confirmed");
                        String message = result.getString("message");

                        new Handler(Looper.getMainLooper()).post(() ->
                            callback.onVerified(confirmed, message));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Parse error"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Image error: " + e.getMessage());
        }
    }
}
