package com.example.vishika;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.media.MediaRecorder;
import java.io.File;
import android.view.MotionEvent;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.yalantis.ucrop.UCrop;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import android.os.CountDownTimer;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 2;
    public static final boolean IS_VISHAL = true    ;
    
    private String myDbNode;
    private String partnerDbNode;

    private FusedLocationProviderClient fusedLocationClient;
        private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    public static boolean isRecording = false;
    private DatabaseReference database;
    private LocationCallback locationCallback;
    
    private CountDownTimer birthdayTimer;
    private double myLat;
    private double myLng;
    private double partnerLat;
    private double partnerLng;
    private long partnerTimestamp = 0;

    // Locket (Daily Photo)
    private ActivityResultLauncher<String> locketImagePickerLauncher;
    private ActivityResultLauncher<android.content.Intent> locketCropImageLauncher;
    private String myLocketNode;  // e.g. "locket/vishal"
    private String partnerLocketNode;
    private String currentMyLocketUrl = null;
    private Long myLocketExpiry = null;
    private Long partnerLocketExpiry = null;
    private String myLocketUrl = null;
    private String partnerLocketUrl = null;
    private android.os.Handler locketTimerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable locketTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateLocketTimers();
            locketTimerHandler.postDelayed(this, 60000); // Update every minute
        }
    };
    
    private String myImageUrl = null;
    private android.graphics.Bitmap myProfileBitmap = null;
    private String partnerImageUrl = null;
    private Bitmap partnerProfileBitmap = null;

    private TextView textDistanceValue;
    private TextView textLocationStatus;
    
    private GoogleMap mMap;
    private Marker myMarker;
    private Marker partnerMarker;
    private Polyline connectionLine;

    private ActivityResultLauncher<Intent> updateApkPickerLauncher;
    private ActivityResultLauncher<Intent> surpriseMediaPickerLauncher;
    private ActivityResultLauncher<Intent> storyMediaPickerLauncher;
    private ActivityResultLauncher<Intent> extractAudioPickerLauncher;
    private String pendingSurpriseType;
    private boolean isPartnerOnline = false;
    
    private List<Story> myStoriesList = new java.util.ArrayList<>();
    private List<Story> partnerStoriesList = new java.util.ArrayList<>();

    private void uploadSurpriseMediaToCloudinary(android.net.Uri mediaUri, String type) {

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Uploading " + type + "...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            try {
                // Get file size for progress
                long totalSize = 0;
                try (android.database.Cursor cursor = getContentResolver().query(mediaUri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1) {
                            totalSize = cursor.getLong(sizeIndex);
                        }
                    }
                }
                if (totalSize == 0) {
                    // Fallback to InputStream available
                    java.io.InputStream tempIs = getContentResolver().openInputStream(mediaUri);
                    if (tempIs != null) {
                        totalSize = tempIs.available();
                        tempIs.close();
                    }
                }
                final long finalTotalSize = totalSize;
                
                String cloudName = "q7btslrv";
                String uploadPreset = "vishika_uploads";
                String urlString = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
                
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                // Enable streaming mode to prevent OOM
                if (finalTotalSize > 0) {
                    // Approximate total length: file size + multipart overhead
                    // For safety, let HttpURLConnection chunk it
                    conn.setChunkedStreamingMode(4096);
                }
                
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                
                java.io.OutputStream os = conn.getOutputStream();
                java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(os, "UTF-8"), true);
                
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                writer.append(uploadPreset).append("\r\n");
                
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"media\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n\r\n");
                writer.flush();
                
                java.io.InputStream is = getContentResolver().openInputStream(mediaUri);
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalRead = 0;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    if (finalTotalSize > 0) {
                        int progress = (int) ((totalRead * 100) / finalTotalSize);
                        runOnUiThread(() -> progressDialog.setProgress(progress));
                    }
                }
                os.flush();
                is.close();
                
                writer.append("\r\n").append("--").append(boundary).append("--\r\n");
                writer.close();
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                    String secureUrl = jsonResponse.getString("secure_url");
                    
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        
                        long timestamp = System.currentTimeMillis();
                        String partnerName = IS_VISHAL ? "rashika" : "vishal";
                        
                        if ("Story".equals(type)) {
                            // Save to stories node
                            String sender = IS_VISHAL ? "vishal" : "rashika";
                            DatabaseReference storyRef = FirebaseDatabase.getInstance().getReference("stories").child(sender);
                            String storyId = storyRef.push().getKey();
                            if (storyId != null) {
                                Story story = new Story(storyId, secureUrl, pendingSurpriseType, timestamp);
                                storyRef.child(storyId).setValue(story);
                            }
                            android.widget.Toast.makeText(MainActivity.this, "Story posted!", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            String sender = IS_VISHAL ? "vishal" : "rashika";
                            
                            // Send to partner
                            DatabaseReference surpriseRef = FirebaseDatabase.getInstance().getReference("surprise_box").child(partnerName);
                            java.util.Map<String, Object> boxData = new java.util.HashMap<>();
                            boxData.put("timestamp", timestamp);
                            boxData.put("sender", sender);
                            boxData.put("type", type);
                            boxData.put("content", secureUrl);
                            surpriseRef.push().setValue(boxData);
                            
                            android.widget.Toast.makeText(MainActivity.this, "Surprise Box sent! 🎁", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        android.widget.Toast.makeText(MainActivity.this, "Upload failed: " + responseCode, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    android.widget.Toast.makeText(MainActivity.this, "Upload error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long lastBackPress = 0;
            @Override
            public void handleOnBackPressed() {
                long now = System.currentTimeMillis();
                if (now - lastBackPress < 2000) {
                    finish();
                } else {
                    lastBackPress = now;
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }
        });
        myDbNode = IS_VISHAL ? "locations/vishal" : "locations/rashika";
        partnerDbNode = IS_VISHAL ? "locations/rashika" : "locations/vishal";
        myLat = IS_VISHAL ? 31.663744 : 25.426241;
        myLng = IS_VISHAL ? 74.928336 : 81.820452;
        partnerLat = IS_VISHAL ? 25.426241 : 31.663744;
        partnerLng = IS_VISHAL ? 81.820452 : 74.928336;
        
        setContentView(R.layout.activity_main);
        
        // Check for OTA updates
        AutoUpdateHelper.checkForUpdates(this);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
        
        locketTimerHandler.post(locketTimerRunnable);

        textDistanceValue = findViewById(R.id.textDistanceValue);
        textLocationStatus = findViewById(R.id.textLocationStatus);

        FrameLayout navProfile = findViewById(R.id.navProfile);
        android.view.View btnRefreshMap = findViewById(R.id.btnRefreshMap);

        android.view.View.OnClickListener openDrawActivity = v -> {
            Intent intent = new Intent(MainActivity.this, DrawActivity.class);
            startActivity(intent);
        };


        FrameLayout navFeed = findViewById(R.id.navFeed);
        FrameLayout navHome = findViewById(R.id.navHome);
        
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshDashboard = findViewById(R.id.swipeRefreshDashboard);
        swipeRefreshDashboard.setOnRefreshListener(() -> {
            new android.os.Handler().postDelayed(() -> {
                swipeRefreshDashboard.setRefreshing(false);
            }, 1000);
        });
        
        navHome.setOnClickListener(new android.view.View.OnClickListener() {
            long lastClickTime = 0;
            @Override
            public void onClick(android.view.View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) {
                    android.widget.ScrollView mainScrollView = findViewById(R.id.mainScrollView);
                    if (mainScrollView != null) {
                        mainScrollView.smoothScrollTo(0, 0);
                    }
                }
                lastClickTime = clickTime;
            }
        });
        
        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });
        
        setupStories();
        
        navFeed.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        btnRefreshMap.setOnClickListener(v -> {
            Toast.makeText(this, "Updating map...", Toast.LENGTH_SHORT).show();
            updateDistanceUI();
        });
        
        btnRefreshMap.setOnLongClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LocationHistoryActivity.class));
            return true;
        });
        
        ImageView btnLocationHistory = findViewById(R.id.btnLocationHistory);
        if (btnLocationHistory != null) {
            btnLocationHistory.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, LocationHistoryActivity.class));
            });
        }
        
        if (textLocationStatus != null) {
            textLocationStatus.setOnLongClickListener(v -> {
                startActivity(new Intent(MainActivity.this, LocationHistoryActivity.class));
                return true;
            });
        }

        updateApkPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            showUploadUpdateDialog(uri);
                        }
                    }
                }
        );
        
        storyMediaPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            uploadSurpriseMediaToCloudinary(uri, "Story");
                        }
                    }
                }
        );

        surpriseMediaPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri mediaUri = result.getData().getData();
                    if (mediaUri != null && pendingSurpriseType != null) {
                        uploadSurpriseMediaToCloudinary(mediaUri, pendingSurpriseType);
                    }
                }
            }
        );

        extractAudioPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri videoUri = result.getData().getData();
                    if (videoUri != null) {
                        processExtractedAudio(videoUri);
                    }
                }
            }
        );

        android.widget.ImageView appLogo = findViewById(R.id.appLogo);
        if (appLogo != null) {
            appLogo.setOnLongClickListener(v -> {
                if (IS_VISHAL) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/vnd.android.package-archive");
                    updateApkPickerLauncher.launch(intent);
                    return true;
                }
                return false;
            });
        }

        database = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        android.view.View btnDrawForPartner = findViewById(R.id.btnDrawForPartner);
        if (btnDrawForPartner != null) {
            btnDrawForPartner.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, DrawActivity.class);
                intent.putExtra("IS_VISHAL", IS_VISHAL);
                startActivity(intent);
            });
        }
        
        android.view.View btnSendSurprise = findViewById(R.id.btnSendSurprise);
        if (btnSendSurprise != null) {
            btnSendSurprise.setOnClickListener(v -> {
                String[] options = {"📷 Send Photo", "📹 Send Video", "🎤 Send Voice Note"};
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Pack your Surprise Box")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            pendingSurpriseType = "Photo";
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            surpriseMediaPickerLauncher.launch(intent);
                        } else if (which == 1) {
                            pendingSurpriseType = "Video";
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("video/*");
                            surpriseMediaPickerLauncher.launch(intent);
                        } else if (which == 2) {
                            showSurpriseVoiceDialog();
                        }
                    })
                    .show();
            });
        }
        
        android.view.View btnGallery = findViewById(R.id.btnGallery);
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                showGalleryPinDialog();
            });
        }
        
        
        setupCulturePreview();

        setupMoodListeners();
        setupSoundListeners();
        setupOnlineStatusListener();

        // Handle Streak Logic
        handleStreakLogic();

        // Locket (Daily Photo) setup
        myLocketNode    = IS_VISHAL ? "locket/vishal"  : "locket/rashika";
        partnerLocketNode = IS_VISHAL ? "locket/rashika" : "locket/vishal";
        setupLocketImagePicker();
        setupLocketListeners();


        android.view.View btnPushToTalk = findViewById(R.id.btnPushToTalk);
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/walkietalkie.m4a";

        final long[] recordingStartTime = {0};
        if (btnPushToTalk != null) {
            btnPushToTalk.setOnTouchListener((v, event) -> {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
                    return false;
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isRecording) {
                            startRecording();
                            recordingStartTime[0] = System.currentTimeMillis();
                            v.setPressed(true);
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null) vibrator.vibrate(50);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isRecording) {
                            long duration = System.currentTimeMillis() - recordingStartTime[0];
                            if (duration < 1000) {
                                // Accidental press, cancel recording
                                cancelRecording();
                                Toast.makeText(this, "Hold longer to record voice note", Toast.LENGTH_SHORT).show();
                            } else {
                                stopRecordingAndUpload();
                            }
                            v.setPressed(false);
                        }
                        return true;
                }
                return true;
            });
        }

        checkOverlayPermissionAndStartService();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        setupPartnerLocationListener();
        setupPartnerProfileListener();
        checkPermissionsAndStart();
        calculateTogetherSince();
    }
    
    private void showGalleryPinDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_pin_entry, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        
        android.widget.EditText editPin = view.findViewById(R.id.editPin);
        android.widget.TextView textPinMask = view.findViewById(R.id.textPinMask);
        android.widget.Button btnUnlock = view.findViewById(R.id.btnUnlock);
        
        editPin.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                StringBuilder mask = new StringBuilder();
                int len = s.length();
                for (int i = 0; i < 4; i++) {
                    if (i > 0) mask.append(" ");
                    if (i < len) {
                        mask.append("❤");
                    } else {
                        mask.append("-");
                    }
                }
                textPinMask.setText(mask.toString());
                
                if (len == 4) {
                    btnUnlock.performClick();
                }
            }
        });
        
        btnUnlock.setOnClickListener(v -> {
            if ("2506".equals(editPin.getText().toString())) {
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this, GalleryActivity.class));
            } else {
                android.widget.Toast.makeText(MainActivity.this, "Incorrect PIN!", android.widget.Toast.LENGTH_SHORT).show();
                editPin.setText("");
                
                String partnerNode = IS_VISHAL ? "rashika" : "vishal";
                String myName = IS_VISHAL ? "Vishal" : "Rashika";
                String notifId = database.child("notifications").child(partnerNode).push().getKey();
                if (notifId != null) {
                    AppNotification notif = new AppNotification(
                            notifId,
                            "system",
                            IS_VISHAL ? "vishal" : "rashika",
                            myName,
                            "tried to open the gallery with a wrong PIN! 😱",
                            System.currentTimeMillis(),
                            ""
                    );
                    database.child("notifications").child(partnerNode).child(notifId).setValue(notif);
                }
                
                editPin.setText("");
            }
        });
        
        dialog.show();
        editPin.requestFocus();
    }
    
    private void showSurpriseVoiceDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Record Voice Note");
        builder.setMessage("Tap Start to begin recording.");
        
        final String voicePath = getExternalCacheDir().getAbsolutePath() + "/surprise_voice.m4a";
        final android.media.MediaRecorder[] recorder = new android.media.MediaRecorder[1];
        
        builder.setPositiveButton("Start", null); 
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (recorder[0] != null) {
                try { recorder[0].stop(); } catch (Exception e) {}
                recorder[0].release();
            }
        });
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        android.widget.Button btnStart = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        btnStart.setOnClickListener(new android.view.View.OnClickListener() {
            boolean recording = false;
            @Override
            public void onClick(android.view.View v) {
                if (!recording) {
                    if (androidx.core.app.ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        androidx.core.app.ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);
                        return;
                    }
                    try {
                        recorder[0] = new android.media.MediaRecorder();
                        recorder[0].setAudioSource(android.media.MediaRecorder.AudioSource.MIC);
                        recorder[0].setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4);
                        recorder[0].setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC);
                        recorder[0].setOutputFile(voicePath);
                        recorder[0].prepare();
                        recorder[0].start();
                        recording = true;
                        btnStart.setText("Stop & Send");
                        dialog.setMessage("Recording...");
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try { recorder[0].stop(); } catch (Exception e) {}
                    recorder[0].release();
                    dialog.dismiss();
                    uploadSurpriseMediaToCloudinary(android.net.Uri.fromFile(new java.io.File(voicePath)), "Voice");
                }
            }
        });
    }
    

    
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAllGesturesEnabled(false); // Make it look like a static widget widget
        mMap.getUiSettings().setZoomControlsEnabled(false);
        updateMapUI();
    }

    private void checkPermissionsAndStart() {
        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateDistanceMeters(10) // Update every 10 meters
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    myLat = location.getLatitude();
                    myLng = location.getLongitude();
                    
                    // Sync my location to Firebase
                    database.child(myDbNode).child("lat").setValue(myLat);
                    database.child(myDbNode).child("lng").setValue(myLng);
                    long timestamp = System.currentTimeMillis();
                    database.child(myDbNode).child("timestamp").setValue(timestamp);
                    
                    // Push to location history
                    String historyNode = IS_VISHAL ? "location_history/vishal" : "location_history/rashika";
                    java.util.Map<String, Object> locData = new java.util.HashMap<>();
                    locData.put("lat", myLat);
                    locData.put("lng", myLng);
                    locData.put("timestamp", timestamp);
                    database.child(historyNode).push().setValue(locData);
                    
                    updateDistanceUI();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void setupPartnerLocationListener() {
        database.child(partnerDbNode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double lat = snapshot.child("lat").getValue(Double.class);
                    Double lng = snapshot.child("lng").getValue(Double.class);
                    Long ts = snapshot.child("timestamp").getValue(Long.class);
                    if (ts != null) partnerTimestamp = ts;
                    if (lat != null && lng != null) {
                        partnerLat = lat;
                        partnerLng = lng;
                        updateDistanceUI();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupPartnerProfileListener() {
        String partnerProfileNode = IS_VISHAL ? "rashika" : "vishal";
        
        database.getRoot().child("profiles").child(IS_VISHAL ? "vishal" : "rashika").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String url = snapshot.child("imageUrl").getValue(String.class);
                    if (url != null && !url.equals(myImageUrl)) {
                        myImageUrl = url;
                        Glide.with(MainActivity.this)
                            .asBitmap()
                            .load(url)
                            .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .into(new CustomTarget<android.graphics.Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable Transition<? super android.graphics.Bitmap> transition) {
                                    myProfileBitmap = resource;
                                    updateMapUI();
                                }
                                public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                            });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        database.getRoot().child("profiles").child(partnerProfileNode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String url = snapshot.child("imageUrl").getValue(String.class);
                    if (url != null && !url.equals(partnerImageUrl)) {
                        partnerImageUrl = url;
                        Glide.with(MainActivity.this)
                            .asBitmap()
                            .load(url)
                            .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @androidx.annotation.Nullable Transition<? super Bitmap> transition) {
                                    partnerProfileBitmap = resource;
                                    updateMapUI();
                                }
                                @Override
                                public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                            });
                    }
                    
                    String dobStr = snapshot.child("dob").getValue(String.class);
                    if (dobStr != null) {
                        calculateAndShowBirthdayCountdown(dobStr);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDistanceUI() {
        float[] results = new float[1];
        Location.distanceBetween(myLat, myLng, partnerLat, partnerLng, results);
        int distanceInKm = Math.round(results[0] / 1000f);
        textDistanceValue.setText("Distance apart: " + distanceInKm + " km");
        
        // Push update to widget
        String widgetText = distanceInKm + " km apart";
        android.appwidget.AppWidgetManager appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this);
        android.content.ComponentName thisWidget = new android.content.ComponentName(this, DistanceWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            DistanceWidgetProvider.updateAppWidgetWithMap(this, appWidgetManager, appWidgetId, widgetText, null);
        }
        
        String partnerName = IS_VISHAL ? "Rashika" : "Vishal";
        
        if (partnerTimestamp == 0) {
            textLocationStatus.setText("Waiting for " + partnerName + " to share location...");
            textLocationStatus.setTextColor(android.graphics.Color.parseColor("#555555"));
        } else {
            String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(partnerTimestamp));
            if (!isPartnerOnline) {
                textLocationStatus.setText(partnerName + " is offline (Last seen at " + timeStr + ")");
                textLocationStatus.setTextColor(android.graphics.Color.parseColor("#888888")); // Subtle gray color
            } else {
                textLocationStatus.setText("Locating " + partnerName + "...");
                textLocationStatus.setTextColor(android.graphics.Color.parseColor("#555555"));
            }

            // Get city name using Geocoder asynchronously
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(partnerLat, partnerLng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        
                        String addressLine = addr.getAddressLine(0);
                        String locationStr = "Unknown area";
                        
                        if (addressLine != null && !addressLine.isEmpty()) {
                            locationStr = addressLine;
                        } else {
                            String feature = addr.getFeatureName();
                            String subLocality = addr.getSubLocality();
                            String locality = addr.getLocality();
                            if (locality == null) locality = addr.getSubAdminArea();
                            String state = addr.getAdminArea();
                            
                            StringBuilder locationBuilder = new StringBuilder();
                            if (feature != null && !feature.contains("+") && !feature.matches("^[0-9\\-]+$") && !feature.equals(locality) && !feature.equals(subLocality) && !feature.equals(state)) {
                                locationBuilder.append(feature).append(", ");
                            } else if (subLocality != null && !subLocality.equals(locality)) {
                                locationBuilder.append(subLocality).append(", ");
                            }
                            
                            if (locality != null) {
                                locationBuilder.append(locality);
                            } else if (state != null) {
                                locationBuilder.append(state);
                            }
                            
                            if (locationBuilder.length() > 0) {
                                locationStr = locationBuilder.toString();
                            }
                        }
                        
                        final String finalText;
                        final int textColor;
                        if (!isPartnerOnline) {
                            finalText = partnerName + " was at " + locationStr + " before going offline (Last seen at " + timeStr + ")";
                            textColor = android.graphics.Color.parseColor("#888888");
                        } else {
                            finalText = partnerName + " is at " + locationStr + " since " + timeStr;
                            textColor = android.graphics.Color.parseColor("#555555");
                        }
                        
                        runOnUiThread(() -> {
                            textLocationStatus.setText(finalText);
                            textLocationStatus.setTextColor(textColor);
                        });
                    }
                } catch (Exception e) {
                    // Ignore geocoder errors
                }
            }).start();
        }
        
        updateMapUI();
    }
    
    private BitmapDescriptor getCustomMarker(android.graphics.Bitmap profileBitmap) {
        if (profileBitmap == null) {
            return bitmapDescriptorFromVector(R.drawable.ic_heart_filled);
        }
        
        View customMarkerView = ((LayoutInflater) getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker, null);
        ImageView markerImage = customMarkerView.findViewById(R.id.markerImage);
        androidx.cardview.widget.CardView markerCard = customMarkerView.findViewById(R.id.markerCard);
        
        markerCard.setVisibility(View.VISIBLE);
        markerImage.setImageBitmap(profileBitmap);
        
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        
        android.graphics.Bitmap returnedBitmap = android.graphics.Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        customMarkerView.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(returnedBitmap);
    }
    
    private void updateMapUI() {
        if (mMap == null) return;
        
        LatLng me = new LatLng(myLat, myLng);
        LatLng partner = new LatLng(partnerLat, partnerLng);
        
        BitmapDescriptor myIcon = getCustomMarker(myProfileBitmap);
        BitmapDescriptor partnerIcon = getCustomMarker(partnerProfileBitmap);
        
        if (myMarker == null) {
            myMarker = mMap.addMarker(new MarkerOptions().position(me).title(IS_VISHAL ? "Me" : "Rashika").icon(myIcon));
        } else {
            myMarker.setPosition(me);
            myMarker.setIcon(myIcon);
        }
        
        if (partnerMarker == null) {
            partnerMarker = mMap.addMarker(new MarkerOptions().position(partner).title(IS_VISHAL ? "Rashika" : "Me").icon(partnerIcon));
        } else {
            partnerMarker.setPosition(partner);
            partnerMarker.setIcon(partnerIcon);
        }
        
        // Remove polyline if it exists since we are focusing just on the partner
        if (connectionLine != null) {
            connectionLine.remove();
            connectionLine = null;
        }
        
        // Center the camera on the partner with a nice close zoom level (e.g., 15f)
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(partner, 15f), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    takeMapSnapshotForWidget();
                }

                @Override
                public void onCancel() {
                    takeMapSnapshotForWidget();
                }
            });
        } catch (Exception e) {
            // Layout not yet complete, ignore
        }
    }
    
    private void takeMapSnapshotForWidget() {
        if (mMap == null) return;
        mMap.snapshot(bitmap -> {
            if (bitmap != null) {
                float[] results = new float[1];
                Location.distanceBetween(myLat, myLng, partnerLat, partnerLng, results);
                int distanceInKm = Math.round(results[0] / 1000f);
                String widgetText = distanceInKm + " km apart";
                
                android.appwidget.AppWidgetManager awm = android.appwidget.AppWidgetManager.getInstance(MainActivity.this);
                android.content.ComponentName cn = new android.content.ComponentName(MainActivity.this, DistanceWidgetProvider.class);
                int[] ids = awm.getAppWidgetIds(cn);
                for (int id : ids) {
                    DistanceWidgetProvider.updateAppWidgetWithMap(MainActivity.this, awm, id, widgetText, bitmap);
                }
            }
        });
    }
    
    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        android.graphics.drawable.Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return null;
        
        // Scale up the heart slightly so it's clearly visible on the map
        int width = (int) (vectorDrawable.getIntrinsicWidth() * 1.5f);
        int height = (int) (vectorDrawable.getIntrinsicHeight() * 1.5f);
        
        vectorDrawable.setBounds(0, 0, width, height);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required for real-time distance!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locketTimerHandler.removeCallbacks(locketTimerRunnable);
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (birthdayTimer != null) birthdayTimer.cancel();
    }

    private void setupMoodListeners() {
        String partnerName = IS_VISHAL ? "rashika" : "vishal";
        
        findViewById(R.id.moodHeart).setOnClickListener(v -> sendMood(partnerName, "❤️"));
        findViewById(R.id.moodPlead).setOnClickListener(v -> sendMood(partnerName, "🥺"));
        findViewById(R.id.moodKiss).setOnClickListener(v -> sendMood(partnerName, "😘"));
        findViewById(R.id.moodBite).setOnClickListener(v -> sendMood(partnerName, "🫦"));
        findViewById(R.id.moodAngry).setOnClickListener(v -> sendMood(partnerName, "😡"));
        findViewById(R.id.moodHug).setOnClickListener(v -> sendMood(partnerName, "🫂"));
    }
    
    private void setupOnlineStatusListener() {
        String myDbNode = IS_VISHAL ? "vishal" : "rashika";
        DatabaseReference connectedRef = database.getRoot().child(".info/connected");
        DatabaseReference myStatusRef = database.child("status").child(myDbNode).child("online");
        
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (connected) {
                    myStatusRef.onDisconnect().setValue(false);
                    myStatusRef.setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        String partnerDbNode = IS_VISHAL ? "rashika" : "vishal";
        String partnerDisplayName = IS_VISHAL ? "Rashika" : "Vishal";
        DatabaseReference statusRef = database.child("status").child(partnerDbNode).child("online");
        
        android.view.View statusDot = findViewById(R.id.statusDot);
        TextView statusText = findViewById(R.id.statusText);
        
        if (statusDot == null || statusText == null) return;
        
        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.getValue(Boolean.class);
                boolean online = isOnline != null && isOnline;
                isPartnerOnline = online;
                
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                gd.setColor(online ? android.graphics.Color.parseColor("#4CAF50") : android.graphics.Color.parseColor("#F44336"));
                statusDot.setBackground(gd);
                
                statusText.setText(online ? partnerDisplayName + " is Online" : partnerDisplayName + " is Offline");
                updateDistanceUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupSoundListeners() {
        String partnerName = IS_VISHAL ? "rashika" : "vishal";
        String pkg = getPackageName();
        
        findViewById(R.id.soundBtn1).setOnClickListener(v -> sendSound(partnerName, "android.resource://" + pkg + "/" + R.raw.audio_1));
        findViewById(R.id.soundBtn2).setOnClickListener(v -> sendSound(partnerName, "android.resource://" + pkg + "/" + R.raw.audio_2));
        findViewById(R.id.soundBtn3).setOnClickListener(v -> sendSound(partnerName, "android.resource://" + pkg + "/" + R.raw.audio_3));
        
        android.view.View extractBtn = findViewById(R.id.soundBtnExtract);
        if (extractBtn != null) {
            extractBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                extractAudioPickerLauncher.launch(intent);
            });
        }
    }

    private void processExtractedAudio(android.net.Uri videoUri) {
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setMessage("Extracting Audio...");
        progress.setCancelable(false);
        progress.show();

        String outPath = getExternalCacheDir().getAbsolutePath() + "/extracted_audio.m4a";

        new Thread(() -> {
            try {
                android.media.MediaExtractor extractor = new android.media.MediaExtractor();
                extractor.setDataSource(this, videoUri, null);

                int audioTrackIndex = -1;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    android.media.MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(android.media.MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        audioTrackIndex = i;
                        break;
                    }
                }

                if (audioTrackIndex == -1) {
                    extractor.release();
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(MainActivity.this, "No audio track found in video", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                extractor.selectTrack(audioTrackIndex);
                android.media.MediaFormat format = extractor.getTrackFormat(audioTrackIndex);
                
                android.media.MediaMuxer muxer = new android.media.MediaMuxer(outPath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int writeTrackIndex = muxer.addTrack(format);
                muxer.start();

                int bufferSize = 256 * 1024;
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(bufferSize);
                android.media.MediaCodec.BufferInfo bufferInfo = new android.media.MediaCodec.BufferInfo();

                while (true) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0);
                    if (bufferInfo.size < 0) {
                        break;
                    }
                    bufferInfo.presentationTimeUs = extractor.getSampleTime();
                    bufferInfo.flags = extractor.getSampleFlags();

                    muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo);
                    extractor.advance();
                }

                muxer.stop();
                muxer.release();
                extractor.release();

                runOnUiThread(() -> {
                    progress.dismiss();
                    showAudioPreviewDialog(outPath);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(MainActivity.this, "Extraction failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void showAudioPreviewDialog(String audioPath) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Preview Sound");
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        layout.setGravity(android.view.Gravity.CENTER);
        
        android.widget.Button btnPlay = new android.widget.Button(this);
        btnPlay.setText("▶ Play Preview");
        layout.addView(btnPlay);
        
        builder.setView(layout);
        
        final android.media.MediaPlayer[] player = new android.media.MediaPlayer[1];
        
        btnPlay.setOnClickListener(v -> {
            try {
                if (player[0] != null) {
                    if (player[0].isPlaying()) {
                        player[0].stop();
                        btnPlay.setText("▶ Play Preview");
                    } else {
                        player[0].release();
                        player[0] = null;
                    }
                }
                
                if (player[0] == null) {
                    player[0] = new android.media.MediaPlayer();
                    player[0].setDataSource(audioPath);
                    player[0].prepare();
                    player[0].start();
                    btnPlay.setText("⏹ Stop Preview");
                    player[0].setOnCompletionListener(mp -> {
                        btnPlay.setText("▶ Play Preview");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        builder.setPositiveButton("Send", (dialog, which) -> {
            if (player[0] != null) {
                try { player[0].stop(); } catch (Exception e) {}
                player[0].release();
            }
            uploadSoundToSupabase(android.net.Uri.fromFile(new java.io.File(audioPath)));
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (player[0] != null) {
                try { player[0].stop(); } catch (Exception e) {}
                player[0].release();
            }
        });
        
        android.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            if (player[0] != null) {
                try { player[0].stop(); } catch (Exception e) {}
                player[0].release();
                player[0] = null;
            }
        });
        dialog.show();
    }
    
    private void uploadSoundToSupabase(android.net.Uri audioUri) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Uploading Sound...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            try {
                java.io.File file = new java.io.File(audioUri.getPath());
                String filename = "custom_sound_" + System.currentTimeMillis() + ".m4a";
                String bucket = "profile_images"; 
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + filename;
                
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.RequestBody body = okhttp3.RequestBody.create(file, okhttp3.MediaType.parse("audio/mp4"));
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(uploadUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Content-Type", "audio/mp4")
                        .build();
                        
                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NonNull okhttp3.Call call, @NonNull java.io.IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws java.io.IOException {
                        if (response.isSuccessful()) {
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + filename;
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                sendExtractedSound(IS_VISHAL ? "rashika" : "vishal", publicUrl, filename);
                            });
                        } else {
                            String err = response.body() != null ? response.body().string() : "Unknown error";
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Upload failed: " + response.code() + " " + err, Toast.LENGTH_LONG).show();
                            });
                        }
                        response.close();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void sendExtractedSound(String partnerName, String url, String filename) {
        String soundId = database.child("sounds").child(partnerName).push().getKey();
        if (soundId != null) {
            database.child("sounds").child(partnerName).child(soundId).child("url").setValue(url);
            database.child("sounds").child(partnerName).child(soundId).child("timestamp").setValue(System.currentTimeMillis());
            if (filename != null && !filename.isEmpty()) {
                database.child("sounds").child(partnerName).child(soundId).child("delete_token").setValue(filename);
            }
            
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }

            final DatabaseReference soundRef = database.child("sounds").child(partnerName).child(soundId);
            final boolean[] delivered = {false};
            final ValueEventListener deliveryListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        delivered[0] = true;
                        soundRef.removeEventListener(this);
                        showDeliveryDialog("Delivered 🎵", "Your custom sound reached " + (IS_VISHAL ? "Rashika" : "Vishal") + " successfully.", true);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            soundRef.addValueEventListener(deliveryListener);
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!delivered[0]) {
                    soundRef.removeEventListener(deliveryListener);
                    soundRef.removeValue();
                    
                    if (filename != null && !filename.isEmpty()) {
                        deleteSupabaseFile(filename);
                    }
                    
                    if (!isFinishing() && !isDestroyed()) {
                        showDeliveryDialog("Not Delivered", (IS_VISHAL ? "Rashika" : "Vishal") + " is offline. The custom sound was deleted.", false);
                    }
                }
            }, 10000);
        }
    }

    private void deleteSupabaseFile(String filename) {
        new Thread(() -> {
            try {
                String bucket = "profile_images";
                String urlString = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + filename;
                
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(urlString)
                        .delete()
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();
                        
                client.newCall(request).execute().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendSound(String partnerName, String url) {
        String soundId = database.child("sounds").child(partnerName).push().getKey();
        if (soundId != null) {
            database.child("sounds").child(partnerName).child(soundId).child("url").setValue(url);
            database.child("sounds").child(partnerName).child(soundId).child("timestamp").setValue(System.currentTimeMillis());
            
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }

            // Setup delivery receipt listener
            final DatabaseReference soundRef = database.child("sounds").child(partnerName).child(soundId);
            final boolean[] delivered = {false};
            final ValueEventListener deliveryListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        // Node was deleted by partner (delivered)
                        delivered[0] = true;
                        soundRef.removeEventListener(this);
                        showDeliveryDialog("Delivered 🎵", "Your sound reached " + (IS_VISHAL ? "Rashika" : "Vishal") + " successfully.", true);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            soundRef.addValueEventListener(deliveryListener);
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!delivered[0]) {
                    soundRef.removeEventListener(deliveryListener);
                    // Check if activity is still running before showing dialog
                    if (!isFinishing() && !isDestroyed()) {
                        showDeliveryDialog("Not Delivered", (IS_VISHAL ? "Rashika" : "Vishal") + " appears to be offline right now. Try again in a bit!", false);
                    }
                }
            }, 10000); // 10 second timeout for sounds
        }
    }
    
    private void sendMood(String partnerName, String emoji) {
        String moodId = database.child("moods").child(partnerName).push().getKey();
        if (moodId != null) {
            database.child("moods").child(partnerName).child(moodId).child("emoji").setValue(emoji);
            database.child("moods").child(partnerName).child(moodId).child("timestamp").setValue(System.currentTimeMillis());
            
            // Play animation locally
            Intent playIntent = new Intent(this, MoodService.class);
            playIntent.setAction("PLAY_ANIMATION");
            playIntent.putExtra("EMOJI", emoji);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(playIntent);
            } else {
                startService(playIntent);
            }
            
            // Add a mild vibration
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }

            // Setup delivery receipt listener
            final DatabaseReference moodRef = database.child("moods").child(partnerName).child(moodId);
            final boolean[] delivered = {false};
            final ValueEventListener deliveryListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        // Node was deleted by partner (delivered)
                        delivered[0] = true;
                        moodRef.removeEventListener(this);
                        showDeliveryDialog("Delivered ♥", "Your mood reached " + (IS_VISHAL ? "Rashika" : "Vishal") + " successfully.", true);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            moodRef.addValueEventListener(deliveryListener);
            
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!delivered[0]) {
                    moodRef.removeEventListener(deliveryListener);
                    // Check if activity is still running before showing dialog
                    if (!isFinishing() && !isDestroyed()) {
                        showDeliveryDialog("Not Delivered", (IS_VISHAL ? "Rashika" : "Vishal") + " appears to be offline right now. Try again in a bit!", false);
                    }
                }
            }, 5000);
        }
    }
    
    private void showDeliveryDialog(String title, String message, boolean isSuccess) {
        if (isFinishing() || isDestroyed()) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View customView = getLayoutInflater().inflate(R.layout.dialog_custom_delivery, null);
        builder.setView(customView);
        
        android.widget.TextView txtTitle = customView.findViewById(R.id.dialogTitle);
        android.widget.TextView txtMessage = customView.findViewById(R.id.dialogMessage);
        android.widget.ImageView icon = customView.findViewById(R.id.dialogIcon);
        android.widget.Button btnOk = customView.findViewById(R.id.dialogBtn);
        
        txtTitle.setText(title);
        txtMessage.setText(message);
        icon.setImageResource(isSuccess ? R.drawable.ic_heart : R.drawable.ic_delete);
        if (!isSuccess) {
            icon.setColorFilter(android.graphics.Color.parseColor("#FF5252"));
        } else {
            icon.setColorFilter(android.graphics.Color.parseColor("#FF4081"));
        }
        
        final android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnOk.setOnClickListener(v -> dialog.dismiss());
        // Use MaterialButton if available
        try { ((com.google.android.material.button.MaterialButton) btnOk).setCornerRadius(52); } catch (Exception ignored) {}
        dialog.show();

        // Autoclose after 3 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 3000);
    }

    private void checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                startMoodService();
            }
        } else {
            startMoodService();
        }
    }
    
    private void startMoodService() {
        if (!isServiceRunning(MoodService.class)) {
            Intent serviceIntent = new Intent(this, MoodService.class);
            serviceIntent.putExtra("IS_VISHAL", IS_VISHAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startMoodService();
            } else {
                Toast.makeText(this, "Permission required for mood rains!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFilePath);
        
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // handle small touches where stop fails
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        }
        File file = new File(audioFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private void stopRecordingAndUpload() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // handle small touches where stop fails
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            // Upload to Supabase Storage
            File file = new File(audioFilePath);
            if (file.exists()) {
                String filename = System.currentTimeMillis() + ".m4a";
                String bucket = "profile_images"; // Using profile_images bucket since we know it exists and is public
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + filename;
                
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(file, MediaType.parse("audio/mp4"));
                
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Content-Type", "audio/mp4")
                        .build();
                        
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to upload audio", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + filename;
                            
                            runOnUiThread(() -> {
                                DatabaseReference voiceRef = database.child("voice_notes").child(myDbNode.replace("locations/", "")).child("latest_url");
                                voiceRef.setValue(publicUrl);
                                
                                final boolean[] voiceDelivered = {false};
                                final ValueEventListener voiceDeliveryListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (!snapshot.exists()) {
                                            // Node was deleted by partner (delivered)
                                            voiceDelivered[0] = true;
                                            voiceRef.removeEventListener(this);
                                            showDeliveryDialog("Delivered ♥", "Your voice note reached " + (IS_VISHAL ? "Rashika" : "Vishal") + " successfully.", true);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                };
                                voiceRef.addValueEventListener(voiceDeliveryListener);
                                
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    if (!voiceDelivered[0]) {
                                        voiceRef.removeEventListener(voiceDeliveryListener);
                                        if (!isFinishing() && !isDestroyed()) {
                                            showDeliveryDialog("Not Delivered", (IS_VISHAL ? "Rashika" : "Vishal") + " appears to be offline right now. Try again in a bit!", false);
                                        }
                                    }
                                }, 5000);
                            });
                        } else {
                            String err = response.body() != null ? response.body().string() : "Unknown error";
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload failed: " + response.code() + " " + err, Toast.LENGTH_LONG).show());
                        }
                        response.close();
                    }
                });
            }
        }
    }

    private void handleStreakLogic() {
        TextView dayPill = findViewById(R.id.dayPill);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        
        database.child("streak").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 1;
                String lastOpened = null;
                
                if (snapshot.exists()) {
                    Long c = snapshot.child("count").getValue(Long.class);
                    if (c != null) count = c.intValue();
                    lastOpened = snapshot.child("lastOpenedDate").getValue(String.class);
                }
                
                if (lastOpened != null && lastOpened.equals(today)) {
                    // Already opened today, do nothing to count
                } else if (lastOpened != null) {
                    try {
                        Date lastDate = sdf.parse(lastOpened);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(lastDate);
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                        String expectedToday = sdf.format(cal.getTime());
                        
                        if (expectedToday.equals(today)) {
                            // Streak continues!
                            count++;
                        } else {
                            // Streak broken
                            count = 1;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        count = 1;
                    }
                }
                
                // Update Firebase and UI
                database.child("streak").child("count").setValue(count);
                database.child("streak").child("lastOpenedDate").setValue(today);
                
                if (dayPill != null) {
                    dayPill.setText("Day " + count);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void calculateAndShowBirthdayCountdown(String dobStr) {
        try {
            if (dobStr.contains(":")) {
                dobStr = dobStr.substring(dobStr.indexOf(":") + 1).trim();
            }
            if (dobStr.isEmpty() || dobStr.equals("Not set")) return;
            
            String[] parts = dobStr.split("/");
            if (parts.length >= 2) {
                int birthDay = Integer.parseInt(parts[0]);
                int birthMonth = Integer.parseInt(parts[1]) - 1; // 0-indexed
                
                java.util.Calendar nextBirthday = java.util.Calendar.getInstance();
                java.util.Calendar today = java.util.Calendar.getInstance();
                nextBirthday.set(java.util.Calendar.MONTH, birthMonth);
                nextBirthday.set(java.util.Calendar.DAY_OF_MONTH, birthDay);
                nextBirthday.set(java.util.Calendar.HOUR_OF_DAY, 0);
                nextBirthday.set(java.util.Calendar.MINUTE, 0);
                nextBirthday.set(java.util.Calendar.SECOND, 0);
                nextBirthday.set(java.util.Calendar.MILLISECOND, 0);
                
                // If birthday is exactly today, check if we want to show "Today is birthday"
                boolean isToday = nextBirthday.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR);
                
                if (nextBirthday.before(today) && !isToday) {
                    nextBirthday.add(java.util.Calendar.YEAR, 1);
                }
                
                android.widget.TextView txtTitle = findViewById(R.id.txtBirthdayTitle);
                android.widget.TextView txtCountdown = findViewById(R.id.txtBirthdayCountdown);
                if (txtCountdown == null || txtTitle == null) return;
                String name = IS_VISHAL ? "Rashika" : "Vishal";
                
                txtTitle.setText(name + "'s Birthday");
                
                if (isToday) {
                    txtCountdown.setText("🎉 TODAY! 🎉");
                    return;
                }
                
                if (birthdayTimer != null) birthdayTimer.cancel();
                
                long diffMillis = nextBirthday.getTimeInMillis() - System.currentTimeMillis();
                birthdayTimer = new CountDownTimer(diffMillis, 1000) {
                    public void onTick(long millisUntilFinished) {
                        long days = millisUntilFinished / (1000 * 60 * 60 * 24);
                        long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                        long mins = (millisUntilFinished / (1000 * 60)) % 60;
                        long secs = (millisUntilFinished / 1000) % 60;
                        String timer = String.format(Locale.getDefault(), "%dd %02d:%02d:%02d", days, hours, mins, secs);
                        txtCountdown.setText(timer);
                    }
                    public void onFinish() {
                        txtCountdown.setText("🎉 TODAY! 🎉");
                    }
                }.start();
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTogetherSince() {
        try {
            java.util.Calendar startDate = java.util.Calendar.getInstance();
            startDate.set(2026, java.util.Calendar.JUNE, 25, 0, 0, 0); // 25 June 2026
            
            long diffMillis = System.currentTimeMillis() - startDate.getTimeInMillis();
            long days = diffMillis / (1000 * 60 * 60 * 24);
            if (days < 0) days = 0; // Prevent negative if current date is somehow earlier
            
            android.widget.TextView txtDays = findViewById(R.id.txtTogetherDays);
            if (txtDays != null) txtDays.setText(days + " days Together");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────
    // LOCKET (Daily Photo)
    // ─────────────────────────────────────────

    private void setupLocketImagePicker() {
        ImageView imgMyLocket = findViewById(R.id.imgMyLocket);
        
        myLocketNode      = "locket/" + (IS_VISHAL ? "vishal" : "rashika");
        partnerLocketNode = "locket/" + (IS_VISHAL ? "rashika" : "vishal");
        
        locketCropImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri croppedUri = UCrop.getOutput(result.getData());
                    if (croppedUri != null) {
                        uploadLocketPhoto(croppedUri, false);
                    }
                }
            }
        );

        locketImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    boolean isGif = false;
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && mimeType.toLowerCase().contains("gif")) {
                        isGif = true;
                    } else if (uri.toString().toLowerCase().endsWith(".gif")) {
                        isGif = true;
                    }
                    
                    if (isGif) {
                        uploadLocketPhoto(uri, true);
                        return;
                    }

                    String destFileName = "ucrop_" + System.currentTimeMillis() + ".jpg";
                    android.net.Uri destUri = android.net.Uri.fromFile(new java.io.File(getCacheDir(), destFileName));
                    
                    com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
                    options.setHideBottomControls(false);
                    options.setFreeStyleCropEnabled(true);
                    options.setAspectRatioOptions(0,
                            new com.yalantis.ucrop.model.AspectRatio("1:1", 1, 1),
                            new com.yalantis.ucrop.model.AspectRatio("2:3", 2, 3),
                            new com.yalantis.ucrop.model.AspectRatio("4:5", 4, 5)
                    );
                    options.setToolbarColor(android.graphics.Color.parseColor("#FF4081"));
                    options.setStatusBarColor(android.graphics.Color.parseColor("#C51162"));
                    options.setToolbarWidgetColor(android.graphics.Color.WHITE);
                    options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF4081"));
                    
                    android.content.Intent uCropIntent = UCrop.of(uri, destUri)
                        .withOptions(options)
                        .withAspectRatio(3, 4)
                        .getIntent(MainActivity.this);
                    locketCropImageLauncher.launch(uCropIntent);
                }
            }
        );
        
        android.view.View btnUploadMyLocket = findViewById(R.id.btnUploadMyLocket);
        if (btnUploadMyLocket != null) {
            btnUploadMyLocket.setOnClickListener(v -> locketImagePickerLauncher.launch("image/*"));
        }
        
        if (imgMyLocket != null) {
            imgMyLocket.setOnClickListener(v -> {
                if (myLocketUrl != null) showFullScreenImage(myLocketUrl);
            });
        }
        
        ImageView imgPartnerLocket = findViewById(R.id.imgPartnerLocket);
        if (imgPartnerLocket != null) {
            imgPartnerLocket.setOnClickListener(v -> {
                if (partnerLocketUrl != null) showFullScreenImage(partnerLocketUrl);
            });
        }
    }

    private void uploadLocketPhoto(Uri uri, boolean isGif) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) return;
                
                byte[] imageBytes;
                String ext;
                String mime;
                
                if (isGif) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    imageBytes = buffer.toByteArray();
                    ext = ".gif";
                    mime = "image/gif";
                    is.close();
                } else {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    is.close();

                    if (bmp == null) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Failed to decode image", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Scale down to max 800px wide to keep size manageable
                    int maxW = 800;
                    if (bmp.getWidth() > maxW) {
                        float ratio = (float) maxW / bmp.getWidth();
                        bmp = Bitmap.createScaledBitmap(bmp,
                                maxW, Math.round(bmp.getHeight() * ratio), true);
                    }

                    // Compress to JPEG
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                    imageBytes = baos.toByteArray();
                    ext = ".jpg";
                    mime = "image/jpeg";
                }

                String who      = IS_VISHAL ? "vishal" : "rashika";
                String filename = who + "_locket_" + System.currentTimeMillis() + ext;
                // Upload flat into the bucket root (same as voice notes)
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/profile_images/" + filename;

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(imageBytes, MediaType.parse(mime));
                Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(body)   // POST like voice notes – works reliably
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", mime)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/profile_images/" + filename;
                    long expiry = System.currentTimeMillis() + (24L * 60 * 60 * 1000);
                    database.child(myLocketNode).child("url").setValue(publicUrl);
                    database.child(myLocketNode).child("expiry").setValue(expiry);
                    database.child(myLocketNode).child("reaction").removeValue();
                    database.child(myLocketNode).child("comment").removeValue();
                    final long finalExpiry = expiry;
                    runOnUiThread(() -> {
                        loadLocketImage(publicUrl, finalExpiry, true);
                        Toast.makeText(MainActivity.this, "Photo shared! 💛", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    final String err = response.code() + ": " + responseBody;
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Upload failed – " + err, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void setupLocketListeners() {
        setupLocketInteractions();
        String partnerName = IS_VISHAL ? "Rashika" : "Vishal";
        TextView txtPartnerLabel = findViewById(R.id.txtPartnerLocketLabel);
        if (txtPartnerLabel != null) txtPartnerLabel.setText(partnerName + "'s photo");
        TextView txtMyLabel = findViewById(R.id.txtMyLocketLabel);
        if (txtMyLabel != null) txtMyLabel.setText("Your photo");

        // Listen to my own locket
        database.child(myLocketNode).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String url    = snap.child("url").getValue(String.class);
                Long   expiry = snap.child("expiry").getValue(Long.class);
                if (url != null && expiry != null) {
                    if (System.currentTimeMillis() < expiry) {
                        loadLocketImage(url, expiry, true);
                    } else {
                        // Expired – clean up
                        database.child(myLocketNode).removeValue();
                        resetLocketSlot(true);
                    }
                } else {
                    resetLocketSlot(true);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen to partner's locket
        database.child(partnerLocketNode).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String url    = snap.child("url").getValue(String.class);
                Long   expiry = snap.child("expiry").getValue(Long.class);
                if (url != null && expiry != null) {
                    if (System.currentTimeMillis() < expiry) {
                        loadLocketImage(url, expiry, false);
                    } else {
                        database.child(partnerLocketNode).removeValue();
                        resetLocketSlot(false);
                    }
                } else {
                    resetLocketSlot(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLocketImage(String url, long expiry, boolean isMine) {
        int imgId    = isMine ? R.id.imgMyLocket    : R.id.imgPartnerLocket;
        ImageView  imgView  = findViewById(imgId);
        if (imgView == null) return;
        
        if (isMine) {
            myLocketExpiry = expiry;
            myLocketUrl = url;
        } else {
            partnerLocketExpiry = expiry;
            partnerLocketUrl = url;
        }

        imgView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(url).placeholder(R.drawable.ic_photo_placeholder).centerCrop().into(imgView);
        updateLocketTimers();

        // Push to widget
        pushLocketToWidget();
    }
    
    private void showFullScreenImage(String imageUrl) {
        showFullScreenImage(imageUrl, null, false);
    }

    private void showFullScreenImage(String imageUrl, String locketNode, boolean canReact) {
        if (isFinishing() || isDestroyed()) return;
        
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        
        ImageView imgFullScreen = dialog.findViewById(R.id.imgFullScreen);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseFullScreen);
        android.widget.ProgressBar progress = dialog.findViewById(R.id.progressFullScreen);
        
        Glide.with(this)
            .load(imageUrl)
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                    if (progress != null) progress.setVisibility(android.view.View.GONE);
                    return false;
                }
                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    if (progress != null) progress.setVisibility(android.view.View.GONE);
                    return false;
                }
            })
            .into(imgFullScreen);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.fullScreenContainer).setOnClickListener(v -> dialog.dismiss());

        final TextView floating = dialog.findViewById(R.id.txtFloatingLocket);
        final android.view.View actions = dialog.findViewById(R.id.locketActions);
        final String myName = IS_VISHAL ? "vishal" : "rashika";
        ValueEventListener reactionListener = null;
        DatabaseReference locketRef = null;

        if (locketNode != null) {
            locketRef = database.child(locketNode);
            if (canReact) {
                actions.setVisibility(android.view.View.VISIBLE);
                final DatabaseReference ref = locketRef;
                int[] ids = {R.id.react1, R.id.react2, R.id.react3, R.id.react4, R.id.react5, R.id.react6};
                for (int id : ids) {
                    TextView t = dialog.findViewById(id);
                    t.setOnClickListener(v -> {
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("emoji", t.getText().toString());
                        m.put("from", myName);
                        m.put("ts", System.currentTimeMillis());
                        ref.child("reaction").setValue(m);
                        showFloatingLocketBubble(floating, t.getText().toString(), 42f);
                    });
                }
                final android.widget.EditText edt = dialog.findViewById(R.id.edtLocketComment);
                dialog.findViewById(R.id.btnSendLocketComment).setOnClickListener(v -> {
                    String text = edt.getText().toString().trim();
                    if (text.isEmpty()) return;
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("text", text);
                    m.put("from", myName);
                    m.put("ts", System.currentTimeMillis());
                    ref.child("comment").setValue(m);
                    edt.setText("");
                    showFloatingLocketBubble(floating, text, 18f);
                });
                // Let the keyboard resize the dialog so the input stays visible
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                }
            } else {
                // Owner: show what the partner sent, floating over the photo
                reactionListener = new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        String emoji = snap.child("reaction").child("emoji").getValue(String.class);
                        String text = snap.child("comment").child("text").getValue(String.class);
                        if (emoji != null) showFloatingLocketBubble(floating, emoji, 42f);
                        if (text != null) {
                            floating.postDelayed(() -> showFloatingLocketBubble(floating, text, 18f), emoji != null ? 2800 : 0);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                };
                locketRef.addListenerForSingleValueEvent(reactionListener);
                reactionListener = null;
            }
        }

        dialog.show();
    }

    private void setupLocketInteractions() {
        FrameLayout bubblesMy = findViewById(R.id.bubblesMyLocket);
        FrameLayout bubblesPartner = findViewById(R.id.bubblesPartnerLocket);
        attachLocketBubbles(myLocketNode, bubblesMy, false);
        attachLocketBubbles(partnerLocketNode, bubblesPartner, true);

        final String myName = IS_VISHAL ? "vishal" : "rashika";
        final String[] emojis = {"❤️", "😍", "😂", "🔥", "🥺", "🤤"};

        android.view.View btnReact = findViewById(R.id.btnLocketReact);
        if (btnReact != null) {
            btnReact.setOnClickListener(v -> {
                if (partnerLocketUrl == null) {
                    Toast.makeText(this, "No photo to react to yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                final android.app.AlertDialog[] dlg = new android.app.AlertDialog[1];
                android.widget.GridLayout grid = new android.widget.GridLayout(this);
                grid.setColumnCount(3);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                grid.setPadding(pad, pad, pad, pad);
                for (String emoji : emojis) {
                    TextView t = new TextView(this);
                    t.setText(emoji);
                    t.setTextSize(48f);
                    t.setGravity(android.view.Gravity.CENTER);
                    android.widget.GridLayout.LayoutParams glp = new android.widget.GridLayout.LayoutParams(
                            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f),
                            android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f));
                    glp.width = 0;
                    glp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                    t.setLayoutParams(glp);
                    t.setPadding(0, pad / 2, 0, pad / 2);
                    t.setOnClickListener(x -> {
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("emoji", emoji);
                        m.put("from", myName);
                        m.put("ts", System.currentTimeMillis());
                        database.child(partnerLocketNode).child("reaction").setValue(m);
                        sendLocketNotification("reacted " + emoji + " to your photo.");
                        if (dlg[0] != null) dlg[0].dismiss();
                    });
                    grid.addView(t);
                }
                dlg[0] = new android.app.AlertDialog.Builder(this)
                        .setTitle("Add reaction")
                        .setView(grid)
                        .setNeutralButton("Remove reaction",
                                (d, w) -> database.child(partnerLocketNode).child("reaction").removeValue())
                        .show();
            });
        }

        android.view.View btnComment = findViewById(R.id.btnLocketComment);
        if (btnComment != null) {
            btnComment.setOnClickListener(v -> {
                if (partnerLocketUrl == null) {
                    Toast.makeText(this, "No photo to comment on yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.view.View content = getLayoutInflater().inflate(R.layout.dialog_locket_comment, null);
                final android.widget.EditText edt = content.findViewById(R.id.edtLocketCommentDialog);
                final TextView counter = content.findViewById(R.id.txtCommentCounter);
                final TextView title = content.findViewById(R.id.txtCommentDialogTitle);
                final android.view.View btnSend = content.findViewById(R.id.btnSendLocketCommentDialog);
                final android.view.View btnCancel = content.findViewById(R.id.btnCancelLocketCommentDialog);
                final android.view.View btnDelete = content.findViewById(R.id.btnDeleteLocketCommentDialog);

                edt.setText(sentCommentText);
                edt.setSelection(edt.getText().length());
                counter.setText(edt.getText().length() + "/100");
                edt.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                    @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                        counter.setText(s.length() + "/100");
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });
                if (!sentCommentText.isEmpty()) {
                    title.setText("Edit your comment");
                    btnDelete.setVisibility(android.view.View.VISIBLE);
                }

                final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                        .setView(content)
                        .create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                btnSend.setOnClickListener(x -> {
                    String text = edt.getText().toString().trim();
                    if (text.isEmpty()) return;
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("text", text);
                    m.put("from", myName);
                    m.put("ts", System.currentTimeMillis());
                    database.child(partnerLocketNode).child("comment").setValue(m);
                    sendLocketNotification("commented on your photo: " + text);
                    dialog.dismiss();
                });
                btnCancel.setOnClickListener(x -> dialog.dismiss());
                btnDelete.setOnClickListener(x -> {
                    database.child(partnerLocketNode).child("comment").removeValue();
                    dialog.dismiss();
                });
                dialog.show();
            });
        }
    }

    private String sentCommentText = "";

    private void attachLocketBubbles(String node, FrameLayout container, boolean editable) {
        if (container == null) return;
        database.child(node).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                container.removeAllViews();
                if (editable) sentCommentText = "";
                if (snap.child("url").getValue(String.class) == null) return;
                String emoji = snap.child("reaction").child("emoji").getValue(String.class);
                String text = snap.child("comment").child("text").getValue(String.class);
                if (emoji != null) {
                    TextView b = addLocketBubble(container, emoji, 30f, android.view.Gravity.TOP | android.view.Gravity.END, 0);
                    if (editable) b.setOnClickListener(v -> confirmEditLocketItem("reaction", node, R.id.btnLocketReact));
                }
                if (text != null) {
                    TextView b = addLocketBubble(container, text, 12f, android.view.Gravity.BOTTOM | android.view.Gravity.START, 400);
                    if (editable) {
                        sentCommentText = text;
                        b.setOnClickListener(v -> confirmEditLocketItem("comment", node, R.id.btnLocketComment));
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendLocketNotification(String text) {
        String myId = IS_VISHAL ? "vishal" : "rashika";
        String partnerId = IS_VISHAL ? "rashika" : "vishal";
        String myName = IS_VISHAL ? "Vishal" : "Rashika";
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(partnerId);
        String notifId = notifRef.push().getKey();
        if (notifId != null) {
            AppNotification n = new AppNotification(notifId, "locket_reaction", myId, myName, text, System.currentTimeMillis(), "");
            notifRef.child(notifId).setValue(n);
        }
    }

    private void confirmEditLocketItem(String child, String node, int editButtonId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(child.equals("reaction") ? "Your reaction" : "Your comment")
                .setItems(new String[]{"Edit", "Remove"}, (d, which) -> {
                    if (which == 0) {
                        android.view.View btn = findViewById(editButtonId);
                        if (btn != null) btn.performClick();
                    } else {
                        database.child(node).child(child).removeValue();
                    }
                }).show();
    }

    private TextView addLocketBubble(FrameLayout container, String text, float sizeSp, int gravity, long delay) {
        float d = getResources().getDisplayMetrics().density;
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(sizeSp);
        boolean isComment = sizeSp < 20f;
        bubble.setTextColor(android.graphics.Color.parseColor(isComment ? "#FFFFFF" : "#2D2036"));
        bubble.setMaxLines(3);
        bubble.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bubble.setPadding((int) (10 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        if (isComment) {
            bg.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT);
            bg.setColors(new int[]{android.graphics.Color.parseColor("#FF5C81"), android.graphics.Color.parseColor("#FF8FB1")});
        } else {
            bg.setColor(android.graphics.Color.parseColor("#EEFFFFFF"));
        }
        bg.setCornerRadius(18 * d);
        bubble.setBackground(bg);
        bubble.setElevation(4 * d);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = gravity;
        lp.setMargins((int) (6 * d), (int) (6 * d), (int) (6 * d), (int) (6 * d));
        bubble.setLayoutParams(lp);
        container.addView(bubble);
        bubble.setClickable(true);
        if (container.getWidth() > 0) bubble.setMaxWidth((int) (container.getWidth() - 12 * d));
        else bubble.setMaxWidth((int) (130 * d));

        android.animation.ObjectAnimator bob = android.animation.ObjectAnimator.ofFloat(bubble, "translationY", 0f, -6 * d);
        bob.setDuration(1400);
        bob.setStartDelay(delay);
        bob.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        bob.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        bob.start();
        return bubble;
    }

    private void showFloatingLocketBubble(TextView bubble, String text, float sizeSp) {
        if (bubble == null) return;
        bubble.animate().cancel();
        bubble.setText(text);
        bubble.setTextSize(sizeSp);
        bubble.setVisibility(android.view.View.VISIBLE);
        bubble.setAlpha(1f);
        bubble.setTranslationY(120f);
        bubble.animate().translationY(-300f).alpha(0f).setDuration(2600)
                .withEndAction(() -> bubble.setVisibility(android.view.View.GONE)).start();
    }

    private void updateLocketTimers() {
        TextView myTimerTxt = findViewById(R.id.txtMyLocketTimer);
        TextView partnerTimerTxt = findViewById(R.id.txtPartnerLocketTimer);
        
        long now = System.currentTimeMillis();
        
        if (myTimerTxt != null && myLocketExpiry != null) {
            long remaining = myLocketExpiry - now;
            if (remaining > 0) {
                long h = remaining / (1000 * 60 * 60);
                long m = (remaining / (1000 * 60)) % 60;
                myTimerTxt.setText(String.format(Locale.getDefault(), "Expires in %dh %02dm", h, m));
            } else {
                myTimerTxt.setText("No Photo");
                myLocketExpiry = null;
                database.child(myLocketNode).removeValue();
                resetLocketSlot(true);
            }
        }
        
        if (partnerTimerTxt != null && partnerLocketExpiry != null) {
            long remaining = partnerLocketExpiry - now;
            if (remaining > 0) {
                long h = remaining / (1000 * 60 * 60);
                long m = (remaining / (1000 * 60)) % 60;
                partnerTimerTxt.setText(String.format(Locale.getDefault(), "Expires in %dh %02dm", h, m));
            } else {
                partnerTimerTxt.setText("Waiting...");
                partnerLocketExpiry = null;
                database.child(partnerLocketNode).removeValue();
                resetLocketSlot(false);
            }
        }
    }

    private void resetLocketSlot(boolean isMine) {
        int imgId    = isMine ? R.id.imgMyLocket    : R.id.imgPartnerLocket;
        int timerId  = isMine ? R.id.txtMyLocketTimer : R.id.txtPartnerLocketTimer;
        ImageView imgView  = findViewById(imgId);
        TextView  timerTxt = findViewById(timerId);
        
        if (isMine) { myLocketExpiry = null; myLocketUrl = null; }
        else { partnerLocketExpiry = null; partnerLocketUrl = null; }
        
        if (imgView  != null) {
            imgView.setScaleType(android.widget.ImageView.ScaleType.CENTER);
            imgView.setImageResource(R.drawable.ic_photo_placeholder);
        }
        if (timerTxt != null) timerTxt.setText(isMine ? "No Photo" : "Waiting...");
    }

    private void pushLocketToWidget() {
        android.appwidget.AppWidgetManager awm = android.appwidget.AppWidgetManager.getInstance(this);
        android.content.ComponentName cn = new android.content.ComponentName(this, LocketWidgetProvider.class);
        int[] ids = awm.getAppWidgetIds(cn);

        database.child(myLocketNode).get().addOnSuccessListener(snap -> {
            String myUrl      = snap.child("url").getValue(String.class);
            database.child(partnerLocketNode).get().addOnSuccessListener(snap2 -> {
                String partnerUrl = snap2.child("url").getValue(String.class);
                for (int id : ids) {
                    LocketWidgetProvider.updateWidget(this, awm, id, myUrl, partnerUrl);
                }
            });
        });
    }
    private void setupStories() {
        String myNameId = IS_VISHAL ? "vishal" : "rashika";
        String partnerNameId = IS_VISHAL ? "rashika" : "vishal";
        
        android.widget.TextView txtPartnerStoryLabel = findViewById(R.id.txtPartnerStoryLabel);
        txtPartnerStoryLabel.setText((IS_VISHAL ? "Rashika" : "Vishal") + "'s Story");

        // Fetch Profile Pictures
        DatabaseReference myProfileRef = FirebaseDatabase.getInstance().getReference().child("profiles").child(myNameId);
        DatabaseReference partnerProfileRef = FirebaseDatabase.getInstance().getReference().child("profiles").child(partnerNameId);
        
        myProfileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                ImageView imgMyStoryProfile = findViewById(R.id.imgMyStoryProfile);
                Object loadSource = (imgUrl != null && !imgUrl.isEmpty()) ? imgUrl : R.drawable.ic_photo_placeholder;
                Glide.with(MainActivity.this)
                     .load(loadSource)
                     .placeholder(R.drawable.ic_photo_placeholder)
                     .error(R.drawable.ic_photo_placeholder)
                     .transform(new CircleCrop())
                     .into(imgMyStoryProfile);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        partnerProfileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                ImageView imgPartnerStoryProfile = findViewById(R.id.imgPartnerStoryProfile);
                Object loadSource = (imgUrl != null && !imgUrl.isEmpty()) ? imgUrl : R.drawable.ic_photo_placeholder;
                Glide.with(MainActivity.this)
                     .load(loadSource)
                     .placeholder(R.drawable.ic_photo_placeholder)
                     .error(R.drawable.ic_photo_placeholder)
                     .transform(new CircleCrop())
                     .into(imgPartnerStoryProfile);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Fetch Stories
        DatabaseReference myStoriesRef = FirebaseDatabase.getInstance().getReference().child("stories").child(myNameId);
        DatabaseReference partnerStoriesRef = FirebaseDatabase.getInstance().getReference().child("stories").child(partnerNameId);
        
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        
        myStoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myStoriesList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Story s = ds.getValue(Story.class);
                    if (s != null) {
                        if (s.getTimestamp() > twentyFourHoursAgo) {
                            myStoriesList.add(s);
                        } else {
                            ds.getRef().removeValue(); // Clean up expired
                        }
                    }
                }
                updateStoryRings();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        partnerStoriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                partnerStoriesList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Story s = ds.getValue(Story.class);
                    if (s != null) {
                        if (s.getTimestamp() > twentyFourHoursAgo) {
                            partnerStoriesList.add(s);
                        } else {
                            ds.getRef().removeValue(); // Clean up expired
                        }
                    }
                }
                updateStoryRings();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Click Listeners
        findViewById(R.id.btnMyStory).setOnClickListener(v -> {
            if (myStoriesList.isEmpty()) {
                openStoryPicker();
            } else {
                View myStoryRing = findViewById(R.id.myStoryRing);
                animateRing(myStoryRing);
                preloadFirstStoryAndOpen(myStoriesList, myNameId);
            }
        });
        
        findViewById(R.id.imgAddStory).setOnClickListener(v -> {
            openStoryPicker();
        });
        
        findViewById(R.id.btnPartnerStory).setOnClickListener(v -> {
            if (!partnerStoriesList.isEmpty()) {
                View partnerStoryRing = findViewById(R.id.partnerStoryRing);
                animateRing(partnerStoryRing);
                preloadFirstStoryAndOpen(partnerStoriesList, partnerNameId);
            } else {
                android.widget.Toast.makeText(MainActivity.this, "No active stories", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private android.animation.ObjectAnimator locketAnimator;
    private com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap> storyPreloadTarget;
    private SensorManager sensorManager;

    private void animateRing(View ring) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(ring, "rotation", 0f, 360f);
        animator.setDuration(800);
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.start();
        ring.setTag(animator);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        View myStoryRing = findViewById(R.id.myStoryRing);
        if (myStoryRing != null && myStoryRing.getTag() instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) myStoryRing.getTag()).cancel();
            myStoryRing.setRotation(0f);
        }
        View partnerStoryRing = findViewById(R.id.partnerStoryRing);
        if (partnerStoryRing != null && partnerStoryRing.getTag() instanceof android.animation.ObjectAnimator) {
            ((android.animation.ObjectAnimator) partnerStoryRing.getTag()).cancel();
            partnerStoryRing.setRotation(0f);
        }
        updateStoryRings();
    }
    
    private void updateStoryRings() {
        String myNameId = IS_VISHAL ? "vishal" : "rashika";
        String partnerNameId = IS_VISHAL ? "rashika" : "vishal";
        
        android.content.SharedPreferences prefs = getSharedPreferences("vishika_prefs", MODE_PRIVATE);
        
        View myStoryRing = findViewById(R.id.myStoryRing);
        if (myStoriesList.isEmpty()) {
            myStoryRing.setBackgroundResource(0);
        } else {
            long lastSeenMy = prefs.getLong("lastSeenStory_" + myNameId, 0);
            boolean hasNewMy = false;
            for (Story s : myStoriesList) {
                if (s.getTimestamp() > lastSeenMy) {
                    hasNewMy = true;
                    break;
                }
            }
            myStoryRing.setBackgroundResource(hasNewMy ? R.drawable.bg_story_ring_active : R.drawable.bg_story_ring_inactive);
        }
        
        View partnerStoryRing = findViewById(R.id.partnerStoryRing);
        if (partnerStoriesList.isEmpty()) {
            partnerStoryRing.setBackgroundResource(0);
        } else {
            long lastSeenPartner = prefs.getLong("lastSeenStory_" + partnerNameId, 0);
            boolean hasNewPartner = false;
            for (Story s : partnerStoriesList) {
                if (s.getTimestamp() > lastSeenPartner) {
                    hasNewPartner = true;
                    break;
                }
            }
            partnerStoryRing.setBackgroundResource(hasNewPartner ? R.drawable.bg_story_ring_active : R.drawable.bg_story_ring_inactive);
        }
    }
    
    private void preloadFirstStoryAndOpen(java.util.List<Story> storiesList, String userId) {
        if (storiesList == null || storiesList.isEmpty()) {
            openStoryViewer(userId, storiesList);
            return;
        }
        
        int startIndex = 0;
        long lastSeen = getSharedPreferences("vishika_prefs", MODE_PRIVATE).getLong("lastSeenStory_" + userId, 0);
        for (int i = 0; i < storiesList.size(); i++) {
            if (storiesList.get(i).getTimestamp() > lastSeen) {
                startIndex = i;
                break;
            }
        }
        
        Story storyToLoad = storiesList.get(startIndex);
        if (storyToLoad.getUrl() == null || storyToLoad.getUrl().isEmpty()) {
            openStoryViewer(userId, storiesList);
            return;
        }

        if ("Photo".equals(storyToLoad.getType())) {
            storyPreloadTarget = new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                @Override
                public void onResourceReady(@NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                    openStoryViewer(userId, storiesList);
                    storyPreloadTarget = null;
                }

                @Override
                public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}

                @Override
                public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                    openStoryViewer(userId, storiesList); // Fallback
                    storyPreloadTarget = null;
                }
            };
            
            Glide.with(this)
                .asBitmap()
                .load(storyToLoad.getUrl())
                .into(storyPreloadTarget);
        } else {
            // For video, we just open it directly
            openStoryViewer(userId, storiesList);
        }
    }
    
    private void openStoryPicker() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Add to Story")
                .setItems(new CharSequence[]{"Photo", "Video"}, (dialog, which) -> {
                    pendingSurpriseType = which == 0 ? "Photo" : "Video";
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType(which == 0 ? "image/*" : "video/*");
                    storyMediaPickerLauncher.launch(intent);
                }).show();
    }
    
    private void openStoryViewer(String userId, java.util.List<Story> storiesList) {
        Intent intent = new Intent(this, StoryViewerActivity.class);
        intent.putExtra("USER_ID", userId);
        if (storiesList != null && storiesList instanceof java.util.ArrayList) {
            intent.putExtra("STORIES_LIST", (java.util.ArrayList<Story>) storiesList);
        }
        startActivity(intent);
    }
    
    private void showUploadUpdateDialog(android.net.Uri apkUri) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText versionCodeInput = new android.widget.EditText(this);
        versionCodeInput.setHint("Version Code (e.g. 18)");
        versionCodeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        final android.widget.EditText releaseNotesInput = new android.widget.EditText(this);
        releaseNotesInput.setHint("Release Notes");
        
        layout.addView(versionCodeInput);
        layout.addView(releaseNotesInput);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Upload Update")
            .setView(layout)
            .setPositiveButton("Upload", (dialog, which) -> {
                String vCodeStr = versionCodeInput.getText().toString();
                String releaseNotes = releaseNotesInput.getText().toString();
                if (!vCodeStr.isEmpty()) {
                    int vCode = Integer.parseInt(vCodeStr);
                    uploadUpdateToSupabase(apkUri, vCode, releaseNotes);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void uploadUpdateToSupabase(android.net.Uri apkUri, int versionCode, String releaseNotes) {
        Toast.makeText(this, "Uploading update...", Toast.LENGTH_LONG).show();
        
        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(apkUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();

                String filename = "vishika_v" + versionCode + ".apk";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/app_updates/" + filename;

                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/vnd.android.package-archive"), fileBytes);
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    String publicDownloadUrl = SUPABASE_URL + "/storage/v1/object/public/app_updates/" + filename;
                    
                    java.util.Map<String, Object> updateMap = new java.util.HashMap<>();
                    updateMap.put("latest_version_code", versionCode);
                    updateMap.put("download_url", publicDownloadUrl);
                    updateMap.put("release_notes", releaseNotes);
                    
                    FirebaseDatabase.getInstance().getReference("app_update/vishal").setValue(updateMap);
                    FirebaseDatabase.getInstance().getReference("app_update/rashika").setValue(updateMap);
                    
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Update published successfully!", Toast.LENGTH_LONG).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error uploading update", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    
    private void setupCulturePreview() {
        TextView tabPartner = findViewById(R.id.tabPartnerCulture);
        TextView tabMine = findViewById(R.id.tabMyCulture);
        ImageView btnEditCulturePencil = findViewById(R.id.btnEditCulturePencil);
        ImageView btnAddCulturePencil = findViewById(R.id.btnAddCulturePencil);
        androidx.viewpager2.widget.ViewPager2 vpCulturePreview = findViewById(R.id.vpCulturePreview);
        com.google.android.material.tabs.TabLayout tabLayoutCulturePreview = findViewById(R.id.tabLayoutCulturePreview);
        LinearLayout layoutCultureImagesPreview = findViewById(R.id.layoutCultureImagesPreview);
        java.util.List<String> previewImageUrls = new java.util.ArrayList<>();
        CultureImageAdapter previewAdapter = new CultureImageAdapter(this, previewImageUrls);
        vpCulturePreview.setAdapter(previewAdapter);
        new com.google.android.material.tabs.TabLayoutMediator(tabLayoutCulturePreview, vpCulturePreview, (tab, position) -> {}).attach();
        android.webkit.WebView webviewCulturePreview = findViewById(R.id.webviewCulturePreview);
        TextView textCultureNoMedia = findViewById(R.id.textCultureNoMedia);
        TextView textCulturePreviewDesc = findViewById(R.id.textCulturePreviewDesc);
        TextView textCulturePreviewName = findViewById(R.id.textCulturePreviewName);
        
        if (tabPartner == null) return;
        
        webviewCulturePreview.getSettings().setJavaScriptEnabled(true);
        webviewCulturePreview.getSettings().setDomStorageEnabled(true);
        webviewCulturePreview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webviewCulturePreview.setWebChromeClient(new android.webkit.WebChromeClient());
        
        final boolean[] isViewingMine = {false};
        
        Runnable updateTabs = () -> {
            if (isViewingMine[0]) {
                tabMine.setBackgroundResource(R.drawable.bg_pill_day);
                tabMine.setTextColor(android.graphics.Color.parseColor("#FF4081"));
                tabPartner.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                tabPartner.setTextColor(android.graphics.Color.parseColor("#888888"));
                btnEditCulturePencil.setVisibility(View.VISIBLE);
                btnAddCulturePencil.setVisibility(View.VISIBLE);
            } else {
                tabPartner.setBackgroundResource(R.drawable.bg_pill_day);
                tabPartner.setTextColor(android.graphics.Color.parseColor("#FF4081"));
                tabMine.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                tabMine.setTextColor(android.graphics.Color.parseColor("#888888"));
                btnEditCulturePencil.setVisibility(View.GONE);
                btnAddCulturePencil.setVisibility(View.GONE);
            }
        };
        
        ValueEventListener cultureListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    layoutCultureImagesPreview.setVisibility(View.GONE);
                    webviewCulturePreview.setVisibility(View.GONE);
                    textCultureNoMedia.setVisibility(View.VISIBLE);
                    textCulturePreviewDesc.setText("");
                    return;
                }
                String mediaType = snapshot.child("mediaType").getValue(String.class);
                String mediaUrl = snapshot.child("mediaUrl").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String cultureName = snapshot.child("cultureName").getValue(String.class);
                
                textCultureNoMedia.setVisibility(View.GONE);
                
                if ("youtube".equals(mediaType) && mediaUrl != null) {
                    layoutCultureImagesPreview.setVisibility(View.GONE);
                    webviewCulturePreview.setVisibility(View.VISIBLE);
                    String videoId = extractYoutubeId(mediaUrl);
                    if (videoId != null) {
                        String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body style='margin:0;padding:0;'><iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/" + videoId + "?autoplay=0&controls=1&rel=0&origin=https://www.youtube.com\" frameborder=\"0\" allow=\"fullscreen\"></iframe></body></html>";
                        webviewCulturePreview.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null);
                    }
                } else if ("image".equals(mediaType) && mediaUrl != null) {
                    webviewCulturePreview.setVisibility(View.GONE);
                    webviewCulturePreview.loadUrl("about:blank");
                    
                    String[] urls = mediaUrl.split(",");
                    previewImageUrls.clear();
                    previewImageUrls.addAll(java.util.Arrays.asList(urls));
                    previewAdapter.notifyDataSetChanged();
                    
                    layoutCultureImagesPreview.setVisibility(View.VISIBLE);
                } else {
                    layoutCultureImagesPreview.setVisibility(View.GONE);
                    webviewCulturePreview.setVisibility(View.GONE);
                    textCultureNoMedia.setVisibility(View.VISIBLE);
                }
                
                if (cultureName != null && !cultureName.isEmpty()) {
                    textCulturePreviewName.setText(capitalizeWords(cultureName));
                    textCulturePreviewName.setVisibility(View.VISIBLE);
                } else {
                    textCulturePreviewName.setVisibility(View.GONE);
                }
                
                textCulturePreviewDesc.setText(description != null ? description : "");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        
        Runnable loadCulture = () -> {
            String target = isViewingMine[0] ? (IS_VISHAL ? "vishal" : "rashika") : (IS_VISHAL ? "rashika" : "vishal");
            // Remove previous listener to prevent leaks
            database.child("culture").child(IS_VISHAL ? "vishal" : "rashika").removeEventListener(cultureListener);
            database.child("culture").child(IS_VISHAL ? "rashika" : "vishal").removeEventListener(cultureListener);
            database.child("culture").child(target).addValueEventListener(cultureListener);
        };
        
        tabPartner.setOnClickListener(v -> {
            isViewingMine[0] = false;
            updateTabs.run();
            loadCulture.run();
        });
        
        tabMine.setOnClickListener(v -> {
            isViewingMine[0] = true;
            updateTabs.run();
            loadCulture.run();
        });
        
        btnEditCulturePencil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CultureActivity.class);
            intent.putExtra("IS_EDITING", true);
            intent.putExtra("IS_ADDING", false);
            startActivity(intent);
        });
        
        btnAddCulturePencil.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CultureActivity.class);
            intent.putExtra("IS_EDITING", true);
            intent.putExtra("IS_ADDING", true);
            startActivity(intent);
        });

        loadCulture.run();
    }
    
    private String extractYoutubeId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) return matcher.group();
        return null;
    }
    
    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
