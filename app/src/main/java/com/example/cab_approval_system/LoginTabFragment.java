package com.example.cab_approval_system;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginTabFragment extends Fragment {

    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

        // Reference to Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Registration_data");

        EditText emailField = view.findViewById(R.id.login_email);
        EditText passwordField = view.findViewById(R.id.login_password);
        Button loginButton = view.findViewById(R.id.login_button);
        Button registrationButton = view.findViewById(R.id.registration_button);

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Input validation
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }
            // Authenticate with Firebase
            loginUser(email, password);
        });

        // Registration button click listener
        registrationButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Navigate to Registration fragment
                ViewPager2 viewPager2 = getActivity().findViewById(R.id.view_pager);
                if (viewPager2 != null) {
                    viewPager2.setCurrentItem(1); // Assuming Registration is at position 1
                }
            }
        });

        return view;
    }

    private void loginUser(String email, String password) {
        databaseReference.orderByChild("email").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            // Check password for the retrieved email
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String dbPassword = snapshot.child("password").getValue(String.class);
                                if (password.equals(dbPassword)) {
                                    Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();

                                    // Navigate to Home Page
                                    Intent intent = new Intent(getContext(), Home_page.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);

                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                    return;
                                }
                            }
                            // Password mismatch
                            Toast.makeText(getContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                        } else {
                            // Email not found
                            Toast.makeText(getContext(), "Email not registered", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Task failure or null result
                        Toast.makeText(getContext(), "Error connecting to database", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
