package com.example.vishika;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;

public class StoryCropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CROPPED_URI = "extra_cropped_uri";

    private PhotoView storyPhotoView;
    private View storyEditorRoot;
    private View topBar;
    private View bottomBar;
    private LinearLayout btnShareStory;
    private ImageButton btnClose;
    private boolean isExporting = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make full screen like Instagram Story
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        setContentView(R.layout.activity_story_crop);

        storyEditorRoot = findViewById(R.id.storyEditorRoot);
        storyPhotoView = findViewById(R.id.storyPhotoView);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
        btnShareStory = findViewById(R.id.btnShareStory);
        btnClose = findViewById(R.id.btnClose);

        // Configure PhotoView for fluid Instagram-like pinch zoom, pan & resize
        storyPhotoView.setMinimumScale(0.3f);
        storyPhotoView.setMediumScale(1.0f);
        storyPhotoView.setMaximumScale(8.0f);

        Uri sourceUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (sourceUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .load(sourceUri)
                .into(storyPhotoView);

        btnClose.setOnClickListener(v -> finish());

        btnShareStory.setOnClickListener(v -> {
            if (isExporting) return;
            exportAndFinish();
        });
    }

    private void exportAndFinish() {
        isExporting = true;
        btnShareStory.setEnabled(false);

        // Hide overlay UI so rendered screen bitmap only contains the positioned photo on black background
        topBar.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);

        storyEditorRoot.post(() -> {
            try {
                int width = storyPhotoView.getWidth();
                int height = storyPhotoView.getHeight();

                if (width <= 0 || height <= 0) {
                    width = storyEditorRoot.getWidth();
                    height = storyEditorRoot.getHeight();
                }

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.BLACK);
                storyPhotoView.draw(canvas);

                File outputFile = new File(getCacheDir(), "story_ig_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                fos.flush();
                fos.close();
                bitmap.recycle();

                Intent data = new Intent();
                data.putExtra(EXTRA_CROPPED_URI, Uri.fromFile(outputFile));
                setResult(Activity.RESULT_OK, data);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                topBar.setVisibility(View.VISIBLE);
                bottomBar.setVisibility(View.VISIBLE);
                btnShareStory.setEnabled(true);
                isExporting = false;
                Toast.makeText(StoryCropActivity.this, "Failed to render story", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
