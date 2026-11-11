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

        String partnerName = MainActivity.IS_VISHAL ? "rashika" : "vishal";
        FirebaseDatabase.getInstance().getReference("location_requests").child(partnerName).setValue(System.currentTimeMillis());
        DatabaseReference liveLocRef = FirebaseDatabase.getInstance().getReference("locations").child(partnerName);

        historyRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                liveLocRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot liveSnap) {
                        pbLoading.setVisibility(android.view.View.GONE);

                        displayList.clear();
                        mapMarkers.clear();
                        List<LocationHistoryModel> rawHistoryList = new ArrayList<>();

                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Double lat = data.child("lat").getValue(Double.class);
                                Double lng = data.child("lng").getValue(Double.class);
                                Long timestamp = data.child("timestamp").getValue(Long.class);

                                if (lat != null && lng != null) {
                                    long ts = (timestamp != null) ? timestamp : 0L;
                                    rawHistoryList.add(new LocationHistoryModel(lat, lng, ts));
                                }
                            }
                        }

                        if (liveSnap.exists()) {
                            Double liveLat = liveSnap.child("lat").getValue(Double.class);
                            Double liveLng = liveSnap.child("lng").getValue(Double.class);
                            Long liveTs = liveSnap.child("timestamp").getValue(Long.class);
                            if (liveLat != null && liveLng != null && liveTs != null) {
                                long newestHistoryTs = rawHistoryList.isEmpty() ? 0L : rawHistoryList.get(rawHistoryList.size() - 1).getTimestamp();
                                if (liveTs > newestHistoryTs) {
                                    rawHistoryList.add(new LocationHistoryModel(liveLat, liveLng, liveTs));
                                }
                            }
                        }

                        if (rawHistoryList.isEmpty()) {
                            Toast.makeText(LocationHistoryActivity.this, "No history found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

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
                        pbLoading.setVisibility(android.view.View.GONE);
                    }
                });
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

        // 1. Sort locations chronologically (oldest to newest) for a proper forward timeline
        List<LocationHistoryModel> sortedList = new ArrayList<>(locations);
        java.util.Collections.sort(sortedList, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        // 2. Filter out stationary jitter (require at least 25 meters movement between successive points)
        List<LocationHistoryModel> filteredLocations = new ArrayList<>();
        LocationHistoryModel lastAdded = null;
        for (LocationHistoryModel model : sortedList) {
            if (lastAdded == null) {
                filteredLocations.add(model);
                lastAdded = model;
            } else {
                float[] dist = new float[1];
                android.location.Location.distanceBetween(lastAdded.getLat(), lastAdded.getLng(),
                        model.getLat(), model.getLng(), dist);
                if (dist[0] >= 25.0f) { // Only keep if moved at least 25 meters
                    filteredLocations.add(model);
                    lastAdded = model;
                }
            }
        }

        // Always ensure the latest point is included
        if (filteredLocations.isEmpty() || !filteredLocations.contains(sortedList.get(sortedList.size() - 1))) {
            filteredLocations.add(sortedList.get(sortedList.size() - 1));
        }

        List<LatLng> pathPoints = new ArrayList<>();
        for (LocationHistoryModel model : filteredLocations) {
            pathPoints.add(new LatLng(model.getLat(), model.getLng()));
        }

        // Draw direct, smooth path polyline connecting filtered locations
        if (pathPoints.size() > 1) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(pathPoints)
                    .color(Color.parseColor("#E91E63")) // Vibrant, clean rose pink
                    .width(8f)
                    .geodesic(true)
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                    .endCap(new com.google.android.gms.maps.model.RoundCap());
            mMap.addPolyline(polylineOptions);
        }

        // Determine which points to add markers for (Start, End, and max 3 intermediate points)
        List<Integer> markerIndices = new ArrayList<>();
        if (pathPoints.size() > 0) markerIndices.add(0);
        if (pathPoints.size() > 1) markerIndices.add(pathPoints.size() - 1);
        
        int maxIntermediateMarkers = 3;
        if (pathPoints.size() > 2) {
            int step = Math.max(1, pathPoints.size() / (maxIntermediateMarkers + 1));
            for (int i = step; i < pathPoints.size() - 1 && markerIndices.size() < maxIntermediateMarkers + 2; i += step) {
                if (!markerIndices.contains(i)) {
                    markerIndices.add(i);
                }
            }
        }

        // Add Markers asynchronously to avoid blocking UI
        for (int i : markerIndices) {
            final LatLng pt = pathPoints.get(i);
            final LocationHistoryModel model = filteredLocations.get(i);
            final boolean isStart = (i == 0);
            final boolean isEnd = (i == pathPoints.size() - 1);
            
            executorService.execute(() -> {
                String placeName = getAddressFromLocation(pt.latitude, pt.longitude);
                
                mainHandler.post(() -> {
                    float markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                    if (isStart) markerColor = BitmapDescriptorFactory.HUE_AZURE;
                    if (isEnd) markerColor = BitmapDescriptorFactory.HUE_RED;
                    
                    String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(model.getTimestamp()));
                    
                    if (mMap != null) {
                        com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(pt)
                                .title(timeStr + (isStart ? " (Start)" : (isEnd ? " (End)" : "")))
                                .snippet(placeName)
                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                        if (marker != null) {
                            mapMarkers.add(marker);
                        }
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
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return "Dropped Pin";
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
