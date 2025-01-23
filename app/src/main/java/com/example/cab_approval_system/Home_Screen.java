package com.example.cab_approval_system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class Home_Screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        // Call the helper method to set up the bottom navigation buttons
        setupBottomNavigation(this);
    }

    // This method sets up the bottom navigation buttons and their click listeners
    public static void setupBottomNavigation(Context context) {
        // Initialize the ImageButtons with context
        ImageButton homeImageBtn = ((AppCompatActivity) context).findViewById(R.id.home_image_button);
        ImageButton historyImageBtn = ((AppCompatActivity) context).findViewById(R.id.history_image_button);
        ImageButton profileImageBtn = ((AppCompatActivity) context).findViewById(R.id.profile_image_button);

        // Set up onClick listeners for navigation
        homeImageBtn.setOnClickListener(v -> {
            // Navigate to Home page
            Intent intent = new Intent(context, Home_page.class);
            context.startActivity(intent);
        });

        historyImageBtn.setOnClickListener(v -> {
            // Navigate to History page
            Intent intent = new Intent(context, History_page.class);
            context.startActivity(intent);
        });

        profileImageBtn.setOnClickListener(v -> {
            // Navigate to Profile page
            Intent intent = new Intent(context, Profile_page.class);
            context.startActivity(intent);
        });
    }
}
