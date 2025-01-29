package com.example.cab_approval_system;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Approver_dashboard extends AppCompatActivity {

    private ImageButton pendingRequestsButton;
    private View notificationDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approver_dashboard);

        pendingRequestsButton = findViewById(R.id.pending_requests_button);
        notificationDot = findViewById(R.id.notification_dot);

        // Get the logged-in approver's email from shared preferences or intent
        String approverEmail = getIntent().getStringExtra("approver_email");
        if (approverEmail == null) {
            Toast.makeText(this, "Approver email not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for unread notifications
        checkForUnreadNotifications(approverEmail);

        // Handle pending requests button click
        pendingRequestsButton.setOnClickListener(v -> {
            // Navigate to the pending requests screen or handle it as needed
            Toast.makeText(Approver_dashboard.this, "Opening pending requests...", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkForUnreadNotifications(String approverEmail) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        notificationsRef.orderByChild("approver_id").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasUnreadNotifications = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("unread".equals(status)) {
                        hasUnreadNotifications = true;
                        // Mark notification as read (optional, depending on your use case)
                        String notificationId = snapshot.getKey();
                        markNotificationAsRead(notificationId);
                        break;
                    }
                }
                // Show/hide notification dot based on unread notifications
                notificationDot.setVisibility(hasUnreadNotifications ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Approver_dashboard.this, "Failed to check notifications: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markNotificationAsRead(String notificationId) {
        if (notificationId != null) {
            DatabaseReference notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Notification").child(notificationId);
            notificationRef.child("status").setValue("read")
                    .addOnSuccessListener(aVoid -> {
                        // Optional: You could show a toast or perform other UI updates here
                        Toast.makeText(Approver_dashboard.this, "Notification marked as read", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(Approver_dashboard.this, "Failed to mark notification as read", Toast.LENGTH_SHORT).show());
        }
    }
}
