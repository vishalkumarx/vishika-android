package com.example.vishika;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(LocationHistoryModel model);
        void onHeaderClick(HeaderItem header);
    }

    private Context context;
    private List<ListItem> displayList;
    private SimpleDateFormat timeFormat;
    private Geocoder geocoder;
    private ExecutorService executorService;
    private Handler mainHandler;
    private OnLocationClickListener clickListener;

    public LocationHistoryAdapter(Context context, List<ListItem> displayList, OnLocationClickListener listener) {
        this.context = context;
        this.displayList = displayList;
        this.timeFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        this.geocoder = new Geocoder(context, Locale.getDefault());
        this.executorService = Executors.newFixedThreadPool(4); // 4 threads for geocoding
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.clickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position).getItemType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_location_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_location_history, parent, false);
            return new LocationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = displayList.get(position);

        if (holder.getItemViewType() == ListItem.TYPE_HEADER) {
            HeaderItem headerItem = (HeaderItem) item;
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeaderDate.setText(headerItem.getDateString());
            
            if (headerItem.isExpanded()) {
                headerHolder.ivHeaderChevron.setRotation(180f);
            } else {
                headerHolder.ivHeaderChevron.setRotation(0f);
            }

            headerHolder.itemView.setOnClickListener(v -> {
                int adapterPosition = headerHolder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                if (headerItem.isExpanded()) {
                    // Collapse
                    headerItem.setExpanded(false);
                    headerHolder.ivHeaderChevron.animate().rotation(0f).setDuration(200).start();
                    
                    int childCount = headerItem.getChildItems().size();
                    for (int i = 0; i < childCount; i++) {
                        displayList.remove(adapterPosition + 1);
                    }
                    notifyItemRangeRemoved(adapterPosition + 1, childCount);
                } else {
                    // Expand
                    headerItem.setExpanded(true);
                    headerHolder.ivHeaderChevron.animate().rotation(180f).setDuration(200).start();
                    
                    displayList.addAll(adapterPosition + 1, headerItem.getChildItems());
                    notifyItemRangeInserted(adapterPosition + 1, headerItem.getChildItems().size());

                    if (clickListener != null) {
                        clickListener.onHeaderClick(headerItem);
                    }
                }
            });

        } else {
            LocationHistoryModel model = (LocationHistoryModel) item;
            LocationViewHolder locationHolder = (LocationViewHolder) holder;
            
            // Format Time
            String timeStr = timeFormat.format(new Date(model.getTimestamp()));
            locationHolder.tvTime.setText(timeStr);

            // Fetch and show address
            if (model.getAddress() != null) {
                locationHolder.tvCoordinates.setText(model.getAddress());
            } else {
                locationHolder.tvCoordinates.setText("Loading address...");
                executorService.execute(() -> {
                    String addressStr = getAddressFromLocation(model.getLat(), model.getLng());
                    model.setAddress(addressStr);
                    mainHandler.post(() -> {
                        // Update only if view is still displaying this item (RecyclerView recycling)
                        if (locationHolder.getAdapterPosition() == position) {
                            locationHolder.tvCoordinates.setText(addressStr);
                        }
                    });
                });
            }

            locationHolder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onLocationClick(model);
                }
            });
        }
    }

    private String getAddressFromLocation(double lat, double lng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                String result = address.getAddressLine(0);
                
                // If getAddressLine is null for some reason, fallback to feature name or locality
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
        // Fallback if geocoding fails
        return String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f", lat, lng);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvCoordinates;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderDate;
        ImageView ivHeaderChevron;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderDate = itemView.findViewById(R.id.tvHeaderDate);
            ivHeaderChevron = itemView.findViewById(R.id.ivHeaderChevron);
        }
    }
}
