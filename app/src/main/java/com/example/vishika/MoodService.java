package com.example.vishika;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.media.MediaPlayer;
import java.io.IOException;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class MoodService extends Service {

    private static final String CHANNEL_ID = "MoodServiceChannel";
    private static MoodService instance;

    public static MoodService getInstance() {
        return instance;
    }
    
    private WindowManager windowManager;
    private DatabaseReference mDatabase;
    private ChildEventListener moodListener;
    private String myDbNode;
    private String partnerDbNode;
    private boolean isVishal;
    private long serviceStartTime;
    private MediaPlayer mediaPlayer;
    
    private DrawingView overlayDrawingView;
    private android.widget.Button overlayClearBtn;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child("moods");
        serviceStartTime = System.currentTimeMillis();
    
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Always enforce identity directly from the app's build configuration
        isVishal = com.example.vishika.MainActivity.IS_VISHAL;
        myDbNode = isVishal ? "vishal" : "rashika";
        partnerDbNode = isVishal ? "rashika" : "vishal";

        if (intent != null) {
            if ("PLAY_ANIMATION".equals(intent.getAction())) {
                String emoji = intent.getStringExtra("EMOJI");
                if (emoji != null) {
                    triggerRainAnimation(emoji);
                }
                return START_STICKY;
            }
        }

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Vishika Connected")
                .setContentText("Listening for partner's moods & drawings...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }

        // Delay setting online status to true to avoid race condition where the 
        // old process's onDisconnect (false) fires AFTER the new process sets true.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference().child("status").child(myDbNode).child("online");
            statusRef.setValue(true);
            statusRef.onDisconnect().setValue(false);
        }, 4000);

        listenForMoods();
        
        // Listen for new walkie-talkie voice notes from the partner
        FirebaseDatabase.getInstance().getReference().child("voice_notes").child(partnerDbNode).child("latest_url")
            .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    String url = snapshot.getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        // Show custom overlay dialog
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            showVoiceNoteOverlay();
                        });
                        
                        android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                            am.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVol, 0);
                        }
                        
                        try {
                            android.net.Uri customSound = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_tone);
                            android.media.MediaPlayer tonePlayer = new android.media.MediaPlayer();
                            tonePlayer.setDataSource(getApplicationContext(), customSound);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                tonePlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build());
                            } else {
                                tonePlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
                            }
                            tonePlayer.prepare();
                            tonePlayer.setOnCompletionListener(android.media.MediaPlayer::release);
                            tonePlayer.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        playAudioFromUrl(url);
                        // Delete after receiving to prevent replay on restart
                        snapshot.getRef().removeValue();
                    }
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
        setupOverlayDrawing();
        listenForFeedNotifications();
        listenForChats();
        listenForSounds();
        
        setDrawingOverlayVisible(true);

        return START_STICKY;
    }

    private void listenForSounds() {
        FirebaseDatabase.getInstance().getReference().child("sounds").child(myDbNode)
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                    String url = snapshot.child("url").getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        if (am != null) {
                            int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                            am.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVol, 0);
                        }
                        
                        try {
                            android.net.Uri customSound = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_tone);
                            android.media.MediaPlayer tonePlayer = new android.media.MediaPlayer();
                            tonePlayer.setDataSource(getApplicationContext(), customSound);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                tonePlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build());
                            } else {
                                tonePlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
                            }
                            tonePlayer.prepare();
                            tonePlayer.setOnCompletionListener(mp -> {
                                mp.release();
                                // Play the actual sound after the tone finishes
                                playAudioFromUrl(url);
                            });
                            tonePlayer.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Fallback if tone fails
                            playAudioFromUrl(url);
                        }

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(MoodService.this::showIncomingSoundOverlay);
                        // Delete the event so it doesn't trigger again on restart
                        snapshot.getRef().removeValue();
                    }
                }
                @Override
                public void onChildChanged(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
                @Override
                public void onChildRemoved(@androidx.annotation.NonNull DataSnapshot snapshot) {}
                @Override
                public void onChildMoved(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
            });
    }

    private void listenForChats() {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");
        android.content.SharedPreferences prefs = getSharedPreferences("vishika_prefs", MODE_PRIVATE);
        
        // Find the last known message ID
        String lastSeenId = prefs.getString("last_seen_chat_id", null);
        
        com.google.firebase.database.Query query;
        if (lastSeenId != null) {
            query = chatsRef.orderByKey().startAfter(lastSeenId);
        } else {
            // First time ever running, just listen for the newest 1 to avoid downloading history
            query = chatsRef.orderByKey().limitToLast(1);
        }
        
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    // Save this as the latest seen message
                    prefs.edit().putString("last_seen_chat_id", message.id).apply();
                    
                    // Only notify if it's from the partner and we haven't notified for this specific message before
                    if (message.senderId != null && !message.senderId.equals(myDbNode) && !prefs.getBoolean("chat_" + message.id, false)) {
                        if (!ChatActivity.isVisible) {
                            sendMissedNotification("New Message from " + (myDbNode.equals("vishal") ? "Rashika" : "Vishal"), message.text, null, "chat");
                        }
                        prefs.edit().putBoolean("chat_" + message.id, true).apply();
                    }
                }
            }
            @Override
            public void onChildChanged(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@androidx.annotation.NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
        });
    }
    
    private void listenForFeedNotifications() {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications").child(myDbNode);
                
        android.content.SharedPreferences prefs = getSharedPreferences("vishika_prefs", MODE_PRIVATE);
        
        // Find the last known notification ID
        String lastSeenNotifId = prefs.getString("last_seen_notif_id", null);
        
        com.google.firebase.database.Query query;
        if (lastSeenNotifId != null) {
            query = notificationsRef.orderByKey().startAfter(lastSeenNotifId);
        } else {
            // First time running, just listen for newest 1
            query = notificationsRef.orderByKey().limitToLast(1);
        }
        
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                AppNotification notification = snapshot.getValue(AppNotification.class);
                if (notification != null) {
                    prefs.edit().putString("last_seen_notif_id", notification.id).apply();
                    
                    if (!prefs.getBoolean("notif_" + notification.id, false)) {
                        sendMissedNotification("Vishika", notification.senderName + " " + notification.text, notification.postId, notification.type);
                        prefs.edit().putBoolean("notif_" + notification.id, true).apply();
                    }
                }
            }
            @Override
            public void onChildChanged(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@androidx.annotation.NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@androidx.annotation.NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
        });
    }
    
    private android.view.View voiceNoteScrimView = null;

    private android.view.View soundOverlayView = null;
    private android.view.View soundScrimView = null;

    private void showIncomingSoundOverlay() {
        if (soundOverlayView != null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return;
        }

        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                android.os.PowerManager.WakeLock wl = pm.newWakeLock(
                        android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
                                | android.os.PowerManager.ON_AFTER_RELEASE,
                        "Vishika:SoundWakeLock");
                wl.acquire(5000); 
            }
        } catch (Exception ignored) {}

        try {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

            int baseFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

            soundScrimView = new android.view.View(this);
            soundScrimView.setBackgroundColor(0x88000000);
            WindowManager.LayoutParams scrimParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    baseFlags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);
            scrimParams.gravity = Gravity.CENTER;
            windowManager.addView(soundScrimView, scrimParams);

            android.view.ContextThemeWrapper ctx = new android.view.ContextThemeWrapper(this, R.style.Theme_Vishika);
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(ctx);
            soundOverlayView = inflater.inflate(R.layout.dialog_custom_delivery, null);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85f),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    baseFlags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                soundOverlayView.setElevation(24f);
                soundOverlayView.setClipToOutline(true);
                soundOverlayView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 56f);
                    }
                });
            }

            android.widget.TextView txtTitle = soundOverlayView.findViewById(R.id.dialogTitle);
            android.widget.TextView txtMessage = soundOverlayView.findViewById(R.id.dialogMessage);
            android.widget.ImageView icon = soundOverlayView.findViewById(R.id.dialogIcon);
            android.view.View btnOk = soundOverlayView.findViewById(R.id.dialogBtn);

            txtTitle.setText("Sound Received 🎵");
            String name = isVishal ? "Rashika" : "Vishal";
            txtMessage.setText(name + " sent you a funny sound!");
            icon.setImageResource(R.drawable.ic_heart); // or any other icon you want
            icon.setColorFilter(android.graphics.Color.parseColor("#FF4081"));

            if (btnOk != null) {
                btnOk.setOnClickListener(v -> dismissIncomingSoundOverlay());
            }

            windowManager.addView(soundOverlayView, params);

            // Auto-dismiss after 4 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::dismissIncomingSoundOverlay, 4000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dismissIncomingSoundOverlay() {
        if (soundOverlayView != null) {
            try { windowManager.removeView(soundOverlayView); } catch (Exception ignored) {}
            soundOverlayView = null;
        }
        if (soundScrimView != null) {
            try { windowManager.removeView(soundScrimView); } catch (Exception ignored) {}
            soundScrimView = null;
        }
    }

    private void showVoiceNoteOverlay() {
        if (voiceNoteOverlayView != null) return; // already showing

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return;
        }

        // Wake the screen so user sees the dialog
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                android.os.PowerManager.WakeLock wl = pm.newWakeLock(
                        android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
                                | android.os.PowerManager.ON_AFTER_RELEASE,
                        "Vishika:VoiceNoteWakeLock");
                wl.acquire(20000); // keep screen on for 20s
            }
        } catch (Exception ignored) {}

        try {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

            int baseFlags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

            // ── Dim scrim behind dialog ──
            voiceNoteScrimView = new android.view.View(this);
            voiceNoteScrimView.setBackgroundColor(0xAA000000);
            WindowManager.LayoutParams scrimParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    baseFlags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);
            scrimParams.gravity = Gravity.CENTER;
            windowManager.addView(voiceNoteScrimView, scrimParams);

            // ── Dialog card ──
            android.view.ContextThemeWrapper ctx = new android.view.ContextThemeWrapper(this, R.style.Theme_Vishika);
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(ctx);
            voiceNoteOverlayView = inflater.inflate(R.layout.dialog_incoming_voice, null);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85f),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    baseFlags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER;

            // Round corner clip
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                voiceNoteOverlayView.setElevation(24f);
                voiceNoteOverlayView.setClipToOutline(true);
                voiceNoteOverlayView.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 56f);
                    }
                });
            }

            // Animate waveform bars
            int[] barIds = {R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5, R.id.bar6, R.id.bar7};
            for (int i = 0; i < barIds.length; i++) {
                android.view.View bar = voiceNoteOverlayView.findViewById(barIds[i]);
                if (bar != null) {
                    int delay = i * 80;
                    android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofFloat(bar, "scaleY", 1f, 2.2f, 1f);
                    anim.setDuration(600);
                    anim.setStartDelay(delay);
                    anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                    anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                    anim.start();
                }
            }

            // Sender label
            android.widget.TextView senderName = voiceNoteOverlayView.findViewById(R.id.voiceNoteSubtitle);
            String name = isVishal ? "Rashika" : "Vishal";
            if (senderName != null) senderName.setText(name + " sent you a voice note 💕");

            // Dismiss button
            android.view.View dismissBtn = voiceNoteOverlayView.findViewById(R.id.btnDismissVoice);
            if (dismissBtn != null) {
                dismissBtn.setOnClickListener(v -> dismissVoiceNoteOverlay());
            }

            windowManager.addView(voiceNoteOverlayView, params);

            // Auto-dismiss after 20 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::dismissVoiceNoteOverlay, 20000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private android.view.View voiceNoteOverlayView = null;

    private void dismissVoiceNoteOverlay() {
        if (voiceNoteOverlayView != null) {
            try { windowManager.removeView(voiceNoteOverlayView); } catch (Exception ignored) {}
            voiceNoteOverlayView = null;
        }
        if (voiceNoteScrimView != null) {
            try { windowManager.removeView(voiceNoteScrimView); } catch (Exception ignored) {}
            voiceNoteScrimView = null;
        }
    }

    private void setupOverlayDrawing() {
        if (overlayDrawingView == null) {
            overlayDrawingView = new DrawingView(this, null);
            
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT);
            
            params.gravity = Gravity.TOP | Gravity.START;
            
            windowManager.addView(overlayDrawingView, params);
            
            // This setup makes it listen to the network and draw!
            overlayDrawingView.setup(FirebaseDatabase.getInstance().getReference(), myDbNode, partnerDbNode);

            // Add floating clear button
            overlayClearBtn = new android.widget.Button(this);
            overlayClearBtn.setText("Clear Drawing");
            
            // Format button with rounded corners
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(android.graphics.Color.parseColor("#FF4B4B"));
            gd.setCornerRadius(100f);
            overlayClearBtn.setBackground(gd);
            overlayClearBtn.setTextColor(android.graphics.Color.WHITE);
            overlayClearBtn.setTextSize(16f);
            overlayClearBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            overlayClearBtn.setPadding(80, 40, 80, 40);
            // Elevation for shadow
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                overlayClearBtn.setElevation(8f);
            }
            
            overlayClearBtn.setVisibility(android.view.View.VISIBLE);
            
            overlayClearBtn.setOnClickListener(v -> {
                FirebaseDatabase.getInstance().getReference()
                    .child("drawings").child(partnerDbNode).child("strokes").removeValue();
            });

            WindowManager.LayoutParams btnParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT);
            
            // Show at bottom with margin
            btnParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            btnParams.y = 250; // padding from bottom
            
            final boolean[] isBtnAdded = {false};

            FirebaseDatabase.getInstance().getReference()
                .child("drawings").child(partnerDbNode).child("strokes")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (overlayClearBtn != null) {
                            boolean shouldShow = snapshot.exists() && snapshot.getChildrenCount() > 0;
                            if (shouldShow && !isBtnAdded[0]) {
                                try {
                                    windowManager.addView(overlayClearBtn, btnParams);
                                    isBtnAdded[0] = true;
                                } catch (Exception e) {}
                            } else if (!shouldShow && isBtnAdded[0]) {
                                try {
                                    windowManager.removeView(overlayClearBtn);
                                    isBtnAdded[0] = false;
                                } catch (Exception e) {}
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
                
            // NEW: Listen for drawing last_updated to catch offline/missed drawings
            FirebaseDatabase.getInstance().getReference()
                .child("drawings").child(partnerDbNode).child("last_updated")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    private Long lastSeenTimestamp = 0L;
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long lastUpdated = snapshot.getValue(Long.class);
                        if (lastUpdated != null && lastUpdated > lastSeenTimestamp) {
                            if (lastSeenTimestamp != 0L) {
                                // Live update while service is running
                                if (!com.example.vishika.DrawActivity.isVisible) {
                                    setDrawingOverlayVisible(true);
                                }
                            }
                            lastSeenTimestamp = lastUpdated;
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        }
    }

    private void sendMissedNotification(String title, String content) {
        sendMissedNotification(title, content, null, null);
    }

    private void sendMissedNotification(String title, String content, String postId, String type) {
        String channelId = "VishikaMissedEvents";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Missed Moods & Drawings",
                        NotificationManager.IMPORTANCE_HIGH
                );
                manager.createNotificationChannel(channel);
            }
            
            // Intent to open the app
            Intent intent = new Intent(this, postId != null ? FeedActivity.class : MainActivity.class);
            if ("chat".equals(type)) {
                intent = new Intent(this, ChatActivity.class);
            } else if (postId != null) {
                intent.putExtra("SCROLL_TO_POST", postId);
                if (type != null && (type.equals("comment") || type.equals("reply") || type.equals("like_comment"))) {
                    intent.putExtra("OPEN_COMMENTS", true);
                }
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            int pendingFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE : 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT;
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, pendingFlags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.app_logo)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            
            int notificationId = "chat".equals(type) ? 1001 : (int) System.currentTimeMillis();
            manager.notify(notificationId, builder.build());
        }
    }

    private void listenForMoods() {
        if (moodListener != null) {
            mDatabase.child(myDbNode).removeEventListener(moodListener);
        }

        moodListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // It's a new mood!
                String emoji = snapshot.child("emoji").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                
                if (emoji != null) {
                    boolean isMissed = false;
                    if (timestamp != null) {
                        long age = System.currentTimeMillis() - timestamp;
                        if (age > 60000) { // Older than 1 minute -> missed offline
                            isMissed = true;
                        }
                    }
                    
                    android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
                    boolean isScreenOff = pm != null && !pm.isInteractive();
                    if (isScreenOff) isMissed = true;

                    if (isMissed) {
                        sendMissedNotification("Missed Mood " + emoji, "Your partner sent you a mood while you were away!");
                    } else {
                        triggerRainAnimation(emoji);
                    }
                }
                // Remove the node so it doesn't pile up
                snapshot.getRef().removeValue();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mDatabase.child(myDbNode).addChildEventListener(moodListener);
    }

    private void triggerRainAnimation(String emoji) {
        // Play notification sound
        try {
            android.net.Uri customSound = android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.custom_tone);
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, customSound);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(android.media.MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Wake up screen to show mood
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            android.os.PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Vishika:MoodWakeLock"
            );
            wakeLock.acquire(4000); // Wake screen for 4 seconds
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                final FrameLayout overlay = new FrameLayout(this);
                overlay.setBackgroundColor(Color.TRANSPARENT);

                int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                        PixelFormat.TRANSLUCENT);
                
                params.gravity = Gravity.TOP | Gravity.START;

                windowManager.addView(overlay, params);

                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;

                Random random = new Random();
                int numberOfEmojis = 30 + random.nextInt(20);

                final int[] activeAnimations = {numberOfEmojis};

                for (int i = 0; i < numberOfEmojis; i++) {
                    TextView tv = new TextView(this);
                    tv.setText(emoji);
                    tv.setTextSize(30 + random.nextInt(30));
                    
                    FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    
                    float startX = random.nextInt(screenWidth);
                    tv.setX(startX);
                    tv.setY(-200);
                    
                    overlay.addView(tv, tvParams);

                    long duration = 2000 + random.nextInt(2000);
                    long delay = random.nextInt(1500);

                    ObjectAnimator fallAnim = ObjectAnimator.ofFloat(tv, "y", screenHeight + 200);
                    fallAnim.setDuration(duration);
                    fallAnim.setStartDelay(delay);
                    fallAnim.setInterpolator(new AccelerateInterpolator(0.8f));

                    float wind = -100 + random.nextInt(200);
                    ObjectAnimator windAnim = ObjectAnimator.ofFloat(tv, "x", startX + wind);
                    windAnim.setDuration(duration);
                    windAnim.setStartDelay(delay);

                    fallAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            overlay.removeView(tv);
                            activeAnimations[0]--;
                            if (activeAnimations[0] <= 0) {
                                try {
                                    windowManager.removeView(overlay);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                    fallAnim.start();
                    windAnim.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mood Rain Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartServicePendingIntent = PendingIntent.getForegroundService(
                    getApplicationContext(), 1, restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        }
        
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (moodListener != null) {
            mDatabase.child(myDbNode).removeEventListener(moodListener);
        }
        
        FirebaseDatabase.getInstance().getReference().child("status").child(myDbNode).child("online").setValue(false);
        
        // Also try to restart from onDestroy
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartServicePendingIntent = PendingIntent.getForegroundService(
                    getApplicationContext(), 1, restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        }
        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        }
    }

    public void setDrawingOverlayVisible(boolean visible) {
        if (overlayDrawingView != null) {
            overlayDrawingView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (overlayClearBtn != null) {
            overlayClearBtn.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void playAudioFromUrl(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
            } else {
                mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_ALARM);
            }
            
            // Optionally force alarm stream to max volume
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVol, 0);
            }
            
            mediaPlayer.setVolume(1.0f, 1.0f);
            
            if (url.startsWith("android.resource://")) {
                mediaPlayer.setDataSource(getApplicationContext(), android.net.Uri.parse(url));
            } else {
                mediaPlayer.setDataSource(url);
            }
            
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    android.media.audiofx.LoudnessEnhancer enhancer = new android.media.audiofx.LoudnessEnhancer(mp.getAudioSessionId());
                    enhancer.setTargetGain(2000); // 20 dB boost
                    enhancer.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mp.start();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                dismissVoiceNoteOverlay();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}