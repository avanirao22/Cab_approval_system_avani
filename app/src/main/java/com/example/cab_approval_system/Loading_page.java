package com.example.cab_approval_system;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Loading_page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading_page);

        // Delay for 5 seconds before navigating to the login page
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Loading_page.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish(); // Finish the current activity to prevent going back to it
        }, 100); // 5000 milliseconds = 5 seconds
    }
}
