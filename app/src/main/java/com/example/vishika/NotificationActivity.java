package com.example.vishika;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList;
    private TextView tvEmptyState;
    private ValueEventListener notificationsListener;
    private DatabaseReference notificationsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            currentUserId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList, currentUserId);
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.content.SharedPreferences prefs = getSharedPreferences("VishikaPrefs", MODE_PRIVATE);
        prefs.edit().putLong("last_seen_notification_" + currentUserId, System.currentTimeMillis()).apply();
    }

    private void loadNotifications() {
        notificationsRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications").child(currentUserId);
                
        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    AppNotification notification = dataSnapshot.getValue(AppNotification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                    }
                }
                
                // Show most recent first
                Collections.reverse(notificationList);
                adapter.notifyDataSetChanged();
                
                if (!notificationList.isEmpty()) {
                    android.content.SharedPreferences prefs = getSharedPreferences("VishikaPrefs", MODE_PRIVATE);
                    prefs.edit().putString("last_seen_notif_id_" + currentUserId, notificationList.get(0).id).apply();
                }
                
                if (notificationList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        notificationsRef.orderByChild("timestamp").addValueEventListener(notificationsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsRef != null && notificationsListener != null) {
            notificationsRef.orderByChild("timestamp").removeEventListener(notificationsListener);
        }
    }
}
