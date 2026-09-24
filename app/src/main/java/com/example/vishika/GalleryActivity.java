package com.example.vishika;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private GalleryAdapter adapter;
    private List<Gift> allGifts = new ArrayList<>();
    private List<Gift> displayedGifts = new ArrayList<>();
    private TextView txtEmptyGallery;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private String mySenderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        txtEmptyGallery = findViewById(R.id.txtEmptyGallery);
        
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new GalleryAdapter(this, displayedGifts);
        galleryRecyclerView.setAdapter(adapter);

        mySenderId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        String partnerName = MainActivity.IS_VISHAL ? "Rashika" : "Vishal";

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Sent by you"));
        tabLayout.addTab(tabLayout.newTab().setText("Sent by " + partnerName));

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                filterGifts();
            }
            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        loadPastGifts();
    }

    private void loadPastGifts() {
        DatabaseReference pastGiftsRef = FirebaseDatabase.getInstance().getReference("past_gifts").child("shared");
        pastGiftsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allGifts.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Gift gift = ds.getValue(Gift.class);
                    if (gift != null) {
                        if ("Photo".equals(gift.getType()) || "Video".equals(gift.getType()) || "Voice".equals(gift.getType())) {
                            allGifts.add(gift);
                        }
                    }
                }
                
                Collections.reverse(allGifts);
                filterGifts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void filterGifts() {
        displayedGifts.clear();
        boolean showingSentByMe = tabLayout.getSelectedTabPosition() == 0;
        
        for (Gift gift : allGifts) {
            boolean sentByMe = mySenderId.equals(gift.getSender());
            if (showingSentByMe == sentByMe) {
                displayedGifts.add(gift);
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (displayedGifts.isEmpty()) {
            txtEmptyGallery.setVisibility(View.VISIBLE);
        } else {
            txtEmptyGallery.setVisibility(View.GONE);
        }
    }
}
