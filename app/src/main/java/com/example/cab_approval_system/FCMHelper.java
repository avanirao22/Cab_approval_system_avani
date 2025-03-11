package com.example.cab_approval_system;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.auth.oauth2.GoogleCredentials;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FCMHelper {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static void getAccessToken(Callback callback,Context context) {
        executorService.execute(() -> {
            try {
                InputStream serviceAccount = context.getAssets().open("service-account.json");
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                Log.d("oauth token",token);

                // Switch back to the main thread to return the token
                new Handler(Looper.getMainLooper()).post(() -> callback.onTokenReceived(token));

            } catch (IOException e) {
                Log.e("FCMHelper", "Failed to get OAuth Token: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> callback.onTokenReceived(null));
            }
        });
    }

    public static void sendFCMNotification(Context context, String FCM_token, String requesterEmail, String approverEmail, String title, String message,String requestedTime) {
        getAccessToken(accessToken -> {
            if (accessToken == null) {
                Log.e("FCMHelper", "❌ OAuth token is null, cannot send notification.");
                return;
            }

            String FCM_API = "https://fcm.googleapis.com/v1/projects/cab-approval-system/messages:send";
            Log.d("FCMHelper", "📨 Sending FCM notification to token: " + FCM_token);

            JSONObject payload = new JSONObject();
            JSONObject messageBody = new JSONObject();
            JSONObject notification = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject android = new JSONObject(); // 🔹 Android-specific settings
            JSONObject androidNotification = new JSONObject(); // 🔹 Android notification settings
            try {
                String fullMessage = message + "\n\nRequester: " + requesterEmail + "\nTime: " + requestedTime;

                notification.put("title", title);
                notification.put("body", fullMessage);  // 🔹 Ensure "body" is correct


                // 🔹 Android-specific notification settings (BigTextStyle)
                androidNotification.put("title", title);
                androidNotification.put("body", fullMessage);
                androidNotification.put("sound", "default"); // 🔊 Ensures notification makes a sound
                androidNotification.put("notification_priority", "PRIORITY_HIGH"); // 👈 Higher priority for visibility
                androidNotification.put("visibility", "PUBLIC");


                android.put("priority", "high");  // 🔹 Set priority to high
                android.put("notification", androidNotification); // 🔹 Attach it properly

                data.put("approverEmail", approverEmail);
                data.put("requesterEmail", requesterEmail);
                data.put("requestedTime", requestedTime);
                data.put("click_action", "LoadingPage");
                data.put("activity", "Loading_page"); // 🏷️ Used to determine which activity to open
                data.put("bigText", fullMessage);

                messageBody.put("token", FCM_token);
                messageBody.put("notification", notification);
                messageBody.put("data", data);
                messageBody.put("android", android);  // 🔹 Add the android priority setting

                payload.put("message", messageBody);

                // 🔹 Log full payload for debugging
                Log.d("FCMHelper", "📦 FCM Payload: " + payload.toString());

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, payload,
                        response -> Log.d("FCMHelper", "✅ Notification sent successfully to: " + approverEmail + " 🎉 Response: " + response.toString()),
                        error -> {
                            if (error.networkResponse != null) {
                                int statusCode = error.networkResponse.statusCode;
                                String errorMessage = new String(error.networkResponse.data);

                                Log.e("FCMHelper", "❌ Failed to send notification.");
                                Log.e("FCMHelper", "🔴 HTTP Status Code: " + statusCode);
                                Log.e("FCMHelper", "⚠️ Response Data: " + errorMessage);
                            } else {
                                Log.e("FCMHelper", "❌ Failed to send notification: Request didn't reach the server.");
                            }
                        }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "Bearer " + accessToken);
                        headers.put("Content-Type", "application/json");

                        // 🔹 Log headers for debugging
                        Log.d("FCMHelper", "📝 Request Headers: " + headers.toString());

                        return headers;
                    }
                };

                RequestQueue requestQueue = Volley.newRequestQueue(context);
                requestQueue.add(jsonObjectRequest);

            } catch (JSONException e) {
                Log.e("FCMHelper", "❌ JSON Exception: " + e.getMessage());
            }
        }, context);
    }


    public interface Callback {
        void onTokenReceived(String token);
    }
}
