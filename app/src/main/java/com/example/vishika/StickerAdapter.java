package com.example.vishika;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private OnStickerClickListener listener;

    public interface OnStickerClickListener {
        void onAddClicked();
        void onStickerClicked(String url);
        void onStickerLongClicked(String url);
    }

    public StickerAdapter(Context context, List<String> imageUrls, OnStickerClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sticker, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerViewHolder holder, int position) {
        if (position == 0) {
            // Add button
            holder.ivSticker.setVisibility(View.GONE);
            holder.ivAdd.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddClicked();
            });
        } else {
            // Sticker
            holder.ivSticker.setVisibility(View.VISIBLE);
            holder.ivAdd.setVisibility(View.GONE);
            String url = imageUrls.get(position - 1);
            
            Glide.with(context)
                .load(url)
                .into(holder.ivSticker);
                
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStickerClicked(url);
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onStickerLongClicked(url);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size() + 1; // +1 for the Add button
    }

    public static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSticker;
        ImageView ivAdd;

        public StickerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSticker = itemView.findViewById(R.id.ivSticker);
            ivAdd = itemView.findViewById(R.id.ivAdd);
        }
    }
}
