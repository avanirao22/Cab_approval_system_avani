package com.example.cab_approval_system;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class FCMListener extends FirebaseMessagingService {
    private static final String TAG = "FCMListener";
    private static final String CHANNEL_ID = "cab_approval_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Data Payload: " + remoteMessage.getData());

            // Safely retrieve notification data
            String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "New Notification";
            String message = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "";
            String bigText = remoteMessage.getData().get("bigText"); // 👈 Retrieve BigText from data payload

            // Fallback to message if bigText is missing
            if (bigText == null || bigText.isEmpty()) {
                bigText = message;
            }


            Intent intent;
                intent = new Intent(this, Loading_page.class); // 🏷️ Open target activity


            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.supracabs_monochrome)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText)); // 👈 Ensure BigTextStyle always has valid text

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build()); // 🆗 ID 1 means it'll update the same notification
        }
    }



    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cab Approvals", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.supracabs_monochrome)  // Use your app's icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(0, builder.build());
    }
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "🔄 New FCM Token: " + token);

        // ✅ Update the token in Firebase Database
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        String email = getCurrentUserEmail(); // Implement this method
        if (email != null) {
            String emailKey = email.replace(".", ","); // Firebase doesn't allow dots in keys
            usersRef.child(emailKey).child("fcm_token").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Token updated successfully!"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update token: " + e.getMessage()));
        }
    }

    // Helper method to get the current user's email
    private String getCurrentUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getEmail() : null;
    }


}
