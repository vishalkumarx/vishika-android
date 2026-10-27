package com.example.vishika;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import com.bumptech.glide.Glide;

import java.util.List;

public class CultureImageAdapter extends RecyclerView.Adapter<CultureImageAdapter.ViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private boolean isEditing;
    private OnImageDeleteListener deleteListener;

    public interface OnImageDeleteListener {
        void onDelete(int position);
    }

    public CultureImageAdapter(Context context, List<String> imageUrls) {
        this(context, imageUrls, false, null);
    }

    public CultureImageAdapter(Context context, List<String> imageUrls, boolean isEditing, OnImageDeleteListener deleteListener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.isEditing = isEditing;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_culture_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        Glide.with(context).load(url).into(holder.imgCultureItem);
        
        holder.imgCultureItem.setOnClickListener(v -> {
            Intent intent = new Intent(context, MediaViewerActivity.class);
            intent.putExtra("MEDIA_URL", url);
            intent.putExtra("MEDIA_TYPE", "Photo");
            context.startActivity(intent);
        });

        if (isEditing) {
            holder.btnDeleteImage.setVisibility(View.VISIBLE);
            holder.btnDeleteImage.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(position);
                }
            });
        } else {
            holder.btnDeleteImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCultureItem;
        ImageView btnDeleteImage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCultureItem = itemView.findViewById(R.id.imgCultureItem);
            btnDeleteImage = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
