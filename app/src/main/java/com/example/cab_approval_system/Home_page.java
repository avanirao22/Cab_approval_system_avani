package com.example.cab_approval_system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class Home_page extends AppCompatActivity {

    private TextView emp_Name, empID, empTeam;
    private DatabaseReference databaseReference;
    private ImageView notificationDot;// Add an ImageView for notification dot
    private String user_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        user_email =  getIntent().getStringExtra("email");
        Home_Screen.setupBottomNavigation(this,user_email);

        emp_Name = findViewById(R.id.emp_name_fetch);
        empID = findViewById(R.id.emp_id_edit_text);
        empTeam = findViewById(R.id.emp_team_edit_text);
        notificationDot = findViewById(R.id.notification_dot);  // Initialize the notification dot

        // Initialize buttons
        ImageButton request_ride = findViewById(R.id.request_ride);
        ImageButton pending_approvals = findViewById(R.id.pending_approvals);
        ImageButton cab_request = findViewById(R.id.cab_request);

        // Get passed data (email and role)
        Intent intent = getIntent();
        String passedEmail = intent.getStringExtra("email");
        String userRole = intent.getStringExtra("userRole");

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        // Fetch user data from the database
        if (passedEmail != null) {
            fetchUserData(passedEmail);
            checkForUnreadNotifications(passedEmail);  // Check for notifications
        } else {
            Toast.makeText(this, "No email provided", Toast.LENGTH_SHORT).show();
        }

        // Handle visibility based on user role
        if ("Employee".equals(userRole)) {
            // Only request button visible for employees
            pending_approvals.setVisibility(View.GONE);
            cab_request.setVisibility(View.GONE);
        } else if ("HR Head".equals(userRole) || "FH".equals(userRole)) {
            // Show both request and pending approvals for HR and Functional Heads
            pending_approvals.setVisibility(View.VISIBLE);
            cab_request.setVisibility(View.GONE);
        }

        // Handle button clicks
        request_ride.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Request_ride.class);
            i.putExtra("email", passedEmail);
            startActivity(i);
        });

        pending_approvals.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Pending_approvals.class);
            i.putExtra("email", passedEmail);
            startActivity(i);
        });

        cab_request.setOnClickListener(v -> {
            Intent i = new Intent(Home_page.this, Cab_request.class);
            i.putExtra("email", passedEmail);
            startActivity(i);
        });
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
                                // Retrieve user details
                                String empId = String.valueOf(snapshot.child("Emp ID").getValue());
                                String empName = String.valueOf(snapshot.child("Employee Name").getValue());
                                String team = String.valueOf(snapshot.child("Team").getValue());

                                // Set data to TextViews
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
        notificationsRef.orderByChild("approver_email").equalTo(approverEmail).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
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
                // Show/hide notification dot based on unread notifications
                notificationDot.setVisibility(hasUnreadNotifications ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                Toast.makeText(Home_page.this, "Failed to check notifications: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    }
