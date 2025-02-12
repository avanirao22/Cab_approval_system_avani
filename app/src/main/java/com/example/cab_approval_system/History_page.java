package com.example.cab_approval_system;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.*;

public class History_page extends AppCompatActivity {

    private RecyclerView recyclerView;
    private History_adapter adapter;
    private List<RequestModel> approvedRequestList;
    private DatabaseReference approvedRequestsRef, employeeRef;
    private Map<String, String> employeeTeamMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);

        String requesterEmail = getIntent().getStringExtra("email");
        if (requesterEmail == null) {
            requesterEmail = "";
        }
        Log.d("email", "Requester email: " + requesterEmail);

        employeeRef =FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Sheet1");
        approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("Approved_requests");

        recyclerView = findViewById(R.id.history_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        approvedRequestList = new ArrayList<>();
        adapter = new History_adapter(this, approvedRequestList);
        recyclerView.setAdapter(adapter);

        loadEmployeeData(requesterEmail);
    }

    private void loadEmployeeData(String requesterEmail) {
        employeeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String requesterDesignation = null;
                String requesterTeam = null;

                for (DataSnapshot empSnapshot : snapshot.getChildren()) {
                    String email = empSnapshot.child("Official Email ID").getValue(String.class);
                    String team = empSnapshot.child("Team").getValue(String.class);

                    if (email != null && team != null) {
                        employeeTeamMap.put(email.toLowerCase(), team);
                    }

                    if (email != null && email.equalsIgnoreCase(requesterEmail)) {
                        requesterDesignation = empSnapshot.child("Approval Matrix").getValue(String.class);
                        requesterTeam = team;
                    }
                }

                if (requesterDesignation == null) {
                    Log.e("FirebaseError", "User designation not found!");
                    return;
                }
                Log.d("Designation", "User is a: " + requesterDesignation);
                fetchApprovedRequests(requesterDesignation, requesterEmail, requesterTeam);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching employee data", error.toException());
            }
        });
    }

    private void fetchApprovedRequests(String designation, String requesterEmail, String team) {
        approvedRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                approvedRequestList.clear();
                Set<Integer> requestIds = new HashSet<>(); // To prevent duplicates

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String empEmail = requestSnapshot.child("Emp_email").getValue(String.class);
                    if (empEmail == null) continue;

                    String empTeam = employeeTeamMap.getOrDefault(empEmail.toLowerCase(), "");
                    boolean shouldAdd = false;

                    if (designation.equals("Employee") && empEmail.equalsIgnoreCase(requesterEmail)) {
                        shouldAdd = true;
                    } else if (designation.equals("FH") && team != null && team.equals(empTeam)) {
                        shouldAdd = true;
                    } else if (designation.equals("HR Head")) {
                        shouldAdd = true;
                    }

                    if (shouldAdd) {
                        int requestId = getIntValue(requestSnapshot, "Request_id");
                        if (!requestIds.contains(requestId)) {
                            approvedRequestList.add(mapRequestModel(requestSnapshot));
                            requestIds.add(requestId);
                        }
                    }
                }
                Log.d("History", "Filtered request list size: " + approvedRequestList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching data", error.toException());
            }
        });
    }

    private RequestModel mapRequestModel(DataSnapshot snapshot) {
        RequestModel request = new RequestModel();
        Long empId = snapshot.child("Emp_ID").getValue(Long.class);
        request.setEmpId(empId != null ? empId.toString() : "");
        request.setEmpName(snapshot.child("Emp_name").getValue(String.class));
        request.setEmpEmail(snapshot.child("Emp_email").getValue(String.class));
        request.setRequestId(getIntValue(snapshot, "Request_id"));
        request.setPickupLocation(snapshot.child("Source").getValue(String.class));
        request.setDropoffLocation(snapshot.child("Destination").getValue(String.class));
        request.setDate(snapshot.child("Date").getValue(String.class));
        request.setDistance(snapshot.child("Distance").getValue(String.class));
        request.setProject(snapshot.child("Project").getValue(String.class));
        request.setTime(snapshot.child("Time").getValue(String.class));
        request.setApproverName(snapshot.child("Approver_name").getValue(String.class));
        request.setApprovedTime(snapshot.child("Approved_time").getValue(String.class));
        request.setApproverEmail(snapshot.child("Approver_email").getValue(String.class));
        request.setStatus(snapshot.child("Status").getValue(String.class));
        return request;
    }

    private int getIntValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.e("FirebaseError", key + " is not a valid number: " + value);
            }
        }
        return 0;
    }
}