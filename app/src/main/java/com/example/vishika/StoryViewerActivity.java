package com.example.vishika;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class StoryViewerActivity extends AppCompatActivity {

    private String userId;
    private ImageView storyImageView;
    private VideoView storyVideoView;
    private LinearLayout progressContainer;
    private ImageView storyProfileImage;
    private TextView storyProfileName;
    private TextView storyTime;
    
    private List<Story> stories = new ArrayList<>();
    private int currentIndex = 0;
    
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private static final int PHOTO_DURATION_MS = 5000;
    private static final int PROGRESS_UPDATE_INTERVAL_MS = 50;
    private long currentStoryStartTime;
    private long currentDurationMs;

    private LinearLayout storyInteractionBar;
    private android.widget.EditText etStoryReply;
    private ImageView btnSendStoryReply;
    private ImageView btnStoryLike;

    private float touchStartY, touchStartX;
    private static final int SWIPE_THRESHOLD = 250;
    private boolean isSwipingDown = false;
    private View storyRootView;

    private boolean isPaused = false;
    private long pauseStartTime = 0;

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentIndex >= 0 && currentIndex < stories.size() && !isPaused) {
                long elapsed = System.currentTimeMillis() - currentStoryStartTime;
                int progress = (int) ((elapsed * 100) / currentDurationMs);
                
                ProgressBar currentBar = (ProgressBar) progressContainer.getChildAt(currentIndex);
                if (currentBar != null) {
                    currentBar.setProgress(Math.min(progress, 100));
                }
                
                if (elapsed >= currentDurationMs) {
                    nextStory();
                } else {
                    progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);
        
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            finish();
            return;
        }

        storyImageView = findViewById(R.id.storyImageView);
        storyVideoView = findViewById(R.id.storyVideoView);
        progressContainer = findViewById(R.id.progressContainer);
        storyProfileImage = findViewById(R.id.storyProfileImage);
        storyProfileName = findViewById(R.id.storyProfileName);
        storyTime = findViewById(R.id.storyTime);

        storyProfileName.setText(userId.equals("vishal") ? "Vishal" : "Rashika");
        
        storyInteractionBar = findViewById(R.id.storyInteractionBar);
        etStoryReply = findViewById(R.id.etStoryReply);
        btnSendStoryReply = findViewById(R.id.btnSendStoryReply);
        btnStoryLike = findViewById(R.id.btnStoryLike);

        String myId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        if (userId.equals(myId)) {
            storyInteractionBar.setVisibility(View.GONE);
        } else {
            storyInteractionBar.setVisibility(View.VISIBLE);
        }

        etStoryReply.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                pauseStory();
            } else {
                resumeStory();
            }
        });

        etStoryReply.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSendStoryReply.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
                btnStoryLike.setVisibility(s.toString().trim().isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnSendStoryReply.setOnClickListener(v -> sendReply());
        btnStoryLike.setOnClickListener(v -> sendLike());
        
        findViewById(R.id.btnCloseStory).setOnClickListener(v -> finish());
        
        ImageView btnDeleteStory = findViewById(R.id.btnDeleteStory);
        if (userId.equals(myId)) {
            btnDeleteStory.setVisibility(View.VISIBLE);
            btnDeleteStory.setOnClickListener(v -> {
                pauseStory();
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Story")
                    .setMessage("Are you sure you want to delete this story?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Story currentStory = stories.get(currentIndex);
                        FirebaseDatabase.getInstance().getReference("stories")
                            .child(userId).child(currentStory.getId()).removeValue();
                        stories.remove(currentIndex);
                        if (stories.isEmpty()) {
                            finish();
                        } else {
                            if (currentIndex >= stories.size()) {
                                currentIndex = stories.size() - 1;
                            }
                            setupProgressBars();
                            showStory(currentIndex);
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> resumeStory())
                    .setOnCancelListener(dialog -> resumeStory())
                    .show();
            });
        }
        
        findViewById(R.id.clickLeft).setOnClickListener(v -> prevStory());
        findViewById(R.id.clickRight).setOnClickListener(v -> {
            // Prevent going to next story if typing
            if (etStoryReply.hasFocus()) {
                etStoryReply.clearFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etStoryReply.getWindowToken(), 0);
            } else {
                nextStory();
            }
        });

        fetchProfilePicture();
        
        if (getIntent().hasExtra("STORIES_LIST")) {
            java.util.ArrayList<Story> passedStories = (java.util.ArrayList<Story>) getIntent().getSerializableExtra("STORIES_LIST");
            if (passedStories != null && !passedStories.isEmpty()) {
                stories.clear();
                stories.addAll(passedStories);
                initStoryDisplay();
            } else {
                fetchStories();
            }
        } else {
            fetchStories();
        }
    }
    
    private void fetchProfilePicture() {
        DatabaseReference profileRef = FirebaseDatabase.getInstance().getReference("profiles").child(userId);
        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                if (imgUrl != null) {
                    Glide.with(StoryViewerActivity.this).load(imgUrl).centerCrop().into(storyProfileImage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initStoryDisplay() {
        if (stories.isEmpty()) {
            finish();
        } else {
            int startIndex = 0;
            long lastSeen = getSharedPreferences("vishika_prefs", MODE_PRIVATE).getLong("lastSeenStory_" + userId, 0);
            for (int i = 0; i < stories.size(); i++) {
                if (stories.get(i).getTimestamp() > lastSeen) {
                    startIndex = i;
                    break;
                }
            }
            setupProgressBars();
            showStory(startIndex);
        }
    }

    private void fetchStories() {
        DatabaseReference storiesRef = FirebaseDatabase.getInstance().getReference("stories").child(userId);
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        
        storiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stories.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Story s = ds.getValue(Story.class);
                    if (s != null && s.getTimestamp() > twentyFourHoursAgo) {
                        stories.add(s);
                    }
                }
                
                initStoryDisplay();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupProgressBars() {
        progressContainer.removeAllViews();
        for (int i = 0; i < stories.size(); i++) {
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, 10, 1.0f);
            params.setMargins(4, 0, 4, 0);
            pb.setLayoutParams(params);
            pb.setMax(100);
            pb.setProgress(0);
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.bg_story_progress));
            progressContainer.addView(pb);
        }
    }

    private void showStory(int index) {
        if (index < 0 || index >= stories.size()) {
            finish();
            return;
        }
        
        isPaused = false;
        currentIndex = index;
        Story story = stories.get(index);

        btnStoryLike.setImageResource(R.drawable.ic_heart);
        btnStoryLike.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        
        // Update previous progress bars to 100% and future ones to 0%
        for (int i = 0; i < stories.size(); i++) {
            ProgressBar pb = (ProgressBar) progressContainer.getChildAt(i);
            if (pb != null) {
                if (i < index) pb.setProgress(100);
                else if (i > index) pb.setProgress(0);
                else pb.setProgress(0);
            }
        }
        
        android.content.SharedPreferences prefs = getSharedPreferences("vishika_prefs", MODE_PRIVATE);
        long lastSeen = prefs.getLong("lastSeenStory_" + userId, 0);
        if (story.getTimestamp() > lastSeen) {
            prefs.edit().putLong("lastSeenStory_" + userId, story.getTimestamp()).apply();
            
            // Send view notification to partner
            String myId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
            if (!userId.equals(myId)) {
                sendViewNotification(story);
            }
        }
        
        storyTime.setText(getRelativeTime(story.getTimestamp()));
        progressHandler.removeCallbacks(progressRunnable);

        if ("Video".equals(story.getType())) {
            storyImageView.setVisibility(View.GONE);
            storyVideoView.setVisibility(View.VISIBLE);
            
            storyVideoView.setVideoPath(story.getUrl());
            storyVideoView.setOnPreparedListener(mp -> {
                currentDurationMs = mp.getDuration();
                if (currentDurationMs <= 0) currentDurationMs = 15000; // Fallback
                currentStoryStartTime = System.currentTimeMillis();
                mp.start();
                progressHandler.post(progressRunnable);
            });
            storyVideoView.setOnCompletionListener(mp -> nextStory());
            storyVideoView.setOnErrorListener((mp, what, extra) -> {
                nextStory();
                return true;
            });
        } else {
            storyVideoView.setVisibility(View.GONE);
            storyImageView.setVisibility(View.VISIBLE);
            
            Glide.with(this).load(story.getUrl()).into(storyImageView);
            currentDurationMs = PHOTO_DURATION_MS;
            currentStoryStartTime = System.currentTimeMillis();
            progressHandler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL_MS);
        }
    }
    
    private void pauseStory() {
        if (!isPaused) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
            if ("Video".equals(stories.get(currentIndex).getType()) && storyVideoView.isPlaying()) {
                storyVideoView.pause();
            }
        }
    }

    private void resumeStory() {
        if (isPaused) {
            isPaused = false;
            long pausedDuration = System.currentTimeMillis() - pauseStartTime;
            currentStoryStartTime += pausedDuration;
            if ("Video".equals(stories.get(currentIndex).getType())) {
                storyVideoView.start();
            }
            progressHandler.post(progressRunnable);
        }
    }

    private void sendReply() {
        String replyText = etStoryReply.getText().toString().trim();
        if (replyText.isEmpty()) return;
        
        sendChatMessage(replyText);
        
        etStoryReply.setText("");
        etStoryReply.clearFocus();
        
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etStoryReply.getWindowToken(), 0);
        
        android.widget.Toast.makeText(this, "Sent", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void sendLike() {
        sendChatMessage("❤️ Liked your story");
        btnStoryLike.setImageResource(R.drawable.ic_heart_filled);
        btnStoryLike.setColorFilter(null);
        btnStoryLike.setImageTintList(null);
        android.widget.Toast.makeText(this, "Liked", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void sendViewNotification(Story story) {
        String myNameId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        String myDisplayName = MainActivity.IS_VISHAL ? "Vishal" : "Rashika";
        
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId);
        String notifId = notifRef.push().getKey();
        if (notifId != null) {
            AppNotification notif = new AppNotification(notifId, "view_story", myNameId, myDisplayName, "viewed your story.", System.currentTimeMillis(), story.getId());
            notifRef.child(notifId).setValue(notif);
        }
    }

    private void sendChatMessage(String text) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");
        String messageId = chatsRef.push().getKey();
        if (messageId != null) {
            Story story = stories.get(currentIndex);
            String myId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
            ChatMessage msg = new ChatMessage(messageId, myId, text, System.currentTimeMillis());
            msg.imageUrl = story.getUrl();
            msg.storyTimestamp = story.getTimestamp();
            chatsRef.child(messageId).setValue(msg);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (storyRootView == null) {
            android.view.ViewGroup content = findViewById(android.R.id.content);
            if (content != null && content.getChildCount() > 0) {
                storyRootView = content.getChildAt(0);
            }
        }
        
        switch (ev.getAction()) {
            case android.view.MotionEvent.ACTION_DOWN:
                touchStartY = ev.getY();
                touchStartX = ev.getX();
                isSwipingDown = false;
                if (!etStoryReply.hasFocus()) {
                    pauseStory();
                }
                break;
                
            case android.view.MotionEvent.ACTION_MOVE:
                float currentY = ev.getY();
                float currentX = ev.getX();
                float diffYMove = currentY - touchStartY;
                float diffXMove = Math.abs(currentX - touchStartX);
                
                // Allow swiping down visually
                if (diffYMove > 0 && diffYMove > diffXMove && storyRootView != null) {
                    isSwipingDown = true;
                    float progress = Math.min(diffYMove / (float) getResources().getDisplayMetrics().heightPixels, 1.0f);
                    storyRootView.setTranslationY(diffYMove);
                    float scale = 1.0f - (progress * 0.15f); // Scale down slightly to 85%
                    storyRootView.setScaleX(scale);
                    storyRootView.setScaleY(scale);
                }
                break;

            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                if (!etStoryReply.hasFocus()) {
                    resumeStory();
                }
                if (isSwipingDown && storyRootView != null) {
                    float touchEndY = ev.getY();
                    float diffY = touchEndY - touchStartY;
                    
                    if (diffY > SWIPE_THRESHOLD) {
                        storyRootView.animate()
                                .translationY(getResources().getDisplayMetrics().heightPixels)
                                .setDuration(250)
                                .withEndAction(this::finish)
                                .start();
                    } else {
                        // Snap back
                        storyRootView.animate()
                                .translationY(0)
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(250)
                                .start();
                    }
                    isSwipingDown = false;
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void nextStory() {
        if (currentIndex < stories.size() - 1) {
            showStory(currentIndex + 1);
        } else {
            finish();
        }
    }

    private void prevStory() {
        if (currentIndex > 0) {
            showStory(currentIndex - 1);
        } else {
            // Restart current story
            showStory(currentIndex);
        }
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return "Just now";
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
    }
}
