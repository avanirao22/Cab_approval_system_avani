package com.example.cab_approval_system;

import android.content.Context;
import android.util.Log;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.auth.oauth2.GoogleCredentials;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FCMHelper {

    private static String getAccessToken() {
        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            Log.e("FCMHelper", "Failed to get OAuth Token: " + e.getMessage());
            return null;
        }
    }

    public static void sendFCMNotification(Context context, String FCM_token, String approverEmail, String title, String message) {
        String FCM_API = "https://fcm.googleapis.com/v1/projects/cab-approval-system/messages:send";
        String accessToken = getAccessToken();

        if (accessToken == null) {
            Log.e("FCMHelper", "OAuth token is null, cannot send notification.");
            return;
        }

        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("message", message);

            JSONObject data = new JSONObject();
            data.put("approverEmail", approverEmail); // Including approver email in data payload

            JSONObject messageBody = new JSONObject();
            messageBody.put("token", FCM_token); // Correct key for FCM API
            messageBody.put("notification", notification);
            messageBody.put("data", data); // Custom data payload

            JSONObject payload = new JSONObject();
            payload.put("message", messageBody);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, FCM_API, payload,
                    response -> Log.d("FCMHelper", "Notification sent to: " + approverEmail + ", Response: " + response.toString()),
                    error -> Log.e("FCMHelper", "Failed to send notification: " + error.getMessage())) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + accessToken);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            Log.e("FCMHelper", "JSON Exception: " + e.getMessage());
        }
    }

}
