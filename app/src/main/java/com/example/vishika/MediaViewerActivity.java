package com.example.vishika;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class MediaViewerActivity extends AppCompatActivity {

    private ImageView mediaViewerImage;
    private VideoView mediaViewerVideo;
    private ProgressBar mediaViewerProgress;
    private ImageButton btnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        mediaViewerImage = findViewById(R.id.mediaViewerImage);
        mediaViewerVideo = findViewById(R.id.mediaViewerVideo);
        mediaViewerProgress = findViewById(R.id.mediaViewerProgress);
        btnClose = findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> finish());

        String url = getIntent().getStringExtra("MEDIA_URL");
        String type = getIntent().getStringExtra("MEDIA_TYPE");

        if (url == null || type == null) {
            finish();
            return;
        }

        mediaViewerProgress.setVisibility(View.VISIBLE);

        if ("Photo".equals(type)) {
            mediaViewerImage.setVisibility(View.VISIBLE);
            
            // Using Glide to load image without caching it to disk as per requirements
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        mediaViewerProgress.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        mediaViewerProgress.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(mediaViewerImage);
                
        } else if ("Video".equals(type) || "Voice".equals(type)) {
            mediaViewerVideo.setVisibility(View.VISIBLE);
            
            if ("Voice".equals(type)) {
                mediaViewerImage.setVisibility(View.VISIBLE);
                mediaViewerImage.setImageResource(R.drawable.microphone_svgrepo_com);
            }
            
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(mediaViewerVideo);
            mediaViewerVideo.setMediaController(mediaController);
            
            if (url != null && url.contains("res.cloudinary.com") && url.contains("/upload/")) {
                url = url.replace("/upload/", "/upload/q_auto,vc_auto/");
            }

            mediaViewerVideo.setVideoURI(android.net.Uri.parse(url));
            
            mediaViewerVideo.setOnPreparedListener(mp -> {
                mediaViewerProgress.setVisibility(View.GONE);
                mp.setLooping(true);
                mediaViewerVideo.start();
            });
            
            mediaViewerVideo.setOnErrorListener((mp, what, extra) -> {
                mediaViewerProgress.setVisibility(View.GONE);
                return false;
            });
        }
    }
}
