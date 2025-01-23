package com.example.cab_approval_system;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.NumberPicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.checkerframework.checker.units.qual.C;
import org.w3c.dom.Text;

import java.util.Calendar;

public class Request_ride extends AppCompatActivity {

    private ImageButton time_picker_button,date_picker_button, decrease_button, increase_button;
    private TextView time_selected, date_selected,people_count, num_of_riders_edit_text;
    private View num_of_people_horizontal_layout;
    private ToggleButton toggle_button;
    private Button request_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_request_ride);

        time_picker_button = findViewById(R.id.time_picker_button);
        time_selected = findViewById(R.id.time_edit_text);
        date_picker_button = findViewById(R.id.date_picker_button);
        date_selected = findViewById(R.id.date_edit_text);
        increase_button =  findViewById(R.id.num_increase_button);
        decrease_button = findViewById(R.id.num_decrease_button);
        people_count = findViewById(R.id.people_count);
        num_of_riders_edit_text = findViewById(R.id.num_of_rides_edit_text);
        num_of_people_horizontal_layout =  findViewById(R.id.inner_num_of_passenger_layout);
        toggle_button = findViewById(R.id.toggleButton);
        request_button = findViewById(R.id.request_btn);

        time_picker_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar c=Calendar.getInstance();
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
                        },year,month,day);
                datePickerDialog.show();
            }
        });
        Spinner spinner = (Spinner) findViewById(R.id.purpose_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.purpose_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        increase_button.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 int currentCount = Integer.parseInt(people_count.getText().toString());
                 if (currentCount < 100 ){
                     currentCount++;
                     people_count.setText(String.valueOf(currentCount));
                 }
             }
         });
        decrease_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentCount = Integer.parseInt(people_count.getText().toString());
                if (currentCount > 0 ){
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

        request_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Request_ride.this, "Ride Requested", Toast.LENGTH_SHORT).show();
            }
        });
    }
}