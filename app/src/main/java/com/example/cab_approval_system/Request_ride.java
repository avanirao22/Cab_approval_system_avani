package com.example.cab_approval_system;

import static java.security.AccessController.getContext;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.NumberPicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.checkerframework.checker.units.qual.C;
import org.w3c.dom.Text;

import java.util.Calendar;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_request_ride);


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

        time_picker_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int min = c.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(Request_ride.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                String amPm;
                                if (hourOfDay >= 12) {
                                    amPm = "PM";
                                    if (hourOfDay > 12) {
                                        hourOfDay -= 12;
                                    }
                                } else {
                                    amPm = "AM";
                                    if (hourOfDay == 0) {
                                        hourOfDay = 12; // Midnight is 12:00 AM
                                    }
                                }
                                String time = String.format("%02d:%02d %s", hourOfDay, minute, amPm);
                                time_selected.setText(time);
                            }
                        }, hour, min, false);
                timePickerDialog.show();
            }
        });

        date_picker_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c = Calendar.getInstance();

                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(Request_ride.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                date_selected.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });
        Spinner spinner = (Spinner) findViewById(R.id.project_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.purpose_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        increase_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentCount = Integer.parseInt(people_count.getText().toString());
                if (currentCount < 100) {
                    currentCount++;
                    people_count.setText(String.valueOf(currentCount));
                }
            }
        });
        decrease_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentCount = Integer.parseInt(people_count.getText().toString());
                if (currentCount > 0) {
                    currentCount--;
                    people_count.setText(String.valueOf(currentCount));
                }
            }
        });
        toggle_button.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // If ToggleButton is ON (Change mode)
                String currentCount = people_count.getText().toString();
                num_of_riders_edit_text.setText(currentCount);  // Set the selected number to edit text
                num_of_people_horizontal_layout.setVisibility(View.GONE);  // Hide the number buttons
                num_of_riders_edit_text.setVisibility(View.VISIBLE);  // Show the EditText for manual input
            } else {
                // If ToggleButton is OFF (View mode)
                String selectedNumber = num_of_riders_edit_text.getText().toString();
                people_count.setText(selectedNumber);  // Set the text from EditText back to people_count TextView
                num_of_people_horizontal_layout.setVisibility(View.VISIBLE);  // Show the number buttons
                num_of_riders_edit_text.setVisibility(View.GONE);  // Hide the EditText
            }
        });

        request_button.setOnClickListener(v -> {
            String pickup_location = pickup.getText().toString();
            String dropoff_location = dropoff.getText().toString();
            String distnace_obtained = distance.getText().toString();
            String time = time_selected.getText().toString();
            String date = date_selected.getText().toString();
            String project = project_spinner.getSelectedItem().toString();
            String count = num_of_riders_edit_text.getText().toString();
            String email_id = getIntent().getStringExtra("email");

            if (distnace_obtained.isEmpty() || time.isEmpty() || date.isEmpty() || project.isEmpty() || count.isEmpty()) {
                Toast.makeText(Request_ride.this, "ALL fields need to be filled", Toast.LENGTH_SHORT).show();
            } else {
                saveDetails(pickup_location, dropoff_location, distnace_obtained, time, date, project, count, email_id);
            }

        });
    }
    private void saveDetails(String pickupLocation, String dropoffLocation, String distanceObtained, String time, String date, String project, String count, String email) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference requestDetailsRef = database.getReference("Request_details");
        DatabaseReference lastIdRef = database.getReference("Request_Counter");

        lastIdRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer lastId = task.getResult().exists() ? task.getResult().getValue(Integer.class) : 1;
                if (lastId == null) lastId = 1;

                int finalNewId = lastId + 1;
                Toast.makeText(Request_ride.this, "Fetched request_counter: " + lastId, Toast.LENGTH_SHORT).show();

                // Create the RideRequest object with the email added
                RideRequest request = new RideRequest(finalNewId, pickupLocation, dropoffLocation, distanceObtained, time, date, project, count, email);
                requestDetailsRef.child(String.valueOf(finalNewId)).setValue(request)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Request_ride.this, "Request details saved successfully", Toast.LENGTH_SHORT).show();


                            // Clear all input fields
                            pickup.setText("");
                            dropoff.setText("");
                            distance.setText("");
                            time_selected.setText("");
                            date_selected.setText("");
                            num_of_riders_edit_text.setText("0");
                            people_count.setText("0");
                            toggle_button.setChecked(false);
                            project_spinner.setSelection(0);

                            // Update the last ID in Firebase
                            lastIdRef.setValue(finalNewId);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Request_ride.this, "Failed to save request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(Request_ride.this, "Error retrieving request_counter: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

        // Default constructor required for Firebase
        public RideRequest() {}

        public RideRequest(int id, String pickupLocation, String dropoffLocation, String distanceObtained, String time, String date, String project, String count, String email) {
            this.id= id;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.distanceObtained = distanceObtained;
            this.time = time;
            this.date = date;
            this.project = project;
            this.count = count;
            this.email =email;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
        // Getters and setters (if needed)
        public String getPickupLocation() {
            return pickupLocation;
        }

        public void setPickupLocation(String pickupLocation) {
            this.pickupLocation = pickupLocation;
        }

        public String getDropoffLocation() {
            return dropoffLocation;
        }

        public void setDropoffLocation(String dropoffLocation) {
            this.dropoffLocation = dropoffLocation;
        }

        public String getDistanceObtained() {
            return distanceObtained;
        }

        public void setDistanceObtained(String distanceObtained) {
            this.distanceObtained = distanceObtained;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getProject() {
            return project;
        }

        public void setProject(String project) {
            this.project = project;
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getEmail() {
            return email;
        }
        public String setEmail() {
            return email;
        }
    }

}
