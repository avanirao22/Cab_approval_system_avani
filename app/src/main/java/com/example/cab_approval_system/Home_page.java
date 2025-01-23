package com.example.cab_approval_system;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import  android.widget.Toast;
import android.widget.ImageButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;


import androidx.appcompat.app.AppCompatActivity;

public class Home_page extends AppCompatActivity {

    private TextView emp_Name,empID,empTeam;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        Home_Screen.setupBottomNavigation(this);

        emp_Name = findViewById(R.id.emp_name_fetch);
        empID = findViewById(R.id.emp_id_edit_text);
        empTeam = findViewById(R.id.emp_team_edit_text);

        ImageButton request_ride = findViewById(R.id.request_ride);
        ImageButton pending_approvals = findViewById(R.id.pending_approvals);
        ImageButton cab_request = findViewById(R.id.cab_request);

        Intent intent = getIntent();
        String passedEmail = intent.getStringExtra("email");

        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        fetchUserData(passedEmail);

        request_ride.setOnClickListener(v->{
            Intent i = new Intent(Home_page.this, Request_ride.class);
            startActivity(i);
        });

        pending_approvals.setOnClickListener(v->{
            Intent i = new Intent(Home_page.this, Pending_approvals.class);
            startActivity(i);
        });

        cab_request.setOnClickListener(v->{
            Intent i = new Intent(Home_page.this, Cab_request.class);
            startActivity(i);
        });
    }

    private void fetchUserData(String email){
        if(email == null || email.isEmpty()) {
            Toast.makeText(this,"No email provided",Toast.LENGTH_SHORT).show();
            return;
        }
        databaseReference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Home_page.this,"Task is successful",Toast.LENGTH_SHORT).show();
                        DataSnapshot dataSnapshot = task.getResult();

                        if (dataSnapshot.exists()) {
                            Log.d("DataSnapshot", "Data: " + dataSnapshot.toString());;
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
                        Log.e("FirebaseError", "Error: " + task.getException().getMessage());
                        Toast.makeText(Home_page.this, "Failed to fetch data!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
