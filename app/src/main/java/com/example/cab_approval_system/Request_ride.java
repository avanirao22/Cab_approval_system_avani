package com.example.cab_approval_system;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Request_ride extends AppCompatActivity {

    private ImageButton time_picker_button, date_picker_button, decrease_button, increase_button;
    private TextView time_selected, date_selected, people_count, num_of_riders_edit_text, distance;
    private View num_of_people_horizontal_layout;
    private ToggleButton toggle_button;
    private Button request_button;
    private EditText pickup, dropoff;
    private Spinner project_spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_ride);

        initializeUI();

        setupDateTimePickers();
        setupCountButtons();
        setupToggleButton();
        setupRequestButton();

        // Subscribe to Firebase topic for notifications
        FirebaseMessaging.getInstance().subscribeToTopic("ride_requests")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseMessaging", "Subscribed to ride_requests topic");
                    }
                });
        FirebaseApp.initializeApp(this);

    }

    private void initializeUI() {
        time_picker_button = findViewById(R.id.time_picker_button);
        time_selected = findViewById(R.id.time_edit_text);
        date_picker_button = findViewById(R.id.date_picker_button);
        date_selected = findViewById(R.id.date_edit_text);
        increase_button = findViewById(R.id.num_increase_button);
        decrease_button = findViewById(R.id.num_decrease_button);
        people_count = findViewById(R.id.num_of_rides_edit_text);
        num_of_riders_edit_text = findViewById(R.id.people_count);
        num_of_people_horizontal_layout = findViewById(R.id.inner_num_of_passenger_layout);
        toggle_button = findViewById(R.id.toggleButton);
        request_button = findViewById(R.id.request_btn);
        pickup = findViewById(R.id.source_edit_text);
        dropoff = findViewById(R.id.destination_edit_text);
        distance = findViewById(R.id.distance_edit_text);
        project_spinner = findViewById(R.id.project_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.purpose_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        project_spinner.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        time_picker_button.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(Request_ride.this,
                    (view, hourOfDay, minute) -> {
                        String amPm = hourOfDay >= 12 ? "PM" : "AM";
                        hourOfDay = hourOfDay % 12;
                        hourOfDay = hourOfDay == 0 ? 12 : hourOfDay;
                        String time = String.format("%02d:%02d %s", hourOfDay, minute, amPm);
                        time_selected.setText(time);
                    }, hour, min, false);
            timePickerDialog.show();
        });

        date_picker_button.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(Request_ride.this,
                    (view, year1, month1, dayOfMonth) -> date_selected.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1),
                    year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupCountButtons() {
        increase_button.setOnClickListener(v -> {
            int currentCount = Integer.parseInt(people_count.getText().toString());
            if (currentCount < 100) {
                currentCount++;
                people_count.setText(String.valueOf(currentCount));
            }
        });

        decrease_button.setOnClickListener(v -> {
            int currentCount = Integer.parseInt(people_count.getText().toString());
            if (currentCount > 0) {
                currentCount--;
                people_count.setText(String.valueOf(currentCount));
            }
        });
    }

    private void setupToggleButton() {
        toggle_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String currentCount = people_count.getText().toString();
                num_of_riders_edit_text.setText(currentCount);
                num_of_people_horizontal_layout.setVisibility(View.GONE);
                num_of_riders_edit_text.setVisibility(View.VISIBLE);
            } else {
                String selectedNumber = num_of_riders_edit_text.getText().toString();
                people_count.setText(selectedNumber);
                num_of_people_horizontal_layout.setVisibility(View.VISIBLE);
                num_of_riders_edit_text.setVisibility(View.GONE);
            }
        });
    }

    private void setupRequestButton() {
        request_button.setOnClickListener(v -> {
            String pickup_location = pickup.getText().toString();
            String dropoff_location = dropoff.getText().toString();
            String distanceObtained = distance.getText().toString();
            String time = time_selected.getText().toString();
            String date = date_selected.getText().toString();
            String project = project_spinner.getSelectedItem().toString();
            String count = num_of_riders_edit_text.getText().toString();
            String email_id = getIntent().getStringExtra("email");

            if (distanceObtained.isEmpty() || time.isEmpty() || date.isEmpty() || project.isEmpty() || count.isEmpty()) {
                Toast.makeText(Request_ride.this, "All fields need to be filled", Toast.LENGTH_SHORT).show();
            } else {
                saveDetails(pickup_location, dropoff_location, distanceObtained, time, date, project, count, email_id);
            }
        });
    }

    private void saveDetails(String pickupLocation, String dropoffLocation, String distanceObtained, String time, String date, String project, String count, String email) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference requestDetailsRef = database.getReference("Request_details");
        DatabaseReference lastIdRef = database.getReference("Request_Counter");

        lastIdRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Integer lastId = task.getResult().getValue(Integer.class);
                if (lastId == null) lastId = 1;
                int finalNewId = lastId + 1;

                // Create a new request with updated ID
                RideRequest request = new RideRequest(finalNewId, pickupLocation, dropoffLocation, distanceObtained, time, date, project, count, email);
                requestDetailsRef.child(String.valueOf(finalNewId)).setValue(request)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Request_ride.this, "Ride Requested ", Toast.LENGTH_SHORT).show();
                            clearFields();

                            // Update the counter in Firebase after saving
                            lastIdRef.setValue(finalNewId)
                                    .addOnSuccessListener(unused -> {
                                        fetchApproverEmail(email, approverEmail -> {
                                            if (approverEmail != null) {
                                                fetchApproverToken(approverEmail, token -> {
                                                    if (token != null) {
                                                        sendFCMNotification(finalNewId, token);
                                                        saveNotificationData(finalNewId, approverEmail); // Save notification data
                                                    } else {
                                                        Toast.makeText(Request_ride.this, "Approver FCM token not found", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(Request_ride.this, "Approver email not found", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(Request_ride.this, "Failed to update request counter: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(Request_ride.this, "Failed to save request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(Request_ride.this, "Error retrieving request_counter", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchApproverToken(String approverEmail, OnApproverTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data"); // Firebase table containing user tokens
        usersRef.orderByChild("email").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class); // Adjust according to your schema
                }
                listener.onApproverTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch FCM token: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFCMNotification(int requestId, String token) {

        String message = "A new ride request has been submitted with ID: " + requestId;

    }

    private void saveNotificationData(int requestId, String approverEmail) {
        if (approverEmail == null || approverEmail.isEmpty()) {
            Toast.makeText(Request_ride.this, "Approver email is empty, cannot send notification.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification"); // Ensure correct table name

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("approver_id", approverEmail);
        notificationData.put("message", "A new ride request has been submitted with ID: " + requestId);
        notificationData.put("request_id", requestId);
        notificationData.put("status", "unread");
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("title", "New Ride Request");

        notificationRef.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> Toast.makeText(Request_ride.this, "Notification sent to approver.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(Request_ride.this, "Failed to send notification: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    public interface OnApproverTokenFetchedListener {
        void onApproverTokenFetched(String token);
    }
    private void fetchApproverEmail(String employeeEmail, OnApproverEmailFetchedListener listener) {
        DatabaseReference employeesRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
        employeesRef.orderByChild("Officail Email ID").equalTo(employeeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String approverEmail = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    approverEmail = snapshot.child("Email ID of Approver").getValue(String.class);
                }
                listener.onApproverEmailFetched(approverEmail);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch approver email: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnApproverEmailFetchedListener {
        void onApproverEmailFetched(String approverEmail);
    }

    private void clearFields() {
        pickup.setText("");
        dropoff.setText("");
        distance.setText("");
        time_selected.setText("");
        date_selected.setText("");
        num_of_riders_edit_text.setText("0");
        people_count.setText("0");
        toggle_button.setChecked(false);
        project_spinner.setSelection(0);
    }
    public class RideRequest {
        private int id;
        private String pickupLocation;
        private String dropoffLocation;
        private String distanceObtained;
        private String time;
        private String date;
        private String project;
        private String count;
        private String email;

        public RideRequest(){

        }

        public RideRequest(int id, String pickupLocation, String dropoffLocation, String distanceObtained, String time, String date, String project, String count, String email) {
            this.id = id;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.distanceObtained = distanceObtained;
            this.time = time;
            this.date = date;
            this.project = project;
            this.count = count;
            this.email = email;
        }

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
        public String getDropoffLocation() { return dropoffLocation; }
        public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }
        public String getDistanceObtained() { return distanceObtained; }
        public void setDistanceObtained(String distanceObtained) { this.distanceObtained = distanceObtained; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
        public String getCount() { return count; }
        public void setCount(String count) { this.count = count; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

}
