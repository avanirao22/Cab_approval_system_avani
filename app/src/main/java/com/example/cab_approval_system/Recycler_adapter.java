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

import java.util.List;

public class Recycler_adapter extends RecyclerView.Adapter<Recycler_adapter.RequestViewHolder> {

    private Context context;
    private List<RequestModel> requestList;
    private DatabaseReference notificationRef;

    public Recycler_adapter(Context context, List<RequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
        notificationRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Notification");
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

        holder.approveChip.setOnClickListener(v -> updateRequestStatus(request.getRequestId(), holder.statusTextView, holder.approveChip, request.getEmpEmail()));

    }

    private void updateRequestStatus(String requestId, TextView statusTextView, Chip approveChip, String requesterEmail) {
        // Check if requestId and requesterEmail are valid
        if (requestId == null || requesterEmail == null) {
            Toast.makeText(context, "Request details are incomplete.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call updateNotificationStatus in Pending_approvals to update Firebase
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

                    // Check if both request_id and requester_email match
                    if (dbRequestId != null && dbRequestId.equals(requestId) &&
                            dbRequesterEmail != null && dbRequesterEmail.equals(requesterEmail)) {

                        requestSnapshot.getRef().child("status").setValue("Approved ✅")
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(context, "Request approved successfully.", Toast.LENGTH_SHORT).show();
                                        statusTextView.setText("Approved ✅");
                                        approveChip.setVisibility(View.GONE);
                                    } else {
                                        Toast.makeText(context, "Failed to approve request.", Toast.LENGTH_SHORT).show();
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
