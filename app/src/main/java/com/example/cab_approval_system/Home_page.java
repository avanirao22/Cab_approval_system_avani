package com.example.cab_approval_system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Home_page extends AppCompatActivity {

    private TextView emp_Name, empID, empTeam;
    private DatabaseReference databaseReference, notificationRef;
    private static ImageView notificationDot; // Notification dot
    private String user_email, user_role;
    private static final String CHANNEL_ID = "1001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Get user details from intent
        user_email = getIntent().getStringExtra("email");
        user_role = getIntent().getStringExtra("userRole");

        Home_Screen.setupBottomNavigation(this, user_email,user_role);

        // Initialize UI elements
        emp_Name = findViewById(R.id.emp_name_fetch);
        empID = findViewById(R.id.emp_id_edit_text);
        empTeam = findViewById(R.id.emp_team_edit_text);
        notificationDot = findViewById(R.id.notification_dot);

        // Initialize buttons
        ImageButton request_ride = findViewById(R.id.request_ride);
        ImageButton pending_approvals = findViewById(R.id.pending_approvals);
        ImageButton cab_request = findViewById(R.id.cab_request);

        // Ensure user_role is assigned before checking visibility
        updateButtonVisibility(user_role, pending_approvals, cab_request);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
        //notification ref
        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Notification/user123");

        //instantiate notification channel
        createNotificationChannel();

        // Fetch user data & notifications
        if (user_email != null) {
            fetchUserData(user_email);
            checkForUnreadNotifications(user_email);
        } else {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
        }
        // FCM Notifications
        listenForNotifications();

        // Button click listeners
        request_ride.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Request_ride.class);
            i.putExtra("email", user_email);
            startActivity(i);
        });

        pending_approvals.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Pending_approvals.class);
            i.putExtra("email", user_email);
            i.putExtra("userRole", user_role);
            startActivity(i);
        });

        cab_request.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Cab_request.class);
            i.putExtra("email", user_email);
            startActivity(i);
        });


    }

    //new function for androidx notifications. created on 9th march
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Ride_request";
            String description = "Notification for approval of ride request";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void listenForNotifications() {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Notification");

        notificationsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    String approverEmail = snapshot.child("approver_email").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);

                    if (approverEmail != null && message != null) {
                        FCMHelper.sendFCMNotification(getApplicationContext(), approverEmail, "", "New Ride Request", message);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Notification listener failed: " + error.getMessage());
            }
        });
    }




    @Override
    protected void onResume() {
        super.onResume();
        fetchUserRoleAndUpdateUI(); // Fetch updated role and set visibility
    }

    public static ImageView getNotificationDot() {
        return notificationDot;
    }

    private void fetchUserRoleAndUpdateUI() {
        DatabaseReference userRoleRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");

        String modifiedEmail = user_email.replace(".", ",");

        userRoleRef.child(modifiedEmail).child("userRole").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                user_role = task.getResult().getValue(String.class);

                // Now update the button visibility with the latest userRole
                updateButtonVisibility(user_role, findViewById(R.id.pending_approvals), findViewById(R.id.cab_request));
            }
        });
    }


    private void updateButtonVisibility(String userRole, ImageButton pending_approvals, ImageButton cab_request) {
        if (userRole == null) return; // Prevent null exceptions

        // Handle visibility based on user role
        if ("Employee".equals(userRole)) {
            pending_approvals.setVisibility(View.GONE);
            cab_request.setVisibility(View.GONE);
        } else if ("HR Head".equals(userRole) || "FH".equals(userRole)) {
            pending_approvals.setVisibility(View.VISIBLE);
            cab_request.setVisibility(View.GONE);
        }
    }

    private void fetchUserData(String email) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DataSnapshot dataSnapshot = task.getResult();

                        if (dataSnapshot.exists()) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String empId = String.valueOf(snapshot.child("Emp ID").getValue());
                                String empName = String.valueOf(snapshot.child("Employee Name").getValue());
                                String team = String.valueOf(snapshot.child("Team").getValue());

                                empID.setText(empId);
                                emp_Name.setText(empName);
                                empTeam.setText(team);
                            }
                        } else {
                            Toast.makeText(Home_page.this, "User not found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Home_page.this, "Failed to fetch data!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkForUnreadNotifications(String approverEmail) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");

        notificationsRef.orderByChild("approver_email").equalTo(approverEmail)
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean hasUnreadNotifications = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String status = snapshot.child("status").getValue(String.class);
                            if ("pending".equals(status)) {
                                hasUnreadNotifications = true;
                                break;
                            }
                        }
                        if (notificationDot != null) {
                            notificationDot.setVisibility(hasUnreadNotifications ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                        Toast.makeText(Home_page.this, "Failed to check notifications: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
