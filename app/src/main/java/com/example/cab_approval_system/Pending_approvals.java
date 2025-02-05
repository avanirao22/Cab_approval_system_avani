package com.example.cab_approval_system;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Pending_approvals extends AppCompatActivity {

    private TextView emp_name, emp_email, emp_id, emp_pick_up, emp_drop_off, emp_date, emp_distance, emp_project, emp_time, emp_status;
    private Chip approve_chip;
    private DatabaseReference requestRef, notificationRef, sheetRef;
    private String requesterEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approvals);

        emp_name = findViewById(R.id.name_textview);
        emp_email = findViewById(R.id.emp_emailid_textview);
        emp_id = findViewById(R.id.emp_id_textview);
        emp_pick_up = findViewById(R.id.source_textview);
        emp_drop_off = findViewById(R.id.destination_textview);
        emp_date = findViewById(R.id.date_textview);
        emp_distance = findViewById(R.id.distance_textview);
        emp_project = findViewById(R.id.project_textview);
        emp_time = findViewById(R.id.time_textview);
        emp_status = findViewById(R.id.status_textview);
        approve_chip = findViewById(R.id.approve_chip);

        requestRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Request_details");
        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        fetchLatestPendingRequest();

        approve_chip.setOnClickListener(v -> approveRequest(requesterEmail));
    }

    private void approveRequest(String email) {
        if (email != null) {
            HashMap<String, Object> updateStatus = new HashMap<>();
            updateStatus.put("status", "Approved ✅");

            notificationRef.orderByChild("requester_email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                requestSnapshot.getRef().updateChildren(updateStatus)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(Pending_approvals.this, "Request approved successfully.", Toast.LENGTH_SHORT).show();
                                                emp_status.setText("Approved ✅");
                                                approve_chip.setVisibility(View.GONE);
                                                emp_status.setVisibility(View.VISIBLE);
                                            } else {
                                                Toast.makeText(Pending_approvals.this, "Failed to approve request.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(Pending_approvals.this, "Error updating status.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void fetchLatestPendingRequest() {
        notificationRef.orderByChild("status").equalTo("pending")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                requesterEmail = requestSnapshot.child("requester_email").getValue(String.class);
                                Log.d("PendingApprovals", "Requester Email: " + requesterEmail);
                                if (requesterEmail != null) {
                                    fetchRequestDetails(requesterEmail);
                                    fetchEmployeeDetails(requesterEmail);
                                }
                            }
                        } else {
                            Toast.makeText(Pending_approvals.this, "No pending requests found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Error fetching pending requests.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchRequestDetails(String email) {
        requestRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                                Log.d("PendingApprovals", "Request details found");
                                emp_pick_up.setText(requestSnapshot.child("pickupLocation").getValue(String.class));
                                emp_drop_off.setText(requestSnapshot.child("dropoffLocation").getValue(String.class));
                                emp_date.setText(requestSnapshot.child("date").getValue(String.class));
                                emp_distance.setText(requestSnapshot.child("distanceObtained").getValue(String.class));
                                emp_project.setText(requestSnapshot.child("project").getValue(String.class));
                                emp_time.setText(requestSnapshot.child("time").getValue(String.class));
                            }
                        } else {
                            Toast.makeText(Pending_approvals.this, "Request details not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Failed to fetch request details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchEmployeeDetails(String email) {
        sheetRef.orderByChild("Official Email ID").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                String employeeName = data.child("Employee Name").getValue(String.class);
                                long emp_Id = data.child("Emp ID").getValue(long.class);
                                String empId = String.valueOf(emp_Id);
                                String officialEmail = data.child("Official Email ID").getValue(String.class);

                                emp_name.setText(employeeName != null ? employeeName : "N/A");
                                emp_id.setText(empId != null ? empId : "N/A");
                                emp_email.setText(officialEmail != null ? officialEmail : "N/A");
                            }
                        } else {
                            Toast.makeText(Pending_approvals.this, "Employee details not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Pending_approvals.this, "Failed to fetch employee details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
