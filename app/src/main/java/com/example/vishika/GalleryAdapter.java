package com.example.vishika;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private Context context;
    private List<Gift> gifts;

    public GalleryAdapter(Context context, List<Gift> gifts) {
        this.context = context;
        this.gifts = gifts;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Gift gift = gifts.get(position);
        
        String url = gift.getContent();
        boolean isVideo = "Video".equals(gift.getType());
        boolean isVoice = "Voice".equals(gift.getType());
        
        holder.icVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        if (isVoice) {
            holder.galleryImage.setImageResource(R.drawable.microphone_svgrepo_com);
        } else {
            holder.icVideo.setImageResource(android.R.drawable.ic_media_play);
            
            String displayUrl = url;
            if (isVideo && url != null) {
                // Cloudinary generates thumbnails by replacing extension with .jpg
                int lastDot = url.lastIndexOf('.');
                if (lastDot > 0 && lastDot > url.lastIndexOf('/')) {
                    displayUrl = url.substring(0, lastDot) + ".jpg";
                }
            }
            
            Glide.with(context)
                    .load(displayUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bg_button_answer)
                    .into(holder.galleryImage);
        }
                
        String senderName = "vishal".equals(gift.getSender()) ? "Vishal" : "Rashika";
        holder.txtSender.setText(senderName);
        
        SimpleDateFormat sdfTime = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
        holder.txtTime.setText(sdfTime.format(new Date(gift.getTimestamp())));
        
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MediaViewerActivity.class);
            intent.putExtra("MEDIA_URL", url);
            intent.putExtra("MEDIA_TYPE", gift.getType());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return gifts.size();
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView galleryImage, icVideo;
        TextView txtSender, txtTime;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            galleryImage = itemView.findViewById(R.id.galleryImage);
            icVideo = itemView.findViewById(R.id.icVideo);
            txtSender = itemView.findViewById(R.id.txtSender);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
