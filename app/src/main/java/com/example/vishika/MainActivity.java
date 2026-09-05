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
    public static final boolean IS_VISHAL = true; // Toggle for testing
    
    private String myDbNode = IS_VISHAL ? "locations/vishal" : "locations/rashika";
    private String partnerDbNode = IS_VISHAL ? "locations/rashika" : "locations/vishal";

    private FusedLocationProviderClient fusedLocationClient;
        private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private DatabaseReference database;
    private LocationCallback locationCallback;
    
    private CountDownTimer birthdayTimer;
    private double myLat = IS_VISHAL ? 31.663744 : 25.426241;
    private double myLng = IS_VISHAL ? 74.928336 : 81.820452;
    private double partnerLat = IS_VISHAL ? 25.426241 : 31.663744;
    private double partnerLng = IS_VISHAL ? 81.820452 : 74.928336;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        navFeed.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        btnRefreshMap.setOnClickListener(v -> {
            Toast.makeText(this, "Centering map...", Toast.LENGTH_SHORT).show();
            updateDistanceUI();
        });

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
        TextView txtPushToTalk = findViewById(R.id.txtPushToTalk);
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/walkietalkie.m4a";

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
                            txtPushToTalk.setText("Recording...");
                            v.setBackgroundResource(R.drawable.bg_button_answer); // Add custom tint if you want
                            v.setAlpha(0.7f);
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null) vibrator.vibrate(50);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isRecording) {
                            stopRecordingAndUpload();
                            txtPushToTalk.setText("Hold to Talk");
                            v.setAlpha(1.0f);
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
                    database.child(myDbNode).child("timestamp").setValue(System.currentTimeMillis());
                    
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
        } else if ((System.currentTimeMillis() - partnerTimestamp) > (15 * 60 * 1000)) {
            String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(partnerTimestamp));
            textLocationStatus.setText(partnerName + " is offline (Last seen at " + timeStr + ")");
            textLocationStatus.setTextColor(android.graphics.Color.parseColor("#888888")); // Subtle gray color
        } else {
            textLocationStatus.setTextColor(android.graphics.Color.parseColor("#555555"));
            // Get city name using Geocoder asynchronously
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(partnerLat, partnerLng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        
                        String feature = addr.getFeatureName();
                        String subLocality = addr.getSubLocality();
                        String locality = addr.getLocality();
                        if (locality == null) locality = addr.getSubAdminArea();
                        String state = addr.getAdminArea();
                        
                        StringBuilder locationBuilder = new StringBuilder();
                        
                        // Filter out Plus Codes (e.g. RWJH+XM6), pure street numbers, and duplicates
                        if (feature != null) {
                            if (feature.contains("+") || feature.matches("^[0-9\\-]+$") || feature.equals(locality) || feature.equals(subLocality) || feature.equals(state)) {
                                feature = null;
                            }
                        }
                        
                        if (feature != null) {
                            locationBuilder.append(feature).append(", ");
                        } else if (subLocality != null && !subLocality.equals(locality)) {
                            locationBuilder.append(subLocality).append(", ");
                        }
                        
                        if (locality != null) {
                            locationBuilder.append(locality);
                        } else if (state != null) {
                            locationBuilder.append(state);
                        }
                        
                        if (locationBuilder.length() == 0) {
                            locationBuilder.append("Unknown area");
                        }
                        
                        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(partnerTimestamp));
                        final String finalText = partnerName + " is at " + locationBuilder.toString() + " since " + timeStr;
                        
                        runOnUiThread(() -> {
                            textLocationStatus.setText(finalText);
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
                
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                gd.setColor(online ? android.graphics.Color.parseColor("#4CAF50") : android.graphics.Color.parseColor("#F44336"));
                statusDot.setBackground(gd);
                
                statusText.setText(online ? partnerDisplayName + " is Online" : partnerDisplayName + " is Offline");
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
                        uploadLocketPhoto(croppedUri);
                    }
                }
            }
        );

        locketImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String destFileName = "ucrop_" + System.currentTimeMillis() + ".jpg";
                    android.net.Uri destUri = android.net.Uri.fromFile(new java.io.File(getCacheDir(), destFileName));
                    
                    com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
                    options.setHideBottomControls(false);
                    options.setFreeStyleCropEnabled(true);
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

    private void uploadLocketPhoto(Uri uri) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) return;
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();

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
                byte[] imageBytes = baos.toByteArray();

                String who      = IS_VISHAL ? "vishal" : "rashika";
                String filename = who + "_locket_" + System.currentTimeMillis() + ".jpg";
                // Upload flat into the bucket root (same as voice notes)
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/profile_images/" + filename;

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(body)   // POST like voice notes – works reliably
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "image/jpeg")
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/profile_images/" + filename;
                    long expiry = System.currentTimeMillis() + (24L * 60 * 60 * 1000);
                    database.child(myLocketNode).child("url").setValue(publicUrl);
                    database.child(myLocketNode).child("expiry").setValue(expiry);
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
        
        dialog.show();
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
            }
        }
    }

    private void resetLocketSlot(boolean isMine) {
        int imgId    = isMine ? R.id.imgMyLocket    : R.id.imgPartnerLocket;
        int timerId  = isMine ? R.id.txtMyLocketTimer : R.id.txtPartnerLocketTimer;
        ImageView imgView  = findViewById(imgId);
        TextView  timerTxt = findViewById(timerId);
        
        if (isMine) myLocketExpiry = null;
        else partnerLocketExpiry = null;
        
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
}
