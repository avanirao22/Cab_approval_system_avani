package com.example.cab_approval_system;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Request_ride extends AppCompatActivity {

    private ImageButton time_picker_button, date_picker_button, decrease_button, increase_button;
    private TextView time_selected, date_selected, people_count, num_of_riders_edit_text, passengers_details;
    private View num_of_people_horizontal_layout;
    private ToggleButton select_toggle_button;
    private Button request_button;
    private EditText pickup, dropoff,purpose_of_ride;
    private String email_id;
    private LinearLayout passenger_layout;
    private int passenger_count;

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
        select_toggle_button = findViewById(R.id.select_toggleButton);
        request_button = findViewById(R.id.request_btn);
        pickup = findViewById(R.id.source_edit_text);
        dropoff = findViewById(R.id.destination_edit_text);
        purpose_of_ride = findViewById(R.id.purpose_edittext);
        passengers_details = findViewById(R.id.passenger_details);
        passenger_layout = findViewById(R.id.passengerdetails_layout);

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
        select_toggle_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int passengerCount = Integer.parseInt(people_count.getText().toString());
            //setting visibility on click of select button
            if (passengerCount > 1 && passengers_details.getVisibility() == View.GONE) {
                passengers_details.setVisibility(View.VISIBLE);
            }

            if (isChecked) {
                num_of_riders_edit_text.setText(String.valueOf(passengerCount));
                num_of_people_horizontal_layout.setVisibility(View.GONE);
                num_of_riders_edit_text.setVisibility(View.VISIBLE);

                generateEditTexts(passengerCount - 1);
            } else {
                people_count.setText(num_of_riders_edit_text.getText().toString());
                num_of_people_horizontal_layout.setVisibility(View.VISIBLE);
                num_of_riders_edit_text.setVisibility(View.GONE);
                passenger_layout.removeAllViews(); // Clear all passenger fields when toggled off
            }
        });
    }

    private void generateEditTexts(int numFields) {
        passenger_layout.removeAllViews(); // Clear existing views

        if (numFields <= 0) return; // No need to generate fields if count is 0 or negative

        for (int i = 1; i <= numFields; i++) {
            TextView textView = new TextView(this);
            textView.setText("Passenger " + i + ":");
            textView.setTextSize(16);
            textView.setPadding(10, 10, 10, 5);
            textView.setTextColor(ContextCompat.getColor(this,R.color.text_color));
            passenger_layout.addView(textView);

            EditText editText = new EditText(this);
            editText.setHint("Enter Passenger Name");
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setPadding(10, 10, 10, 10);
            editText.setId(View.generateViewId());
            textView.setBackgroundResource(R.drawable.edittext_bkg);
            textView.setTextColor(ContextCompat.getColor(this,R.color.text_color));// Assign unique ID
            passenger_layout.addView(editText);
        }
    }


    private void setupRequestButton() {
        request_button.setOnClickListener(v -> {
            String pickup_location = pickup.getText().toString();
            String dropoff_location = dropoff.getText().toString();
            String time = time_selected.getText().toString();
            String date = date_selected.getText().toString();
            String purpose = purpose_of_ride.getText().toString();
            String no_of_passengers = num_of_riders_edit_text.getText().toString();
            email_id = getIntent().getStringExtra("email");

            if (time.isEmpty() || date.isEmpty() || purpose.isEmpty() || no_of_passengers .isEmpty()) {
                Toast.makeText(Request_ride.this, "All fields need to be filled", Toast.LENGTH_SHORT).show();
            } else {
                saveDetails(pickup_location, dropoff_location, time, date, purpose, no_of_passengers , email_id);
            }
        });
    }

    private void saveDetails(String pickupLocation, String dropoffLocation, String time, String date, String purpose, String no_of_passengers, String email) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference requestDetailsRef = database.getReference("Request_details");
        DatabaseReference lastIdRef = database.getReference("Request_Counter");

        lastIdRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Integer lastId = task.getResult().getValue(Integer.class);
                if (lastId == null) lastId = 1;
                int finalNewId = lastId + 1;

                // Create a new request with updated ID
                RideRequest request = new RideRequest(finalNewId, pickupLocation, dropoffLocation, time, date, purpose, no_of_passengers, email);
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

    private void fetchApproverToken(String approverEmail, OnTokenFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Registration_data");  // Assuming you have a 'Users' table where the FCM token is stored

        usersRef.orderByChild("email").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    token = snapshot.child("fcm_token").getValue(String.class);
                }
                listener.onTokenFetched(token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch FCM token: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnTokenFetchedListener {
        void onTokenFetched(String token);
    }

    private void sendFCMNotification(int requestId, String token) {
        String message = "A new ride request has been submitted with ID: " + requestId;
        // Send notification via FCM (Actual FCM logic should be added here)
        Log.d("FCM", "Sending notification to token: " + token + " with message: " + message);
    }

    private void saveNotificationData(int requestId, String approverEmail) {
        DatabaseReference notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");

        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(new Date(currentTimeMillis));

        String message = "A new ride request has been submitted with ID: " + requestId;
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("approver_email", approverEmail);
        notificationData.put("message", message);
        notificationData.put("request_id", requestId);
        notificationData.put("status", "pending");
        notificationData.put("timestamp", formattedTime);
        notificationData.put("title", "Ride Request Pending");
        notificationData.put("requester_email",email_id);

        notificationRef.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> Log.d("Notification", "Notification saved successfully"))
                .addOnFailureListener(e -> Log.e("Notification", "Failed to save notification: " + e.getMessage()));
    }

    private void fetchApproverEmail(String employeeEmail, OnApproverFetchedListener listener) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        usersRef.orderByChild("Official Email ID").equalTo(employeeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String approverEmail = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    approverEmail = snapshot.child("Email ID of Approver").getValue(String.class);
                }
                listener.onApproverFetched(approverEmail);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Request_ride.this, "Failed to fetch approver email: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnApproverFetchedListener {
        void onApproverFetched(String approverEmail);
    }


    private void clearFields() {
        pickup.setText("");
        dropoff.setText("");
        time_selected.setText("");
        date_selected.setText("");
        num_of_riders_edit_text.setText("0");
        people_count.setText("0");
        select_toggle_button.setChecked(false);
        purpose_of_ride.setText("");
    }
    public class RideRequest {
        private int id;
        private String pickupLocation;
        private String dropoffLocation;
        private String time;
        private String date;
        private String purpose;
        private String no_of_passengers ;
        private String email;

        public RideRequest(){
        }

        public RideRequest(int id, String pickupLocation, String dropoffLocation, String time, String date, String purpose, String no_of_passengers, String email) {
            this.id = id;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.time = time;
            this.date = date;
            this.purpose = purpose;
            this.no_of_passengers  = no_of_passengers;
            this.email = email;
        }

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getPickupLocation() { return pickupLocation; }
        public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
        public String getDropoffLocation() { return dropoffLocation; }
        public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getNo_of_passengers() { return no_of_passengers ; }
        public void setNo_of_passengers(String no_of_passengers) { this.no_of_passengers  = no_of_passengers; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

}
