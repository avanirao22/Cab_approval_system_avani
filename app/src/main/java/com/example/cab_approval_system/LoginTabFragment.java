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

    private DatabaseReference databaseReference,  employeeReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login_tab, container, false);

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
        // Reference to Registration Data table
        DatabaseReference registrationReference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Registration_data");

        registrationReference.orderByChild("email").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String dbPassword = snapshot.child("password").getValue(String.class);
                                if (password.equals(dbPassword)) {
                                    // Password matches, now fetch user role
                                    fetchUserRoleFromSheet(email);
                                    return;
                                }
                            }
                            // Invalid email or password
                            Toast.makeText(getContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Email not registered", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error connecting to Registration details", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserRoleFromSheet(String email) {
        // Reference to Sheet1 (or intern) table
        DatabaseReference sheet1Reference = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Sheet1");  // Change to intern table if needed

        // Query the Sheet1 table for the user's role
        sheet1Reference.orderByChild("Official Email ID").equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot result = task.getResult();
                        if (result.exists()) {
                            for (DataSnapshot snapshot : result.getChildren()) {
                                String userRole = snapshot.child("Approval Matrix").getValue(String.class);
                                if (userRole != null) {
                                    // Successfully fetched the role, navigate to home page
                                    Toast.makeText(getContext(), "Login successful as " + userRole, Toast.LENGTH_SHORT).show();

                                    // Pass the user role and email to the Home Page
                                    Intent intent = new Intent(getContext(), Home_page.class);
                                    intent.putExtra("email", email);
                                    intent.putExtra("userRole", userRole);
                                    startActivity(intent);

                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                    return;
                                } else {
                                    Toast.makeText(getContext(), "Role not found for this user", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(getContext(), "User not found in Sheet1 table", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error connecting to Sheet1 table", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
