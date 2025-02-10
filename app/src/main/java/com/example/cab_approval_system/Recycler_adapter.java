package com.example.cab_approval_system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class Recycler_adapter extends RecyclerView.Adapter<Recycler_adapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private String approverEmail;
    private DatabaseReference notificationRef, sheet1Ref;

    public Recycler_adapter(Context context, List<RequestModel> requestList, String approverEmail) {
        this.context = context;
        this.requestList = requestList;
        this.approverEmail = approverEmail;
        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
        sheet1Ref = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Sheet1");
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
        holder.distanceTextView.setText(request.getDistance());
        holder.projectTextView.setText(request.getProject());
        holder.statusTextView.setText(request.getStatus());

        holder.approveChip.setOnClickListener(v -> updateRequestStatus(request, holder.statusTextView, holder.approveChip));
    }

    private void updateRequestStatus(RequestModel request, TextView statusTextView, Chip approveChip) {
        if (request.getRequestId() == null || request.getEmpEmail() == null) {
            Toast.makeText(context, "Request details are incomplete.", Toast.LENGTH_SHORT).show();
            return;
        }

        sheet1Ref.orderByChild("Official Email ID").equalTo(approverEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String approverName = "Unknown Approver";

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        approverName = data.child("Employee Name").getValue(String.class);
                        if (approverName != null) {
                            break;
                        }
                    }
                } else {
                    Toast.makeText(context, "No matching record found for this approver email.", Toast.LENGTH_SHORT).show();
                }

                if (approverName == null) {
                    Toast.makeText(context, "Approver name not found in the database.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Fetched approver name: " + approverName, Toast.LENGTH_SHORT).show();
                }

                final String finalApproverName = approverName;

                notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean recordFound = false;
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String dbRequestId;
                            try {
                                dbRequestId = String.valueOf(requestSnapshot.child("request_id").getValue(Long.class));
                            } catch (Exception e) {
                                dbRequestId = requestSnapshot.child("request_id").getValue(String.class);
                            }

                            String dbRequesterEmail = requestSnapshot.child("requester_email").getValue(String.class);

                            if (dbRequestId != null && dbRequestId.equals(request.getRequestId()) &&
                                    dbRequesterEmail != null && dbRequesterEmail.equals(request.getEmpEmail())) {

                                // Copy specific request fields to the Approved_requests table
                                DatabaseReference approvedRequestsRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                                        .getReference("Approved_requests");

                                Map<String, Object> approvedRequestData = new HashMap<>();

                                // Convert current timestamp to readable date format
                                long currentTimeMillis = System.currentTimeMillis();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                                String formattedTime = sdf.format(new Date(currentTimeMillis));

                                approvedRequestData.put("approved_time", formattedTime);
                                approvedRequestData.put("approver_name", finalApproverName);
                                approvedRequestData.put("approver_email", approverEmail);
                                approvedRequestData.put("date", request.getDate());
                                approvedRequestData.put("distance", request.getDistance());
                                approvedRequestData.put("dropoffLocation", request.getDropoffLocation());
                                approvedRequestData.put("Emp_ID", request.getEmpId());
                                approvedRequestData.put("Emp_name", request.getEmpName());
                                approvedRequestData.put("Emp_email", request.getEmpEmail());
                                approvedRequestData.put("PickupLocation", request.getPickupLocation());
                                approvedRequestData.put("Project", request.getProject());
                                approvedRequestData.put("Request_id", request.getRequestId());
                                approvedRequestData.put("Status", "Approved");
                                approvedRequestData.put("time", request.getTime());

                                approvedRequestsRef.child(request.getRequestId()).setValue(approvedRequestData)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                // Remove the request from the Notification table
                                                requestSnapshot.getRef().removeValue()
                                                        .addOnCompleteListener(removeTask -> {
                                                            if (removeTask.isSuccessful()) {
                                                                Toast.makeText(context, "Request approved and moved to ApprovedRequests.", Toast.LENGTH_SHORT).show();
                                                                statusTextView.setText("Approved ✅");
                                                                approveChip.setVisibility(View.GONE);
                                                            } else {
                                                                Toast.makeText(context, "Failed to remove request from pending list.", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(context, "Failed to move request to ApprovedRequests.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                recordFound = true;
                                break;
                            }
                        }

                        if (!recordFound) {
                            Toast.makeText(context, "No matching record found for this request and email.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Error updating notification status.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to fetch approver details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, empIdTextView, empEmailTextView, pickupTextView,
                dropoffTextView, dateTextView, timeTextView, distanceTextView,
                projectTextView, statusTextView;
        Chip approveChip;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_textview);
            empIdTextView = itemView.findViewById(R.id.emp_id_textview);
            empEmailTextView = itemView.findViewById(R.id.emp_emailid_textview);
            pickupTextView = itemView.findViewById(R.id.source_textview);
            dropoffTextView = itemView.findViewById(R.id.destination_textview);
            dateTextView = itemView.findViewById(R.id.date_textview);
            timeTextView = itemView.findViewById(R.id.time_textview);
            distanceTextView = itemView.findViewById(R.id.distance_textview);
            projectTextView = itemView.findViewById(R.id.project_textview);
            statusTextView = itemView.findViewById(R.id.status_textview);
            approveChip = itemView.findViewById(R.id.approve_chip);
        }
    }
}

