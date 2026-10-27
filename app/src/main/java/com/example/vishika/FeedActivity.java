package com.example.vishika;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private List<FeedPost> posts;
    private DatabaseReference postsRef;
    private String currentUserId;
    private String currentUserName;

    private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";
    private OkHttpClient httpClient;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private ActivityResultLauncher<Intent> newPostLauncher;
    
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private ValueEventListener feedListener;
    private int currentLimit = 10;
    private boolean isLoading = false;
    private String pendingScrollPostId;
    private boolean pendingOpenComments;
    private String latestNotificationId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        currentUserId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        currentUserName = MainActivity.IS_VISHAL ? "Vishal" : "Rashika";

        postsRef = FirebaseDatabase.getInstance().getReference().child("feed_posts");
        httpClient = new OkHttpClient();

        pendingScrollPostId = getIntent().getStringExtra("SCROLL_TO_POST");
        pendingOpenComments = getIntent().getBooleanExtra("OPEN_COMMENTS", false);

        android.widget.FrameLayout layoutNotifications = findViewById(R.id.layoutNotifications);
        android.view.View ivNotificationBadge = findViewById(R.id.ivNotificationBadge);
        
        android.content.SharedPreferences prefs = getSharedPreferences("VishikaPrefs", MODE_PRIVATE);
        
        layoutNotifications.setOnClickListener(v -> {
            ivNotificationBadge.setVisibility(android.view.View.GONE);
            
            Intent intent = new Intent(FeedActivity.this, NotificationActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            startActivity(intent);
        });

        FirebaseDatabase.getInstance().getReference().child("notifications").child(currentUserId)
            .orderByChild("timestamp").limitToLast(1)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String lastSeenId = prefs.getString("last_seen_notif_id_" + currentUserId, "");
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String notifId = child.child("id").getValue(String.class);
                            if (notifId != null) {
                                latestNotificationId = notifId;
                                if (!notifId.equals(lastSeenId)) {
                                    ivNotificationBadge.setVisibility(android.view.View.VISIBLE);
                                } else {
                                    ivNotificationBadge.setVisibility(android.view.View.GONE);
                                }
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
            });

        android.widget.TextView tvMessageBadge = findViewById(R.id.ivMessageBadge);
        FirebaseDatabase.getInstance().getReference().child("chat_seen").child(currentUserId)
            .addValueEventListener(new ValueEventListener() {
                private ValueEventListener chatsListener = null;
                private com.google.firebase.database.Query chatsQuery = null;

                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    Long myLastSeen = snapshot.getValue(Long.class);
                    if (myLastSeen == null) myLastSeen = 0L;
                    
                    if (chatsQuery != null && chatsListener != null) {
                        chatsQuery.removeEventListener(chatsListener);
                    }
                    
                    chatsQuery = FirebaseDatabase.getInstance().getReference().child("chats")
                            .orderByChild("timestamp").startAt(myLastSeen + 1);
                            
                    chatsListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                            long unreadCount = 0;
                            String partnerId = currentUserId.equals("vishal") ? "rashika" : "vishal";
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String senderId = child.child("senderId").getValue(String.class);
                                if (partnerId.equals(senderId)) {
                                    unreadCount++;
                                }
                            }
                            if (unreadCount > 0) {
                                tvMessageBadge.setVisibility(android.view.View.VISIBLE);
                                tvMessageBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                            } else {
                                tvMessageBadge.setVisibility(android.view.View.GONE);
                            }
                        }
                        @Override
                        public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
                    };
                    chatsQuery.addValueEventListener(chatsListener);
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
            });

        recyclerView = findViewById(R.id.recyclerViewFeed);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        posts = new ArrayList<>();
        adapter = new FeedAdapter(this, posts, currentUserId, post -> {
            CommentsBottomSheet bottomSheet = CommentsBottomSheet.newInstance(post.id, currentUserId, currentUserName);
            bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
        });
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshFeed);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentLimit = 10;
            loadPosts();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoading) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                        isLoading = true;
                        currentLimit += 10;
                        loadPosts();
                    }
                }
            }
        });

        setupBottomNavigation();
        setupFloatingActionButton();
        loadPosts();
    }

    private void setupBottomNavigation() {
        android.widget.FrameLayout navHome = findViewById(R.id.navHome);
        android.widget.FrameLayout navProfile = findViewById(R.id.navProfile);
        android.widget.FrameLayout navFeed = findViewById(R.id.navFeed);
        
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMessages).setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        navFeed.setOnClickListener(new android.view.View.OnClickListener() {
            long lastClickTime = 0;
            @Override
            public void onClick(android.view.View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) {
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(0);
                    }
                }
                lastClickTime = clickTime;
            }
        });
    }

    private long lastNewPostLaunchTime = 0;

    private void setupFloatingActionButton() {
        newPostLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String caption = result.getData().getStringExtra("CAPTION");
                        String location = result.getData().getStringExtra("LOCATION");
                        String uriString = result.getData().getStringExtra("IMAGE_URI");
                        if (uriString != null) {
                            uploadImageToSupabase(Uri.parse(uriString), caption != null ? caption : "", location != null ? location : "");
                        }
                    }
                });

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastNewPostLaunchTime < 1000) {
                            return;
                        }
                        lastNewPostLaunchTime = currentTime;
                        Uri resultUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            Intent intent = new Intent(FeedActivity.this, NewPostActivity.class);
                            intent.putExtra("IMAGE_URI", resultUri.toString());
                            newPostLauncher.launch(intent);
                        }
                    } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
                        Throwable cropError = com.yalantis.ucrop.UCrop.getError(result.getData());
                        if (cropError != null) cropError.printStackTrace();
                        Toast.makeText(this, "Crop error", Toast.LENGTH_SHORT).show();
                    }
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            startCrop(selectedImageUri);
                        }
                    }
                });

        FloatingActionButton fab = findViewById(R.id.fabNewPost);
        fab.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNewPostLaunchTime < 1000) {
                return;
            }
            lastNewPostLaunchTime = currentTime;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image for Post"));
        });
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new java.io.File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg"));
        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setCompressionQuality(80);
        options.setToolbarTitle("Crop Image");
        options.setToolbarColor(android.graphics.Color.parseColor("#1A1A1A"));
        options.setToolbarWidgetColor(android.graphics.Color.parseColor("#FFFFFF"));
        options.setStatusBarColor(android.graphics.Color.parseColor("#000000"));
        options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF5C81"));
        options.setAspectRatioOptions(0,
                new com.yalantis.ucrop.model.AspectRatio("1:1", 1, 1),
                new com.yalantis.ucrop.model.AspectRatio("2:3", 2, 3)
        );
        Intent uCropIntent = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(this);
        cropImageLauncher.launch(uCropIntent);
    }



    private void uploadImageToSupabase(Uri imageUri, String caption, String location) {
        Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                byte[] fileBytes = getBytes(is);
                String filename = System.currentTimeMillis() + "_" + currentUserId + ".jpg";

                String uploadUrl = SUPABASE_URL + "/storage/v1/object/profile_images/" + filename; // Reusing profile_images bucket

                RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), fileBytes);
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> Toast.makeText(FeedActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/profile_images/" + filename;
                            postToFirebase(publicUrl, caption, location);
                        } else {
                            runOnUiThread(() -> Toast.makeText(FeedActivity.this, "Upload error: " + response.code(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(FeedActivity.this, "Error processing image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void postToFirebase(String imageUrl, String caption, String location) {
        String postId = postsRef.push().getKey();
        if (postId == null) return;

        FeedPost newPost = new FeedPost(postId, currentUserId, currentUserName, imageUrl, caption, location, System.currentTimeMillis(), "image");
        postsRef.child(postId).setValue(newPost).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Toast.makeText(FeedActivity.this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(0);
                    }
                    
                    String partnerId = currentUserId.equals("vishal") ? "rashika" : "vishal";
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference().child("notifications").child(partnerId);
                    String notifId = notifRef.push().getKey();
                    if (notifId != null) {
                        AppNotification notification = new AppNotification(notifId, "new_post", currentUserId, currentUserName, "uploaded a new post.", System.currentTimeMillis(), postId);
                        notifRef.child(notifId).setValue(notification);
                    }
                } else {
                    Toast.makeText(FeedActivity.this, "Failed to post to Firebase", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadPosts() {
        if (feedListener != null) {
            postsRef.removeEventListener(feedListener);
        }

        feedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                posts.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FeedPost post = data.getValue(FeedPost.class);
                    if (post != null) {
                        posts.add(post);
                    }
                }
                Collections.reverse(posts);
                adapter.notifyDataSetChanged();
                
                if (pendingScrollPostId != null && !posts.isEmpty()) {
                    for (int i = 0; i < posts.size(); i++) {
                        if (posts.get(i).id.equals(pendingScrollPostId)) {
                            final int pos = i;
                            recyclerView.post(() -> {
                                recyclerView.scrollToPosition(pos);
                                if (pendingOpenComments) {
                                    CommentsBottomSheet bottomSheet = CommentsBottomSheet.newInstance(posts.get(pos).id, currentUserId, currentUserName);
                                    bottomSheet.show(getSupportFragmentManager(), "CommentsBottomSheet");
                                    pendingOpenComments = false;
                                }
                            });
                            pendingScrollPostId = null;
                            break;
                        }
                    }
                }
                
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                isLoading = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                isLoading = false;
            }
        };

        postsRef.orderByChild("timestamp").limitToLast(currentLimit).addValueEventListener(feedListener);
    }

    private byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = is.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedListener != null) {
            postsRef.removeEventListener(feedListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String lastSeenId = getSharedPreferences("VishikaPrefs", MODE_PRIVATE).getString("last_seen_notif_id_" + currentUserId, "");
        android.view.View ivNotificationBadge = findViewById(R.id.ivNotificationBadge);
        if (!latestNotificationId.isEmpty() && !latestNotificationId.equals(lastSeenId)) {
            ivNotificationBadge.setVisibility(android.view.View.VISIBLE);
        } else {
            ivNotificationBadge.setVisibility(android.view.View.GONE);
        }
    }
}
