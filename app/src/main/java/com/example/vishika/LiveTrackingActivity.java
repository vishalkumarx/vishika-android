package com.example.vishika;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LiveTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker partnerMarker;
    private Marker myMarker;
    private Circle accuracyCircle;
    private Polyline liveTrailPolyline;

    private final List<LatLng> trailPoints = new ArrayList<>();
    private ValueAnimator markerAnimator;

    private double myLat;
    private double myLng;
    private double partnerLat;
    private double partnerLng;
    private long partnerTimestamp = 0;
    private float partnerSpeedMps = 0f;
    private float partnerAccuracy = 15f;
    private int partnerBattery = -1;
    private boolean partnerCharging = false;
    private boolean isPartnerOnline = false;

    private Bitmap myProfileBitmap;
    private Bitmap partnerProfileBitmap;
    private String myImageUrl;
    private String partnerImageUrl;

    private boolean isFollowingPartner = true;
    private boolean hasCenteredInitially = false;

    private String myNodeName;
    private String partnerNodeName;
    private String partnerDisplayName;

    private DatabaseReference database;
    private DatabaseReference partnerLocRef;
    private DatabaseReference partnerStatusRef;
    private ValueEventListener partnerLocListener;
    private ValueEventListener partnerStatusListener;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback myLocationCallback;

    private String lastGeocodedAddress = null;
    private double lastGeocodedLat = 0;
    private double lastGeocodedLng = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Heartbeat that keeps partner's device in 2s High-Frequency "Live Watch" mode
    private final Runnable liveWatchKeepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (database != null && partnerNodeName != null) {
                DatabaseReference watchRef = database.child("live_watch").child(partnerNodeName);
                watchRef.onDisconnect().removeValue();
                watchRef.setValue(System.currentTimeMillis() + 25000L);
            }
            mainHandler.postDelayed(this, 10000L);
        }
    };

    // 1-second ticker to show live freshness ("Updated 2s ago") like Swiggy/Zomato
    private final Runnable freshnessTickerRunnable = new Runnable() {
        @Override
        public void run() {
            updateFreshnessText();
            mainHandler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_tracking);

        boolean isVishal = MainActivity.IS_VISHAL;
        myNodeName = isVishal ? "vishal" : "rashika";
        partnerNodeName = isVishal ? "rashika" : "vishal";
        partnerDisplayName = isVishal ? "Rashika" : "Vishal";

        myLat = isVishal ? 31.663744 : 25.426241;
        myLng = isVishal ? 74.928336 : 81.820452;
        partnerLat = isVishal ? 25.426241 : 31.663744;
        partnerLng = isVishal ? 81.820452 : 74.928336;

        database = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView txtPartnerName = findViewById(R.id.txtLivePartnerName);
        if (txtPartnerName != null) {
            txtPartnerName.setText(partnerDisplayName);
        }

        View dotLivePulse = findViewById(R.id.dotLivePulse);
        if (dotLivePulse != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(Color.parseColor("#4CAF50"));
            dotLivePulse.setBackground(gd);

            // Subtle pulse animation on the LIVE dot
            android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(dotLivePulse, "alpha", 1f, 0.25f, 1f);
            pulse.setDuration(1200);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
        }

        findViewById(R.id.btnBackLiveTrack).setOnClickListener(v -> finish());

        findViewById(R.id.btnOpenHistoryFromLive).setOnClickListener(v -> {
            startActivity(new Intent(LiveTrackingActivity.this, LocationHistoryActivity.class));
        });

        TextView btnFollowPartner = findViewById(R.id.btnFollowPartner);
        btnFollowPartner.setOnClickListener(v -> {
            isFollowingPartner = true;
            updateFollowButtonUI();
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(partnerLat, partnerLng), 17f));
            }
        });

        findViewById(R.id.btnFitBoth).setOnClickListener(v -> {
            isFollowingPartner = false;
            updateFollowButtonUI();
            if (mMap != null) {
                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                bounds.include(new LatLng(myLat, myLng));
                bounds.include(new LatLng(partnerLat, partnerLng));
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 160));
                } catch (Exception ignored) {}
            }
        });

        findViewById(R.id.btnPingPartnerNow).setOnClickListener(v -> {
            Toast.makeText(this, "Requesting instant GPS fix from " + partnerDisplayName + "...", Toast.LENGTH_SHORT).show();
            database.child("location_requests").child(partnerNodeName).setValue(System.currentTimeMillis());
            database.child("live_watch").child(partnerNodeName).setValue(System.currentTimeMillis() + 25000L);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.liveTrackingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupProfileAvatars();
        loadRecentTrailSeed();
        setupPartnerListeners();
    }

    private void updateFollowButtonUI() {
        TextView btnFollow = findViewById(R.id.btnFollowPartner);
        if (btnFollow != null) {
            btnFollow.setText(isFollowingPartner ? "🎯 Following" : "🎯 Follow " + partnerDisplayName);
            btnFollow.setTextColor(Color.parseColor(isFollowingPartner ? "#FF4081" : "#2D2036"));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setBuildingsEnabled(true);

        // If user manually drags the map, pause auto-follow so they can look around freely
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                if (isFollowingPartner) {
                    isFollowingPartner = false;
                    updateFollowButtonUI();
                }
            }
        });

        updateMarkersOnMap(false);
    }

    private void loadRecentTrailSeed() {
        // Load last 15 history points from today to seed the live trail
        database.child("location_history").child(partnerNodeName)
                .orderByChild("timestamp").limitToLast(15)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000L);
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Double lat = child.child("lat").getValue(Double.class);
                            Double lng = child.child("lng").getValue(Double.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            if (lat != null && lng != null && ts != null && ts >= sixHoursAgo) {
                                addPointToTrail(new LatLng(lat, lng));
                            }
                        }
                        redrawTrailPolyline();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void addPointToTrail(LatLng pt) {
        if (pt.latitude == 0.0 && pt.longitude == 0.0) return;
        if (!trailPoints.isEmpty()) {
            LatLng last = trailPoints.get(trailPoints.size() - 1);
            float[] dist = new float[1];
            Location.distanceBetween(last.latitude, last.longitude, pt.latitude, pt.longitude, dist);
            if (dist[0] < 3.0f) return; // Ignore tiny sub-3m noise
        }
        trailPoints.add(pt);
        if (trailPoints.size() > 80) {
            trailPoints.remove(0);
        }
    }

    private void redrawTrailPolyline() {
        if (liveTrailPolyline != null) {
            liveTrailPolyline.remove();
            liveTrailPolyline = null;
        }
    }

    private void setupProfileAvatars() {
        database.child("profiles").child(myNodeName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.child("imageUrl").getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    myImageUrl = url;
                    Glide.with(LiveTrackingActivity.this)
                            .asBitmap()
                            .load(url)
                            .transform(new CircleCrop())
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    myProfileBitmap = resource;
                                    if (myMarker != null) {
                                        myMarker.setIcon(getCustomMarker(myProfileBitmap));
                                    }
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        database.child("profiles").child(partnerNodeName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.child("imageUrl").getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    partnerImageUrl = url;
                    ImageView imgAvatar = findViewById(R.id.imgLivePartnerAvatar);
                    if (imgAvatar != null) {
                        Glide.with(LiveTrackingActivity.this)
                                .load(url)
                                .placeholder(R.drawable.ic_photo_placeholder)
                                .transform(new CircleCrop())
                                .into(imgAvatar);
                    }
                    Glide.with(LiveTrackingActivity.this)
                            .asBitmap()
                            .load(url)
                            .transform(new CircleCrop())
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    partnerProfileBitmap = resource;
                                    if (partnerMarker != null) {
                                        partnerMarker.setIcon(getCustomMarker(partnerProfileBitmap));
                                    }
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {}
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupPartnerListeners() {
        partnerStatusRef = database.child("status").child(partnerNodeName).child("online");
        partnerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isPartnerOnline = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                updateTelemetryUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        partnerStatusRef.addValueEventListener(partnerStatusListener);

        partnerLocRef = database.child("locations").child(partnerNodeName);
        partnerLocListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Double lat = snapshot.child("lat").getValue(Double.class);
                Double lng = snapshot.child("lng").getValue(Double.class);
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                Double speed = snapshot.child("speed").getValue(Double.class);
                Double acc = snapshot.child("accuracy").getValue(Double.class);
                Integer batt = snapshot.child("battery").getValue(Integer.class);
                Boolean charging = snapshot.child("charging").getValue(Boolean.class);

                if (ts != null) partnerTimestamp = ts;
                if (speed != null) partnerSpeedMps = speed.floatValue();
                if (acc != null) partnerAccuracy = acc.floatValue();
                if (batt != null) partnerBattery = batt;
                if (charging != null) partnerCharging = charging;

                if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                    partnerLat = lat;
                    partnerLng = lng;
                    addPointToTrail(new LatLng(partnerLat, partnerLng));
                    updateMarkersOnMap(true);
                    updateTelemetryUI();
                    resolveAddressIfNeeded(partnerLat, partnerLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        partnerLocRef.addValueEventListener(partnerLocListener);
    }

    private void updateMarkersOnMap(boolean animateGlide) {
        if (mMap == null) return;

        LatLng mePos = new LatLng(myLat, myLng);
        LatLng targetPartnerPos = new LatLng(partnerLat, partnerLng);

        if (myMarker == null) {
            myMarker = mMap.addMarker(new MarkerOptions()
                    .position(mePos)
                    .title("Me")
                    .icon(getCustomMarker(myProfileBitmap)));
        } else {
            myMarker.setPosition(mePos);
        }

        if (partnerMarker == null) {
            partnerMarker = mMap.addMarker(new MarkerOptions()
                    .position(targetPartnerPos)
                    .title(partnerDisplayName)
                    .icon(getCustomMarker(partnerProfileBitmap)));
        }

        if (accuracyCircle == null) {
            accuracyCircle = mMap.addCircle(new CircleOptions()
                    .center(targetPartnerPos)
                    .radius(Math.max(8.0, partnerAccuracy))
                    .fillColor(0x22FF4081)
                    .strokeColor(0x66FF4081)
                    .strokeWidth(2f));
        } else {
            accuracyCircle.setRadius(Math.max(8.0, partnerAccuracy));
        }

        redrawTrailPolyline();

        if (!hasCenteredInitially) {
            hasCenteredInitially = true;
            partnerMarker.setPosition(targetPartnerPos);
            accuracyCircle.setCenter(targetPartnerPos);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetPartnerPos, 17f));
            return;
        }

        if (animateGlide && partnerMarker != null) {
            final LatLng startPos = partnerMarker.getPosition();
            float[] dist = new float[1];
            Location.distanceBetween(startPos.latitude, startPos.longitude,
                    targetPartnerPos.latitude, targetPartnerPos.longitude, dist);

            if (markerAnimator != null && markerAnimator.isRunning()) {
                markerAnimator.cancel();
            }

            // Smooth Swiggy/Zomato style gliding interpolation if moved between 0.5m and 5000m
            if (dist[0] >= 0.5f && dist[0] <= 5000f) {
                markerAnimator = ValueAnimator.ofFloat(0f, 1f);
                markerAnimator.setDuration(1600L);
                markerAnimator.setInterpolator(new LinearInterpolator());
                markerAnimator.addUpdateListener(animation -> {
                    float v = animation.getAnimatedFraction();
                    double lat = startPos.latitude + (targetPartnerPos.latitude - startPos.latitude) * v;
                    double lng = startPos.longitude + (targetPartnerPos.longitude - startPos.longitude) * v;
                    LatLng interpolated = new LatLng(lat, lng);
                    partnerMarker.setPosition(interpolated);
                    if (accuracyCircle != null) {
                        accuracyCircle.setCenter(interpolated);
                    }
                    if (isFollowingPartner && mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(interpolated));
                    }
                });
                markerAnimator.start();
            } else {
                partnerMarker.setPosition(targetPartnerPos);
                if (accuracyCircle != null) accuracyCircle.setCenter(targetPartnerPos);
                if (isFollowingPartner) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(targetPartnerPos));
                }
            }
        }
    }

    private void updateTelemetryUI() {
        TextView txtActivityBanner = findViewById(R.id.txtLiveActivityBanner);
        TextView txtDistance = findViewById(R.id.txtLiveChipDistance);
        TextView txtBattery = findViewById(R.id.txtLiveChipBattery);
        TextView txtAccuracy = findViewById(R.id.txtLiveChipAccuracy);
        TextView txtHeaderBadge = findViewById(R.id.txtLiveHeaderBadge);

        // 1. Distance
        float[] dist = new float[1];
        Location.distanceBetween(myLat, myLng, partnerLat, partnerLng, dist);
        if (txtDistance != null) {
            if (dist[0] < 1000f) {
                txtDistance.setText(Math.round(dist[0]) + " m");
            } else {
                txtDistance.setText(String.format(Locale.getDefault(), "%.1f km", dist[0] / 1000f));
            }
        }

        // 2. Speed & Activity Status (Swiggy/Zomato style)
        float speedKmh = partnerSpeedMps * 3.6f;
        long ageMs = System.currentTimeMillis() - partnerTimestamp;
        boolean isFresh = partnerTimestamp > 0 && ageMs < 60000L;

        if (txtActivityBanner != null) {
            if (!isPartnerOnline && !isFresh) {
                txtActivityBanner.setText("⚪ Offline • Last known location");
                txtActivityBanner.setTextColor(Color.parseColor("#888888"));
            } else if (speedKmh < 1.5f || !isFresh) {
                txtActivityBanner.setText("📍 Stationary");
                txtActivityBanner.setTextColor(Color.parseColor("#FF4081"));
            } else if (speedKmh < 7.0f) {
                txtActivityBanner.setText(String.format(Locale.getDefault(), "🚶 Walking • %.1f km/h", speedKmh));
                txtActivityBanner.setTextColor(Color.parseColor("#4CAF50"));
            } else if (speedKmh < 22.0f) {
                txtActivityBanner.setText(String.format(Locale.getDefault(), "🚲 Moving • %.1f km/h", speedKmh));
                txtActivityBanner.setTextColor(Color.parseColor("#FF9800"));
            } else {
                txtActivityBanner.setText(String.format(Locale.getDefault(), "🚗 On the move • %.0f km/h", speedKmh));
                txtActivityBanner.setTextColor(Color.parseColor("#E91E63"));
            }
        }

        // 3. Battery
        if (txtBattery != null) {
            if (partnerBattery >= 0) {
                txtBattery.setText((partnerCharging ? "⚡ " : "🔋 ") + partnerBattery + "%");
            } else {
                txtBattery.setText("🔋 --%");
            }
        }

        // 4. Accuracy
        if (txtAccuracy != null) {
            txtAccuracy.setText("🎯 ±" + Math.max(3, Math.round(partnerAccuracy)) + "m");
        }

        // 5. Header Badge
        if (txtHeaderBadge != null) {
            txtHeaderBadge.setText(isPartnerOnline || isFresh ? "LIVE RADAR • 2s" : "LAST SEEN");
        }

        updateFreshnessText();
    }

    private void updateFreshnessText() {
        TextView txtFreshness = findViewById(R.id.txtLiveFreshness);
        if (txtFreshness == null) return;
        if (partnerTimestamp == 0) {
            txtFreshness.setText("Waiting for " + partnerDisplayName + "'s GPS signal...");
            return;
        }
        long secAgo = Math.max(0L, (System.currentTimeMillis() - partnerTimestamp) / 1000L);
        if (secAgo < 5) {
            txtFreshness.setText("🟢 Live • Updated just now");
            txtFreshness.setTextColor(Color.parseColor("#2E7D32"));
        } else if (secAgo < 60) {
            txtFreshness.setText("🟢 Live • Updated " + secAgo + "s ago");
            txtFreshness.setTextColor(Color.parseColor("#2E7D32"));
        } else if (secAgo < 3600) {
            long mins = secAgo / 60;
            txtFreshness.setText("Updated " + mins + "m " + (secAgo % 60) + "s ago");
            txtFreshness.setTextColor(Color.parseColor("#777777"));
        } else {
            String timeStr = new java.text.SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
                    .format(new java.util.Date(partnerTimestamp));
            txtFreshness.setText("Last updated " + timeStr);
            txtFreshness.setTextColor(Color.parseColor("#888888"));
        }
    }

    private void resolveAddressIfNeeded(double lat, double lng) {
        float[] dist = new float[1];
        if (lastGeocodedLat != 0 || lastGeocodedLng != 0) {
            Location.distanceBetween(lastGeocodedLat, lastGeocodedLng, lat, lng, dist);
            if (dist[0] < 12.0f && lastGeocodedAddress != null) {
                return;
            }
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(LiveTrackingActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    String line = addr.getAddressLine(0);
                    if (line != null && !line.isEmpty()) {
                        lastGeocodedAddress = line;
                        lastGeocodedLat = lat;
                        lastGeocodedLng = lng;
                        runOnUiThread(() -> {
                            TextView txtAddr = findViewById(R.id.txtLiveAddress);
                            if (txtAddr != null) {
                                txtAddr.setText("📍 " + line);
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void startMyLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2500)
                .setMinUpdateDistanceMeters(0)
                .setWaitForAccurateLocation(false)
                .build();

        if (myLocationCallback == null) {
            myLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    for (Location loc : result.getLocations()) {
                        if (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0)) {
                            myLat = loc.getLatitude();
                            myLng = loc.getLongitude();
                            if (myMarker != null) {
                                myMarker.setPosition(new LatLng(myLat, myLng));
                            }
                            updateTelemetryUI();
                        }
                    }
                }
            };
        }
        try {
            fusedLocationClient.requestLocationUpdates(req, myLocationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activate 2-second High-Frequency "Live Watch" mode on partner's device
        database.child("location_requests").child(partnerNodeName).setValue(System.currentTimeMillis());
        mainHandler.removeCallbacks(liveWatchKeepAliveRunnable);
        mainHandler.post(liveWatchKeepAliveRunnable);

        mainHandler.removeCallbacks(freshnessTickerRunnable);
        mainHandler.post(freshnessTickerRunnable);

        startMyLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainHandler.removeCallbacks(liveWatchKeepAliveRunnable);
        mainHandler.removeCallbacks(freshnessTickerRunnable);
        if (database != null && partnerNodeName != null) {
            database.child("live_watch").child(partnerNodeName).removeValue();
        }
        if (fusedLocationClient != null && myLocationCallback != null) {
            fusedLocationClient.removeLocationUpdates(myLocationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (markerAnimator != null) markerAnimator.cancel();
        if (partnerLocRef != null && partnerLocListener != null) {
            partnerLocRef.removeEventListener(partnerLocListener);
        }
        if (partnerStatusRef != null && partnerStatusListener != null) {
            partnerStatusRef.removeEventListener(partnerStatusListener);
        }
    }

    private BitmapDescriptor getCustomMarker(Bitmap profileBitmap) {
        if (profileBitmap == null) {
            Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_heart_filled);
            if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE);
            int width = (int) (vectorDrawable.getIntrinsicWidth() * 1.5f);
            int height = (int) (vectorDrawable.getIntrinsicHeight() * 1.5f);
            vectorDrawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }

        View customMarkerView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.custom_marker, null);
        ImageView markerImage = customMarkerView.findViewById(R.id.markerImage);
        CardView markerCard = customMarkerView.findViewById(R.id.markerCard);

        markerCard.setVisibility(View.VISIBLE);
        markerImage.setImageBitmap(profileBitmap);

        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());

        Bitmap returnedBitmap = Bitmap.createBitmap(
                customMarkerView.getMeasuredWidth(),
                customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        customMarkerView.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(returnedBitmap);
    }
}
