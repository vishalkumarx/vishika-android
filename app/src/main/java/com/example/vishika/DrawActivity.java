package com.example.vishika;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.app.AlertDialog;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DrawActivity extends AppCompatActivity {

    public static boolean isVisible = false;
    
    private DrawingView drawingView;
    private View lastSelectedColorView;
    private int lastSelectedColor = android.graphics.Color.parseColor("#FF4081");
    private static final int[] COLOR_PALETTE = {
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
        0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
        0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800,
        0xFFFF5722, 0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF000000
    };
    private View activeColorIndicator;
    private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";
    private final okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient();
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePickerLauncher;
    private EditText hiddenEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_draw);

        drawingView = findViewById(R.id.drawingView);
        
        hiddenEditText = findViewById(R.id.hiddenEmojiEditText);
        hiddenEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (drawingView != null) {
                    drawingView.updateActiveObjectText(s.toString());
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadImageToSupabase(selectedImageUri);
                    }
                }
            }
        );
        


        View btnExit = findViewById(R.id.btnExit);
        View btnDraw = findViewById(R.id.btnDraw);
        View btnRainbow = findViewById(R.id.btnRainbow);
        View btnEraser = findViewById(R.id.btnEraser);
        View btnHeart = findViewById(R.id.btnHeart);
        View btnText = findViewById(R.id.btnText);
        View btnSticker = findViewById(R.id.btnSticker);
        View btnUndo = findViewById(R.id.btnUndo);
        View btnClear = findViewById(R.id.btnClear);
        SeekBar sliderSize = findViewById(R.id.sliderSize);
        
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        boolean isVishal = MainActivity.IS_VISHAL;
        String myId = isVishal ? "vishal" : "rashika";
        String partnerId = isVishal ? "rashika" : "vishal";
        
        if (drawingView != null) {
            drawingView.setup(database, myId, partnerId);
        }

        if (btnExit != null) {
            btnExit.setOnClickListener(v -> finish());
        }
        
        View[] toolBtns = {btnDraw, btnRainbow, btnEraser, btnHeart, btnText, btnSticker};
        
        btnDraw.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
                drawingView.currentTool = "path";
                if (drawingView.getCurrentColor() == android.graphics.Color.parseColor("#000001")) {
                    drawingView.setCurrentColor(lastSelectedColor);
                    if (activeColorIndicator != null) {
                        activeColorIndicator.setBackgroundResource(R.drawable.circle_shape);
                        activeColorIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(lastSelectedColor));
                    }
                }
            }
            updateToolSelection(toolBtns, btnDraw);
            sliderSize.setVisibility(android.view.View.VISIBLE);
        });
        
        btnRainbow.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
                drawingView.currentTool = "path";
                int rainbowColor = android.graphics.Color.parseColor("#000001");
                drawingView.setCurrentColor(rainbowColor);
                if (activeColorIndicator != null) {
                    activeColorIndicator.setBackgroundResource(R.drawable.bg_gradient_pink);
                    activeColorIndicator.setBackgroundTintList(null);
                }
            }
            updateToolSelection(toolBtns, btnRainbow);
            sliderSize.setVisibility(android.view.View.VISIBLE);
        });
        
        btnEraser.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
                drawingView.currentTool = "eraser";
            }
            updateToolSelection(toolBtns, btnEraser);
            sliderSize.setVisibility(android.view.View.VISIBLE);
        });
        
        btnHeart.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
                drawingView.currentTool = "heart";
                if (drawingView.getCurrentColor() == android.graphics.Color.parseColor("#000001")) {
                    drawingView.setCurrentColor(lastSelectedColor); // Reset if coming from rainbow
                }
            }
            updateToolSelection(toolBtns, btnHeart);
            sliderSize.setVisibility(android.view.View.VISIBLE);
        });
        
        btnText.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
                drawingView.currentTool = "text";
            }
            updateToolSelection(toolBtns, btnText);
            sliderSize.setVisibility(android.view.View.GONE);
        });
        
        btnSticker.setOnClickListener(v -> {
            if (drawingView != null) {
                drawingView.commitActiveObject();
            }
            openStickerPanel();
            if (drawingView != null) drawingView.currentTool = "image";
            updateToolSelection(toolBtns, btnSticker);
            sliderSize.setVisibility(android.view.View.GONE);
        });
        
        updateToolSelection(toolBtns, btnDraw); // Default
        
        drawingView.setOnTouchListener((v, event) -> {
            String t = drawingView.currentTool;
            if (t.equals("path") || t.equals("eraser") || t.equals("heart")) {
                return false;
            }
            return drawingView.handleInteractiveTouch(event);
        });

        btnUndo.setOnClickListener(v -> {
            if (drawingView != null) drawingView.undo();
        });
        
        btnClear.setOnClickListener(v -> {
            if (drawingView != null) drawingView.clearMyCanvas();
        });
        
        sliderSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Minimum size of 2, max 52
                if (drawingView != null) drawingView.setCurrentWidth(progress + 2f);
                updateSliderThumb(lastSelectedColor, progress + 2f);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        activeColorIndicator = findViewById(R.id.activeColorIndicator);
        findViewById(R.id.btnColorChooser).setOnClickListener(v -> openColorPickerPanel());
        
        // Random color picked from the palette (excluding the last entry, black)
        int defaultColor = COLOR_PALETTE[new java.util.Random().nextInt(COLOR_PALETTE.length - 1)];
        lastSelectedColor = defaultColor;
        if (drawingView != null) {
            drawingView.setCurrentColor(defaultColor);
        }
        if (activeColorIndicator != null) {
            activeColorIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(defaultColor));
        }
    }

    private void openColorPickerPanel() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        View view = getLayoutInflater().inflate(R.layout.dialog_color_palette, null);
        android.widget.GridLayout gridColors = view.findViewById(R.id.gridColors);
        
        int[] palette = COLOR_PALETTE;

        int size = (int) (50 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);

        for (int c : palette) {
            NeumorphicColorView colorView = new NeumorphicColorView(this);
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            colorView.setLayoutParams(params);
            colorView.setColor(c);
            
            // Replaced manual gradient drawable setup as NeumorphicColorView handles it internally
            
            colorView.setOnClickListener(v -> {
                lastSelectedColor = c;
                if (drawingView != null) {
                    drawingView.setCurrentColor(c);
                    drawingView.updateActiveObjectColor(c);
                    if ((drawingView.currentTool.equals("path") || drawingView.currentTool.equals("eraser") || drawingView.currentTool.equals("heart")) && drawingView.getCurrentColor() != android.graphics.Color.parseColor("#000001")) {
                        drawingView.currentTool = "path";
                        updateToolSelection(new View[]{findViewById(R.id.btnDraw), findViewById(R.id.btnRainbow), findViewById(R.id.btnEraser), findViewById(R.id.btnHeart), findViewById(R.id.btnText), findViewById(R.id.btnSticker)}, findViewById(R.id.btnDraw));
                    }
                }
                if (activeColorIndicator != null) {
                    activeColorIndicator.setBackgroundResource(R.drawable.circle_shape);
                    activeColorIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(c));
                }
                dialog.dismiss();
            });
            gridColors.addView(colorView);
        }
        
        dialog.setContentView(view);
        if (view.getParent() != null) {
            ((View) view.getParent()).setBackgroundColor(Color.TRANSPARENT);
        }
        dialog.show();
    }

    private void uploadImageToSupabase(android.net.Uri uri) {
        android.app.ProgressDialog dialog = new android.app.ProgressDialog(this);
        dialog.setMessage("Uploading Sticker...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageBytes = byteBuffer.toByteArray();
                
                String filename = "sticker_" + System.currentTimeMillis() + ".jpg";
                String bucket = "canvas_stickers";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + filename;
                
                okhttp3.RequestBody body = okhttp3.RequestBody.create(imageBytes, okhttp3.MediaType.parse("image/jpeg"));
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(uploadUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .build();
                        
                httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull java.io.IOException e) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(DrawActivity.this, "Upload failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull okhttp3.Response response) throws java.io.IOException {
                        runOnUiThread(() -> dialog.dismiss());
                        if (response.isSuccessful()) {
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + filename;
                            runOnUiThread(() -> {
                                if (drawingView != null) {
                                    drawingView.currentTool = "image";
                                    drawingView.currentImageUrl = publicUrl;
                                    updateToolSelection(new View[]{findViewById(R.id.btnDraw), findViewById(R.id.btnText), findViewById(R.id.btnSticker)}, findViewById(R.id.btnSticker));
                                    SeekBar sliderSize = findViewById(R.id.sliderSize);
                                    if (sliderSize != null) sliderSize.setVisibility(View.GONE);
                                    android.widget.Toast.makeText(DrawActivity.this, "Tap canvas to place image!", android.widget.Toast.LENGTH_LONG).show();
                                }
                                if (loadedStickers != null) {
                                    loadedStickers.add(0, publicUrl);
                                    if (stickerAdapter != null) stickerAdapter.notifyDataSetChanged();
                                }
                            });
                        } else {
                            String err = "Unknown error";
                            if (response.body() != null) err = response.body().string();
                            final String finalErr = err;
                            runOnUiThread(() -> android.widget.Toast.makeText(DrawActivity.this, "Upload failed: " + response.code() + " - " + finalErr, android.widget.Toast.LENGTH_LONG).show());
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(DrawActivity.this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }).start();
    }

    private class ThumbDrawable extends android.graphics.drawable.Drawable {
        private android.graphics.Paint paint;
        private float radius;
        private int intrinsicSize;

        public ThumbDrawable(int maxPx) {
            paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            intrinsicSize = maxPx;
            radius = intrinsicSize / 2f;
        }

        public void setColor(int color) {
            paint.setColor(color);
            invalidateSelf();
        }

        public void setRadius(float r) {
            radius = r;
            invalidateSelf();
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, paint);
        }

        @Override
        public void setAlpha(int alpha) { paint.setAlpha(alpha); }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

        @Override
        public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }

        @Override
        public int getIntrinsicWidth() { return intrinsicSize; }

        @Override
        public int getIntrinsicHeight() { return intrinsicSize; }
    }

    private ThumbDrawable sliderThumbDrawable;

    private void updateSliderThumb(int color, float sizeDp) {
        android.widget.SeekBar sliderSize = findViewById(R.id.sliderSize);
        if (sliderSize == null) return;
        
        int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);
        if (sizePx < 10) sizePx = 10; // minimum touch target visual
        
        if (sliderThumbDrawable == null) {
            int maxSizePx = (int) (60 * getResources().getDisplayMetrics().density); // max size container
            sliderThumbDrawable = new ThumbDrawable(maxSizePx);
            sliderSize.setThumb(sliderThumbDrawable);
            sliderSize.setThumbOffset(maxSizePx / 2);
        }
        
        sliderThumbDrawable.setColor(color);
        sliderThumbDrawable.setRadius(sizePx / 2f);
    }

    private com.google.android.material.bottomsheet.BottomSheetDialog stickerDialog;
    private StickerAdapter stickerAdapter;
    private java.util.List<String> loadedStickers = new java.util.ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView rvStickers;
    private android.widget.TextView textLoadingStickers;

    private void openStickerPanel() {
        if (stickerDialog == null) {
            stickerDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_sticker_panel, null);
            
            rvStickers = view.findViewById(R.id.rvStickers);
            textLoadingStickers = view.findViewById(R.id.textLoadingStickers);
            rvStickers.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 4));
            
            if (loadedStickers.isEmpty()) {
                textLoadingStickers.setVisibility(View.VISIBLE);
                rvStickers.setVisibility(View.GONE);
            } else {
                textLoadingStickers.setVisibility(View.GONE);
                rvStickers.setVisibility(View.VISIBLE);
            }
            
            stickerAdapter = new StickerAdapter(this, loadedStickers, new StickerAdapter.OnStickerClickListener() {
                @Override
                public void onAddClicked() {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    imagePickerLauncher.launch(intent);
                }

                @Override
                public void onStickerLongClicked(String url) {
                    new android.app.AlertDialog.Builder(DrawActivity.this)
                        .setTitle("Delete Sticker")
                        .setMessage("Are you sure you want to permanently delete this sticker from the cloud?")
                        .setPositiveButton("Delete", (d, w) -> deleteStickerFromSupabase(url))
                        .setNegativeButton("Cancel", null)
                        .show();
                }

                @Override
                public void onStickerClicked(String url) {
                    stickerDialog.dismiss();
                    if (drawingView != null) {
                        drawingView.currentTool = "image";
                        drawingView.currentImageUrl = url;
                        android.widget.Toast.makeText(DrawActivity.this, "Tap canvas to place image!", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
            rvStickers.setAdapter(stickerAdapter);
            stickerDialog.setContentView(view);
            
            loadStickersFromSupabase(rvStickers, textLoadingStickers);
        } else {
            loadStickersFromSupabase(rvStickers, textLoadingStickers); // refresh
        }
        stickerDialog.show();
    }
    
    private void deleteStickerFromSupabase(String publicUrl) {
        String filename = publicUrl.substring(publicUrl.lastIndexOf("/") + 1);
        String bucket = "canvas_stickers";
        String deleteUrl = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + filename;
        
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                .build();
                
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull java.io.IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(DrawActivity.this, "Delete failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull okhttp3.Response response) throws java.io.IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        loadedStickers.remove(publicUrl);
                        if (stickerAdapter != null) stickerAdapter.notifyDataSetChanged();
                        android.widget.Toast.makeText(DrawActivity.this, "Sticker deleted!", android.widget.Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> android.widget.Toast.makeText(DrawActivity.this, "Delete failed: " + response.code(), android.widget.Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadStickersFromSupabase(androidx.recyclerview.widget.RecyclerView rv, android.widget.TextView textLoading) {
        new Thread(() -> {
            try {
                String bucket = "canvas_stickers";
                String url = SUPABASE_URL + "/storage/v1/object/list/" + bucket;
                
                String jsonBody = "{\"prefix\":\"\",\"limit\":100,\"offset\":0,\"sortBy\":{\"column\":\"created_at\",\"order\":\"desc\"}}";
                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .build();
                        
                httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull java.io.IOException e) {
                        runOnUiThread(() -> {
                            if (textLoading != null) textLoading.setText("Failed to load stickers.");
                        });
                    }

                    @Override
                    public void onResponse(@androidx.annotation.NonNull okhttp3.Call call, @androidx.annotation.NonNull okhttp3.Response response) throws java.io.IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            try {
                                org.json.JSONArray array = new org.json.JSONArray(responseData);
                                loadedStickers.clear();
                                for (int i = 0; i < array.length(); i++) {
                                    org.json.JSONObject obj = array.getJSONObject(i);
                                    if (obj.has("name")) {
                                        String name = obj.getString("name");
                                        if (name.endsWith(".jpg") || name.endsWith(".png")) {
                                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucket + "/" + name;
                                            loadedStickers.add(publicUrl);
                                        }
                                    }
                                }
                                runOnUiThread(() -> {
                                    if (stickerAdapter != null) stickerAdapter.notifyDataSetChanged();
                                    if (rv != null) rv.setVisibility(android.view.View.VISIBLE);
                                    if (textLoading != null) textLoading.setVisibility(android.view.View.GONE);
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> { if (textLoading != null) textLoading.setText("Error parsing stickers."); });
                            }
                        } else {
                            runOnUiThread(() -> { if (textLoading != null) textLoading.setText("Failed to load stickers."); });
                        }
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

    private void updateToolSelection(View[] btns, View selected) {
        int colorSelected = Color.parseColor("#FF4081");
        int colorUnselected = Color.parseColor("#212121");
        
        for (View b : btns) {
            boolean isSelected = (b == selected);
            b.setAlpha(isSelected ? 1.0f : 0.5f);
            b.setScaleX(isSelected ? 1.1f : 1.0f);
            b.setScaleY(isSelected ? 1.1f : 1.0f);
            
            if (b.getBackground() != null) {
                b.getBackground().mutate().setTint(isSelected ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.WHITE);
            }
            
            if (b instanceof android.view.ViewGroup) {
                android.view.ViewGroup vg = (android.view.ViewGroup) b;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) child).setColorFilter(isSelected ? colorSelected : colorUnselected);
                    } else if (child instanceof android.widget.TextView) {
                        ((android.widget.TextView) child).setTextColor(isSelected ? colorSelected : colorUnselected);
                    }
                }
            }
        }
    }
    
    public void openKeyboardForActiveObject(String text) {
        hiddenEditText.setText(text != null ? text : "");
        if (text != null) hiddenEditText.setSelection(text.length());
        hiddenEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(hiddenEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    

    
    public void closeInputPanels() {

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(hiddenEditText.getWindowToken(), 0);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        if (MoodService.getInstance() != null) {
            MoodService.getInstance().setDrawingOverlayVisible(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
        if (MoodService.getInstance() != null) {
            MoodService.getInstance().setDrawingOverlayVisible(false);
        }
        if (drawingView != null) {
            drawingView.commitActiveObject();
        }
    }
}
