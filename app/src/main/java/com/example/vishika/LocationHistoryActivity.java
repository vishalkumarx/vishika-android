package com.example.vishika;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHistoryActivity extends AppCompatActivity implements OnMapReadyCallback, LocationHistoryAdapter.OnLocationClickListener {

    private GoogleMap mMap;
    private DatabaseReference historyRef;
    private RecyclerView rvLocationHistory;
    private LocationHistoryAdapter adapter;
    private List<ListItem> displayList;
    private List<com.google.android.gms.maps.model.Marker> mapMarkers = new ArrayList<>();
    
    private Geocoder geocoder;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        geocoder = new Geocoder(this, Locale.getDefault());
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());

        ImageView btnBack = findViewById(R.id.btnBackHistory);
        btnBack.setOnClickListener(v -> finish());

        rvLocationHistory = findViewById(R.id.rvLocationHistory);
        rvLocationHistory.setLayoutManager(new LinearLayoutManager(this));
        displayList = new ArrayList<>();
        adapter = new LocationHistoryAdapter(this, displayList, this);
        rvLocationHistory.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.historyMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        String partnerName = MainActivity.IS_VISHAL ? "rashika" : "vishal";
        historyRef = FirebaseDatabase.getInstance().getReference("location_history").child(partnerName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadHistory();
    }

    @Override
    public void onLocationClick(LocationHistoryModel model) {
        if (mMap != null) {
            LatLng pt = new LatLng(model.getLat(), model.getLng());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pt, 16f));
            for (com.google.android.gms.maps.model.Marker marker : mapMarkers) {
                LatLng pos = marker.getPosition();
                if (Math.abs(pos.latitude - pt.latitude) < 0.000001 && Math.abs(pos.longitude - pt.longitude) < 0.000001) {
                    marker.showInfoWindow();
                    break;
                }
            }
        }
    }

    private void loadHistory() {
        ProgressBar pbLoading = findViewById(R.id.pbLoadingHistory);
        pbLoading.setVisibility(android.view.View.VISIBLE);
        
        historyRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pbLoading.setVisibility(android.view.View.GONE);
                if (!snapshot.exists()) {
                    Toast.makeText(LocationHistoryActivity.this, "No history found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                displayList.clear();
                mapMarkers.clear();
                List<LatLng> pathPoints = new ArrayList<>();
                List<LocationHistoryModel> rawHistoryList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double lat = data.child("lat").getValue(Double.class);
                    Double lng = data.child("lng").getValue(Double.class);
                    Long timestamp = data.child("timestamp").getValue(Long.class);
                    
                    if (lat != null && lng != null) {
                        LatLng pt = new LatLng(lat, lng);
                        pathPoints.add(pt);
                        
                        long ts = (timestamp != null) ? timestamp : 0L;
                        rawHistoryList.add(new LocationHistoryModel(lat, lng, ts));
                    }
                }

                if (rawHistoryList.isEmpty()) return;
                
                // Group by date
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Map<String, HeaderItem> headerMap = new java.util.LinkedHashMap<>();
                
                // Reverse to have newest entries first
                java.util.Collections.reverse(rawHistoryList);

                String todayStr = dateFormat.format(new java.util.Date());
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
                String yesterdayStr = dateFormat.format(cal.getTime());

                for (LocationHistoryModel model : rawHistoryList) {
                    String dateStr = dateFormat.format(new java.util.Date(model.getTimestamp()));
                    
                    String displayDateStr = dateStr;
                    if (dateStr.equals(todayStr)) {
                        displayDateStr = "Today";
                    } else if (dateStr.equals(yesterdayStr)) {
                        displayDateStr = "Yesterday";
                    }

                    if (!headerMap.containsKey(displayDateStr)) {
                        headerMap.put(displayDateStr, new HeaderItem(displayDateStr));
                    }
                    headerMap.get(displayDateStr).addChild(model);
                }

                HeaderItem headerToDisplay = null;
                for (HeaderItem header : headerMap.values()) {
                    displayList.add(header);
                    if ("Today".equals(header.getDateString())) {
                        header.setExpanded(true);
                        displayList.addAll(header.getChildItems());
                        headerToDisplay = header;
                    }
                }

                adapter.notifyDataSetChanged();

                if (headerToDisplay != null) {
                    updateMapForLocations(headerToDisplay.getChildItems());
                } else if (!headerMap.isEmpty()) {
                    updateMapForLocations(headerMap.values().iterator().next().getChildItems());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ProgressBar pbLoading = findViewById(R.id.pbLoadingHistory);
                pbLoading.setVisibility(android.view.View.GONE);
                Toast.makeText(LocationHistoryActivity.this, "Failed to load history.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHeaderClick(HeaderItem header) {
        if (header.isExpanded()) {
            updateMapForLocations(header.getChildItems());
        }
    }

    private void updateMapForLocations(List<LocationHistoryModel> locations) {
        if (mMap == null || locations == null || locations.isEmpty()) return;
        mMap.clear();
        mapMarkers.clear();

        List<LatLng> pathPoints = new ArrayList<>();
        for (LocationHistoryModel model : locations) {
            pathPoints.add(new LatLng(model.getLat(), model.getLng()));
        }

        // Draw Path (Polygon/Polyline)
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(pathPoints)
                .color(Color.parseColor("#FF4081")) // Pink line
                .width(10f)
                .geodesic(true);
        mMap.addPolyline(polylineOptions);

        // Add Markers for ALL places asynchronously to avoid blocking UI
        for (int i = 0; i < pathPoints.size(); i++) {
            final LatLng pt = pathPoints.get(i);
            final LocationHistoryModel model = locations.get(i);
            final boolean isStart = (i == 0);
            final boolean isEnd = (i == pathPoints.size() - 1);
            
            executorService.execute(() -> {
                String placeName = getAddressFromLocation(pt.latitude, pt.longitude);
                
                mainHandler.post(() -> {
                    float markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                    if (isStart) markerColor = BitmapDescriptorFactory.HUE_AZURE;
                    if (isEnd) markerColor = BitmapDescriptorFactory.HUE_RED;
                    
                    String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(model.getTimestamp()));
                    
                    com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(pt)
                            .title(timeStr)
                            .snippet(placeName)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                    if (marker != null) {
                        mapMarkers.add(marker);
                    }
                });
            });
        }

        // Move Camera to fit all points
        if (pathPoints.size() > 1) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng point : pathPoints) {
                boundsBuilder.include(point);
            }
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // offset from edges of the map in pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.get(0), 15f));
        }
    }
    
    private String getAddressFromLocation(double lat, double lng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                String result = address.getAddressLine(0);
                
                if (result == null || result.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    if (address.getFeatureName() != null) sb.append(address.getFeatureName()).append(", ");
                    if (address.getLocality() != null) sb.append(address.getLocality());
                    result = sb.toString();
                    if (result.endsWith(", ")) result = result.substring(0, result.length() - 2);
                }
                
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Dropped Pin";
    }
}
