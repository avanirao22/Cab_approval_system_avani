package com.example.cab_approval_system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DepartmentHistoryFragment extends Fragment {

    private static final String ARG_TEAM = "team";
    private String requesterTeam;

    private History_adapter adapter;
    private final List<RequestModel> departmentRequestList = new ArrayList<>();

    public static DepartmentHistoryFragment newInstance(String team) {
        DepartmentHistoryFragment fragment = new DepartmentHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requesterTeam = getArguments().getString(ARG_TEAM);
            Log.d("DepartmentHistory", "Team: " + requesterTeam);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_department_history, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.activity_history_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new History_adapter(getContext(), departmentRequestList);
        recyclerView.setAdapter(adapter);

        fetchData();

        return view;
    }

    private void fetchData() {
        DatabaseReference sheetRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        sheetRef.orderByChild("Team").equalTo(requesterTeam)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> teamEmails = new ArrayList<>();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            String email = dataSnapshot.child("Official Email ID").getValue(String.class);
                            if (email != null) {
                                teamEmails.add(email);
                            }
                        }

                        Log.d("DepartmentHistory", "Team members count: " + teamEmails.size());

                        // Now fetch Approved_requests filtered by these emails
                        fetchApprovedRequests(teamEmails);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Error fetching team members", error.toException());
                    }
                });
    }

    private void fetchApprovedRequests(List<String> teamEmails) {
        DatabaseReference approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");

        approvedRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                departmentRequestList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String requesterEmail = dataSnapshot.child("Emp_email").getValue(String.class);

                    if (requesterEmail != null && teamEmails.contains(requesterEmail)) {
                        RequestModel request = dataSnapshot.getValue(RequestModel.class);
                        if (request != null) {
                            departmentRequestList.add(request);
                        }
                    }
                }

                Log.d("DepartmentHistory", "Final Approved Requests Size: " + departmentRequestList.size());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching approved requests", error.toException());
            }
        });
    }

}