package com.example.vishika;

public class LocationHistoryModel implements ListItem {
    private double lat;
    private double lng;
    private long timestamp;
    private String address;

    public LocationHistoryModel() {
    }

    public LocationHistoryModel(double lat, double lng, long timestamp) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int getItemType() {
        return TYPE_LOCATION;
    }
}
