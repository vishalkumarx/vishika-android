package com.example.vishika;

import static com.example.vishika.MainActivity.IS_VISHAL;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CultureActivity extends AppCompatActivity {

    private boolean isEditingMyCulture;
    private String targetNode;
    
    private TextView textTitle, textSubtitle, textNoMedia, textDescription, textCultureName;
    // imgCultureMedia removed
    private WebView webviewCulture;
    private LinearLayout layoutEditControls;
    
    private EditText editYoutubeLink, editDescription, editCultureName;
    private Button btnUploadImage, btnSaveCulture;
    
    private DatabaseReference cultureRef;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    
    private java.util.List<String> currentUploadedImageUrls = new java.util.ArrayList<>();
    private CultureImageAdapter cultureImageAdapter;
    private androidx.viewpager2.widget.ViewPager2 vpCultureMedia;
    private com.google.android.material.tabs.TabLayout tabLayoutCultureMedia;
    private LinearLayout layoutCultureImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_culture);
        
        isEditingMyCulture = getIntent().getBooleanExtra("IS_EDITING", false);
        
        // Node: culture/vishal or culture/rashika
        String myName = IS_VISHAL ? "vishal" : "rashika";
        String partnerName = IS_VISHAL ? "rashika" : "vishal";
        
        targetNode = isEditingMyCulture ? myName : partnerName;
        cultureRef = FirebaseDatabase.getInstance().getReference("culture").child(targetNode);
        
        textTitle = findViewById(R.id.textTitle);
        textSubtitle = findViewById(R.id.textSubtitle);
        textNoMedia = findViewById(R.id.textNoMedia);
        textDescription = findViewById(R.id.textDescription);
        textCultureName = findViewById(R.id.textCultureName);
        vpCultureMedia = findViewById(R.id.vpCultureMedia);
        tabLayoutCultureMedia = findViewById(R.id.tabLayoutCultureMedia);
        layoutCultureImages = findViewById(R.id.layoutCultureImages);
        
        cultureImageAdapter = new CultureImageAdapter(this, currentUploadedImageUrls, isEditingMyCulture, position -> {
            currentUploadedImageUrls.remove(position);
            updateImagePreviewUI();
        });
        vpCultureMedia.setAdapter(cultureImageAdapter);
        new com.google.android.material.tabs.TabLayoutMediator(tabLayoutCultureMedia, vpCultureMedia, (tab, position) -> {}).attach();
        webviewCulture = findViewById(R.id.webviewCulture);
        layoutEditControls = findViewById(R.id.layoutEditControls);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        editYoutubeLink = findViewById(R.id.editYoutubeLink);
        editDescription = findViewById(R.id.editDescription);
        editCultureName = findViewById(R.id.editCultureName);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnSaveCulture = findViewById(R.id.btnSaveCulture);
        
        android.widget.RadioGroup rgMediaType = findViewById(R.id.rgMediaType);
        rgMediaType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbYoutube) {
                editYoutubeLink.setVisibility(View.VISIBLE);
                btnUploadImage.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbImage) {
                editYoutubeLink.setVisibility(View.GONE);
                btnUploadImage.setVisibility(View.VISIBLE);
            }
        });
        
        setupWebView();
        
        if (isEditingMyCulture) {
            textTitle.setText("My Culture");
            textSubtitle.setText("know my culture");
            textSubtitle.setVisibility(View.VISIBLE);
            layoutEditControls.setVisibility(View.VISIBLE);
            textDescription.setVisibility(View.GONE); // Hide textview since we have edittext
            textCultureName.setVisibility(View.GONE);
        } else {
            textTitle.setText("Partner's Culture");
            textSubtitle.setVisibility(View.GONE);
            layoutEditControls.setVisibility(View.GONE);
            textDescription.setVisibility(View.VISIBLE);
            textCultureName.setVisibility(View.VISIBLE);
        }
        
        boolean isAdding = getIntent().getBooleanExtra("IS_ADDING", false);
        if (isAdding) {
            textNoMedia.setVisibility(View.VISIBLE);
        } else {
            loadCultureData();
        }
        
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri croppedUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                        if (croppedUri != null) {
                            currentUploadedImageUrls.add(croppedUri.toString());
                            updateImagePreviewUI();
                        }
                    }
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                currentUploadedImageUrls.add(uri.toString());
                            }
                            updateImagePreviewUI();
                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            startCrop(uri);
                        }
                    }
                }
        );
        
        btnUploadImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });
        
        btnSaveCulture.setOnClickListener(v -> saveCultureData());
    }
    
    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new java.io.File(getCacheDir(), "crop_culture_" + System.currentTimeMillis() + ".jpg"));
        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setCompressionQuality(80);
        options.setToolbarTitle("Crop Image");
        options.setToolbarColor(android.graphics.Color.parseColor("#1A1A1A"));
        options.setToolbarWidgetColor(android.graphics.Color.parseColor("#FFFFFF"));
        options.setStatusBarColor(android.graphics.Color.parseColor("#000000"));
        options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF5C81"));
        options.setAspectRatioOptions(0,
                new com.yalantis.ucrop.model.AspectRatio("1:1", 1, 1),
                new com.yalantis.ucrop.model.AspectRatio("2:3", 2, 3),
                new com.yalantis.ucrop.model.AspectRatio("4:5", 4, 5)
        );
        Intent uCropIntent = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(this);
        cropImageLauncher.launch(uCropIntent);
    }
    
    private void setupWebView() {
        webviewCulture.getSettings().setJavaScriptEnabled(true);
        webviewCulture.getSettings().setDomStorageEnabled(true);
        webviewCulture.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webviewCulture.getSettings().setPluginState(WebSettings.PluginState.ON);
        webviewCulture.setWebChromeClient(new WebChromeClient());
    }
    
    private void loadCultureData() {
        cultureRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    textNoMedia.setVisibility(View.VISIBLE);
                    return;
                }
                
                String mediaType = snapshot.child("mediaType").getValue(String.class);
                String mediaUrl = snapshot.child("mediaUrl").getValue(String.class);
                String description = snapshot.child("description").getValue(String.class);
                String cultureName = snapshot.child("cultureName").getValue(String.class);
                
                textNoMedia.setVisibility(View.GONE);
                
                if ("youtube".equals(mediaType) && mediaUrl != null) {
                    layoutCultureImages.setVisibility(View.GONE);
                    webviewCulture.setVisibility(View.VISIBLE);
                    loadYoutubeVideo(mediaUrl);
                    if (isEditingMyCulture) {
                        editYoutubeLink.setText(mediaUrl);
                        currentUploadedImageUrls.clear();
                        cultureImageAdapter.notifyDataSetChanged();
                        ((android.widget.RadioButton)findViewById(R.id.rbYoutube)).setChecked(true);
                    }
                } else if ("image".equals(mediaType) && mediaUrl != null) {
                    webviewCulture.setVisibility(View.GONE);
                    webviewCulture.loadUrl("about:blank");
                    
                    String[] urls = mediaUrl.split(",");
                    currentUploadedImageUrls.clear();
                    currentUploadedImageUrls.addAll(java.util.Arrays.asList(urls));
                    cultureImageAdapter.notifyDataSetChanged();
                    
                    layoutCultureImages.setVisibility(View.VISIBLE);
                    
                    if (isEditingMyCulture) {
                        editYoutubeLink.setText("");
                        ((android.widget.RadioButton)findViewById(R.id.rbImage)).setChecked(true);
                    }
                } else {
                    textNoMedia.setVisibility(View.VISIBLE);
                }
                
                if (cultureName != null) {
                    if (isEditingMyCulture) {
                        editCultureName.setText(cultureName);
                    } else {
                        textCultureName.setText(capitalizeWords(cultureName));
                    }
                } else {
                    textCultureName.setVisibility(View.GONE);
                }
                
                if (description != null) {
                    if (isEditingMyCulture) {
                        editDescription.setText(description);
                    } else {
                        textDescription.setText(description);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void loadYoutubeVideo(String url) {
        String videoId = extractYoutubeId(url);
        if (videoId != null) {
            String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body style='margin:0;padding:0;'><iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/" + videoId + "?autoplay=0&controls=1&rel=0&origin=https://www.youtube.com\" frameborder=\"0\" allow=\"fullscreen\"></iframe></body></html>";
            webviewCulture.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null);
        }
    }
    
    private String extractYoutubeId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    private void saveCultureData() {
        String ytLink = editYoutubeLink.getText().toString().trim();
        String desc = editDescription.getText().toString().trim();
        String cultureName = editCultureName.getText().toString().trim();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", desc);
        updates.put("cultureName", cultureName);
        
        boolean isYoutube = ((android.widget.RadioButton)findViewById(R.id.rbYoutube)).isChecked();
        
        if (isYoutube) {
            if (!ytLink.isEmpty()) {
                updates.put("mediaType", "youtube");
                updates.put("mediaUrl", ytLink);
                saveToFirebase(updates, cultureName, desc);
            } else {
                Toast.makeText(this, "Please enter a YouTube link", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (!currentUploadedImageUrls.isEmpty()) {
                uploadLocalImagesAndSave(updates, cultureName, desc);
            } else {
                Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void updateImagePreviewUI() {
        cultureImageAdapter.notifyDataSetChanged();
        if (!currentUploadedImageUrls.isEmpty()) {
            vpCultureMedia.setCurrentItem(currentUploadedImageUrls.size() - 1, true);
            webviewCulture.setVisibility(View.GONE);
            webviewCulture.loadUrl("about:blank");
            layoutCultureImages.setVisibility(View.VISIBLE);
            textNoMedia.setVisibility(View.GONE);
            btnUploadImage.setText("Add More Images");
        } else {
            layoutCultureImages.setVisibility(View.GONE);
            textNoMedia.setVisibility(View.VISIBLE);
            btnUploadImage.setText("Upload Image");
        }
    }
    
    private void uploadLocalImagesAndSave(Map<String, Object> updates, String cultureName, String desc) {
        List<String> localUris = new ArrayList<>();
        for (String url : currentUploadedImageUrls) {
            if (!url.startsWith("http")) {
                localUris.add(url);
            }
        }
        
        if (localUris.isEmpty()) {
            updates.put("mediaType", "image");
            updates.put("mediaUrl", android.text.TextUtils.join(",", currentUploadedImageUrls));
            saveToFirebase(updates, cultureName, desc);
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading Images...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            try {
                String cloudName = "q7btslrv";
                String uploadPreset = "vishika_uploads";
                String urlString = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";
                
                for (int i = 0; i < currentUploadedImageUrls.size(); i++) {
                    String urlOrUri = currentUploadedImageUrls.get(i);
                    if (!urlOrUri.startsWith("http")) {
                        URL url = new URL(urlString);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setDoOutput(true);
                        conn.setChunkedStreamingMode(4096);
                        
                        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        
                        OutputStream os = conn.getOutputStream();
                        PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(os, "UTF-8"), true);
                        
                        writer.append("--").append(boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                        writer.append(uploadPreset).append("\r\n");
                        
                        writer.append("--").append(boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"image\"\r\n");
                        writer.append("Content-Type: application/octet-stream\r\n\r\n");
                        writer.flush();
                        
                        Uri uri = Uri.parse(urlOrUri);
                        InputStream is = getContentResolver().openInputStream(uri);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                        is.close();
                        
                        writer.append("\r\n").append("--").append(boundary).append("--\r\n");
                        writer.close();
                        
                        if (conn.getResponseCode() == 200) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String inputLine;
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) response.append(inputLine);
                            in.close();
                            
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            String secureUrl = jsonResponse.getString("secure_url");
                            currentUploadedImageUrls.set(i, secureUrl);
                        } else {
                            throw new Exception("Upload failed for one of the images");
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    updates.put("mediaType", "image");
                    updates.put("mediaUrl", android.text.TextUtils.join(",", currentUploadedImageUrls));
                    saveToFirebase(updates, cultureName, desc);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(CultureActivity.this, "Error uploading images", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveToFirebase(Map<String, Object> updates, String cultureName, String desc) {
        cultureRef.setValue(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isAdding = getIntent().getBooleanExtra("IS_ADDING", false);
                if (isAdding) {
                    DatabaseReference feedRef = FirebaseDatabase.getInstance().getReference("feed_posts");
                    String postId = feedRef.push().getKey();
                    if (postId != null) {
                        String mediaType = updates.get("mediaType").toString();
                        String mediaUrl = updates.get("mediaUrl").toString();
                        String currentUserName = IS_VISHAL ? "Vishal" : "Rashika";
                        String currentUserId = IS_VISHAL ? "vishal" : "rashika";
                        
                        String title = cultureName.isEmpty() ? "Culture Exchange" : "Culture Exchange: " + cultureName;
                        FeedPost newPost = new FeedPost(postId, currentUserId, currentUserName, mediaUrl, desc, title, System.currentTimeMillis(), mediaType);
                        feedRef.child(postId).setValue(newPost);
                    }
                }
                Toast.makeText(CultureActivity.this, "Culture updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CultureActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
