package com.example.cab_approval_system;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Recycler_adapter extends RecyclerView.Adapter<Recycler_adapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private String approverEmail, userRole;
    private Map<String, RequestModel> requestMap;
    private DatabaseReference notificationRef, sheet1Ref, approvedByFHRef, approvedRequestsRef;
    private ImageView notificationDot;

    public Recycler_adapter(Context context, List<RequestModel> requestList, String approverEmail, String userRole, ImageView notificationDot) {
        this.context = context;
        this.requestList = requestList;
        this.approverEmail = approverEmail;
        this.userRole = userRole;
        this.notificationDot =notificationDot;

        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        approvedByFHRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_by_FH");
        approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Approved_requests");
        sheet1Ref = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");

        requestMap = new HashMap<>();

        Log.d("Role"," "+userRole);

        if (userRole.equals("HR Head")) {
            fetchPendingHRApprovals();  // HR now fetches from Approved_by_FH
            checkForHRNotifications();  // Check HR notifications correctly
        }
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_view_layout_activity, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        holder.nameTextView.setText(request.getEmpName());
        holder.empIdTextView.setText(request.getEmpId());
        holder.empEmailTextView.setText(request.getEmpEmail());
        holder.pickupTextView.setText(request.getPickupLocation());
        holder.dropoffTextView.setText(request.getDropoffLocation());
        holder.dateTextView.setText(request.getDate());
        holder.timeTextView.setText(request.getTime());
        holder.purposeTextView.setText(request.getPurpose());
        holder.statusTextView.setText(request.getStatus());
        holder.passengerCountTextView.setText(request.getNoOfPassengers());
        holder.approveFHName.setText(request.getApprovedFHName());
        holder.approvedFHEmail.setText(request.getApprovedFHEmail());
        holder.FHApprovedTime.setText(request.getApprovedTime());

        holder.passengerLayout.removeAllViews();
        if (request.getPassengerMap() != null) {
            for (String passengerName : request.getPassengerMap().values()) {
                TextView passengerTextView = new TextView(context);
                passengerTextView.setText(passengerName);
                passengerTextView.setTextSize(12);
                passengerTextView.setPadding(0, 8, 0, 8);
                holder.passengerLayout.addView(passengerTextView);
            }
        }

        if (isHRUser() && "Approved by FH".equals(request.getStatus()) && request.isApprovedByFH()) {
            holder.pendingLayout.setVisibility(View.VISIBLE);
            holder.approvedFHemailLayout.setVisibility(View.VISIBLE);
            holder.approvedFHnameLayout.setVisibility(View.VISIBLE);
            holder.FHapprovedTimeLayout.setVisibility(View.VISIBLE);
        } else {
            holder.pendingLayout.setVisibility(View.GONE);
            holder.approvedFHemailLayout.setVisibility(View.GONE);
            holder.approvedFHnameLayout.setVisibility(View.GONE);
            holder.FHapprovedTimeLayout.setVisibility(View.GONE);
        }

        holder.detailsLayout.setVisibility(View.GONE);

        holder.drop_down_button.setOnClickListener(v -> {
            if (holder.detailsLayout.getVisibility() == View.VISIBLE) {
                holder.detailsLayout.setVisibility(View.GONE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_down_24);
            } else {
                holder.detailsLayout.setVisibility(View.VISIBLE);
                holder.drop_down_button.setImageResource(R.drawable.baseline_arrow_drop_up_24);
            }
        });

        holder.approveChip.setOnClickListener(v -> {
            Log.d("ApproveClick", "Approve button clicked for request: " + request.getRequestId());
            approveRequest(request, holder.statusTextView, holder.approveChip);
        });
    }

    private void approveRequest(RequestModel request, TextView statusTextView, Chip approveChip) {
        sheet1Ref.orderByChild("Official Email ID").equalTo(approverEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String approverName = "Unknown";
                String approverRole = "Unknown";

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        approverName = data.child("Employee Name").getValue(String.class);
                        approverRole = data.child("Approval Matrix").getValue(String.class);
                        break;
                    }
                }

                if (approverName == null) {
                    Log.e("ApprovalProcess", "Approver details missing " + approverEmail);
                    Toast.makeText(context, "Approver details not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String finalApproverName = approverName;
                final String finalApproverRole = approverRole;


                DatabaseReference sourceRef = (finalApproverRole.equals("FH") ? notificationRef : approvedByFHRef);
                DatabaseReference destinationRef = (finalApproverRole.equals("FH")? approvedByFHRef : approvedRequestsRef);

                long requestIdLong = Long.parseLong(request.getRequestId());
                Log.d("ID"," "+ requestIdLong);

                sourceRef.orderByChild("request_id")
                        .equalTo(requestIdLong)
                        .addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                 if (!requestSnapshot.exists()) {
                                    Toast.makeText(context, "Request not found in the database.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                for (DataSnapshot snapshot : requestSnapshot.getChildren()) {
                                    String key = snapshot.getKey(); // Get the actual key in Firebase

                                    Map<String, Object> approvedData = new HashMap<>();
                                    approvedData.put("Approved_time", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
                                    approvedData.put("Date", request.getDate());
                                    approvedData.put("Approved_FH_name", finalApproverName);
                                    approvedData.put("Approved_FH_email", approverEmail);
                                    approvedData.put("Destination", request.getDropoffLocation());
                                    approvedData.put("Emp_ID", request.getEmpId());
                                    approvedData.put("Emp_name", request.getEmpName());
                                    approvedData.put("Emp_email", request.getEmpEmail());
                                    approvedData.put("Source", request.getPickupLocation());
                                    approvedData.put("Purpose", request.getPurpose());
                                    approvedData.put("Time", request.getTime());
                                    approvedData.put("no_of_passengers", request.getNoOfPassengers());
                                    approvedData.put("request_id", requestIdLong);

                                    if (request.getPassengerMap() != null) {
                                        approvedData.put("passengerNames", request.getPassengerMap());
                                    }

                                    if (finalApproverRole.equals("FH")) {
                                        approvedData.put("Status", "Approved by FH");
                                        approvedData.put("Pending", "HR approval pending");
                                        approvedData.put("approvedByFH", true);
                                        Log.d("FH name"," "+ finalApproverName);
                                        Log.d("FH email"," "+ approverEmail);

                                    } else {
                                        approvedData.put("Status", "Ride approved successfully");
                                        approvedData.put("approvedByFH", false);
                                        // Keep FH details (already stored when FH approved)
                                        approvedData.put("Approved_FH_name", request.getApprovedFHName());
                                        approvedData.put("Approved_FH_email", request.getApprovedFHEmail());
                                        // Add HR details
                                        approvedData.put("Approved_HR_name", finalApproverName);

                                        Log.d("FH name"," "+ request.getApprovedFHName());
                                        Log.d("approver"," "+ approverEmail);
                                        Log.d("Final approver"," "+ finalApproverName);
                                        Log.d("FH email"," "+ request.getApprovedFHEmail());

                                    }

                                    destinationRef.child(request.getRequestId()).setValue(approvedData)
                                            .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Only delete after successful transfer
                                            sourceRef.child(key).removeValue()
                                                    .addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    Toast.makeText(context, "Request approved", Toast.LENGTH_SHORT).show();
                                                    statusTextView.setText(approvedData.get("Status").toString());
                                                    approveChip.setVisibility(View.GONE);
                                                } else {
                                                    Toast.makeText(context, "Failed to remove old request.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(context, "Approval failed.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(context, "Database error.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to fetch approver details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPendingHRApprovals() {
        Log.d("HR_APPROVALS", "fetchPendingHRApprovals() called");
        approvedByFHRef.orderByChild("Pending").equalTo("HR approval pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Log.d("HR_APPROVALS", "Raw snapshot: " + data.getValue());

                            RequestModel request = data.getValue(RequestModel.class);  // Fetch full object directly

                            if (request != null) {
                                request.setRequestId(String.valueOf(data.child("request_id").getValue(Long.class)));
                                request.setApprovedByFH(Boolean.TRUE.equals(data.child("approvedByFH").getValue(Boolean.class)));

                                Log.d("HR_APPROVALS", "Fetched request: " + request.getRequestId() + " | " + request.getStatus());

                                requestList.add(request);
                            }
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HR_APPROVALS", "Error: " + error.getMessage());
                    }
                });
    }


    private void checkForHRNotifications() {
        if (!isHRUser()) {
            // 🚫 If user is not HR, don't check notifications
            return;
        }
        approvedByFHRef.orderByChild("Pending").equalTo("HR approval pending")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("HR_NOTIFICATIONS", "Total pending approvals: " + snapshot.getChildrenCount());

                boolean hasPendingHRApprovals = snapshot.exists();  // This is the key check
                if (notificationDot != null) {
                    notificationDot.setVisibility(hasPendingHRApprovals ? View.VISIBLE : View.GONE);
                    Log.d("HR_NOTIFICATIONS", "Notification dot " + (hasPendingHRApprovals ? "VISIBLE" : "HIDDEN"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HR_NOTIFICATIONS", "Error checking notifications: " + error.getMessage());
            }
        });
    }

    private boolean isHRUser() {
        return "HR Head".equals(userRole);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }
    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView,
                dropoffTextView, dateTextView, timeTextView, pendingTextView,
                purposeTextView, statusTextView, passengerCountTextView,passenger_details_title,approveFHName,approvedFHEmail,FHApprovedTime;
        Chip approveChip;
        ImageButton drop_down_button;
        LinearLayout detailsLayout, passengerLayout, pendingLayout,approvedFHnameLayout,approvedFHemailLayout,FHapprovedTimeLayout;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_textview);
            empIdTextView = itemView.findViewById(R.id.emp_id_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_emailid_textview);
            pickupTextView = itemView.findViewById(R.id.source_textview);
            dropoffTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            purposeTextView = itemView.findViewById(R.id.purpose_textview);
            statusTextView = itemView.findViewById(R.id.status_textview);
            approveChip = itemView.findViewById(R.id.approve_chip);
            drop_down_button =  itemView.findViewById(R.id.drop_down_button);
            detailsLayout = itemView.findViewById(R.id.outer_layout);
            passengerCountTextView = itemView.findViewById(R.id.no_of_passengers_textview);
            passengerLayout = itemView.findViewById(R.id.passenger_container);
            passenger_details_title =itemView.findViewById(R.id.passenger_details_title);
            pendingTextView =  itemView.findViewById(R.id.pending_textview);
            pendingLayout =  itemView.findViewById(R.id.layout15);
            approvedFHEmail = itemView.findViewById(R.id.approvedFHemail_textview);
            approveFHName = itemView.findViewById(R.id.approvedFHname_textview);
            FHApprovedTime = itemView.findViewById(R.id.FHApprovedTime_textview);
            approvedFHnameLayout = itemView.findViewById(R.id.layout12);
            approvedFHemailLayout = itemView.findViewById(R.id.layout13);
            FHapprovedTimeLayout = itemView.findViewById(R.id.layout14);
        }

    }
}