package com.example.flux.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.flux.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PhotoLogHelper {

    private static final String TAG = "PhotoLogHelper";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    
    // Using v1beta and gemini-2.5-flash for consistent compatibility (and avoiding 429 errors on 2.0)
    private static final String URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PhotoVerifyCallback {
        void onVerified(boolean confirmed, String message);
        void onError(String error);
    }

    public void verifyHabitPhoto(Context context, Uri imageUri,
                                  String habitName, PhotoVerifyCallback callback) {
        executor.execute(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(imageUri);
                if (is == null) {
                    mainHandler.post(() -> callback.onError("Cannot open image"));
                    return;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                is.close();

                if (bitmap == null) {
                    mainHandler.post(() -> callback.onError("Failed to decode image"));
                    return;
                }

                Bitmap scaled = scaleBitmap(bitmap, 800);
                bitmap.recycle();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                scaled.recycle();

                byte[] bytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

                JSONObject textPart = new JSONObject().put("text",
                    "The user is trying to log the habit: '" + habitName + "'. " +
                    "Look at this photo and determine if it is plausible evidence of completing this habit. " +
                    "Reply ONLY in this exact JSON format with no markdown: " +
                    "{\"confirmed\": true, \"message\": \"one warm encouraging sentence\"} " +
                    "or {\"confirmed\": false, \"message\": \"one supportive sentence\"}. " +
                    "Be lenient and encouraging.");

                JSONObject inlineData = new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image);
                JSONObject imagePart = new JSONObject().put("inline_data", inlineData);

                JSONArray parts = new JSONArray().put(textPart).put(imagePart);
                JSONObject content = new JSONObject().put("parts", parts);
                JSONArray contents = new JSONArray().put(content);

                JSONObject genConfig = new JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 150);

                JSONObject body = new JSONObject()
                    .put("contents", contents)
                    .put("generationConfig", genConfig);

                RequestBody requestBody = RequestBody.create(
                    body.toString(), MediaType.parse("application/json"));

                Request request = new Request.Builder().url(URL).post(requestBody).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> callback.onError("Network error"));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String raw = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "API Error " + response.code() + ": " + raw);
                            mainHandler.post(() -> callback.onError("AI Error " + response.code()));
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(raw);
                            String text = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                            
                            int jsonStart = text.indexOf('{');
                            int jsonEnd = text.lastIndexOf('}');
                            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                text = text.substring(jsonStart, jsonEnd + 1);
                            }

                            JSONObject result = new JSONObject(text);
                            boolean confirmed = result.getBoolean("confirmed");
                            String message = result.getString("message");

                            mainHandler.post(() -> callback.onVerified(confirmed, message));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onVerified(true, "Habit logged!"));
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Processing error"));
            }
        });
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;
        float scale = maxSize / (float) Math.max(w, h);
        return Bitmap.createScaledBitmap(src, Math.round(w * scale), Math.round(h * scale), true);
    }
}
