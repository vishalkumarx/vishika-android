package com.example.vishika;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.yalantis.ucrop.UCrop;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvBioVishal, tvNicknameVishal, tvDobVishal, tvAnniversaryVishal;
    private TextView tvBioRashika, tvNicknameRashika, tvDobRashika, tvAnniversaryRashika;
    private ImageView imgVishal, imgRashika;
    
    private DatabaseReference mDatabase;
    private OkHttpClient httpClient = new OkHttpClient();
    
    // TODO: REPLACE THESE WITH YOUR ACTUAL SUPABASE CREDENTIALS!
    private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";
    private static final String BUCKET_NAME = "profile_images";
    
    private Uri selectedImageUri;
    private ImageView currentDialogImageView;
    private String currentlyEditingUserId;
    

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> cropImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri croppedUri = UCrop.getOutput(result.getData());
                    if (croppedUri != null) {
                        selectedImageUri = croppedUri;
                        if (currentDialogImageView != null) {
                            Glide.with(this).load(croppedUri).transform(new CircleCrop()).into(currentDialogImageView);
                        }
                    }
                }
            }
    );

    private ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    String destFileName = "ucrop_" + System.currentTimeMillis() + ".jpg";
                    android.net.Uri destUri = android.net.Uri.fromFile(new java.io.File(getCacheDir(), destFileName));
                    
                    com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
                    options.setHideBottomControls(false);
                    options.setFreeStyleCropEnabled(false);
                    options.setCircleDimmedLayer(true);
                    options.setToolbarColor(android.graphics.Color.parseColor("#FF4081"));
                    options.setStatusBarColor(android.graphics.Color.parseColor("#C51162"));
                    options.setToolbarWidgetColor(android.graphics.Color.WHITE);
                    options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#FF4081"));
                    
                    android.content.Intent uCropIntent = UCrop.of(uri, destUri)
                        .withOptions(options)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(500, 500)
                        .getIntent(ProfileActivity.this);
                    cropImageLauncher.launch(uCropIntent);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvBioVishal = findViewById(R.id.tvBioVishal);
                tvNicknameVishal = findViewById(R.id.tvNicknameVishal);
        tvDobVishal = findViewById(R.id.tvDobVishal);
        tvAnniversaryVishal = findViewById(R.id.tvAnniversaryVishal);
        imgVishal = findViewById(R.id.imgVishal);

        tvBioRashika = findViewById(R.id.tvBioRashika);
                tvNicknameRashika = findViewById(R.id.tvNicknameRashika);
        tvDobRashika = findViewById(R.id.tvDobRashika);
        tvAnniversaryRashika = findViewById(R.id.tvAnniversaryRashika);
        imgRashika = findViewById(R.id.imgRashika);

        FrameLayout navHome = findViewById(R.id.navHome);
        FrameLayout navFeed = findViewById(R.id.navFeed);
        
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        
        navFeed.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    
        
        mDatabase = FirebaseDatabase.getInstance().getReference().child("profiles");
        
        // Listen to Vishal's Profile
        mDatabase.child("vishal").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateProfileUI(snapshot, tvBioVishal, tvNicknameVishal, tvDobVishal, tvAnniversaryVishal, imgVishal);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Listen to Rashika's Profile
        mDatabase.child("rashika").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateProfileUI(snapshot, tvBioRashika, tvNicknameRashika, tvDobRashika, tvAnniversaryRashika, imgRashika);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        findViewById(R.id.btnEditVishal).setOnClickListener(v -> showEditDialog("vishal", "Vishal"));
        findViewById(R.id.btnEditRashika).setOnClickListener(v -> showEditDialog("rashika", "Rashika"));
    }
    
    private void updateProfileUI(DataSnapshot snapshot, TextView bio, TextView nick, TextView dob, TextView anniv, ImageView img) {
        if (snapshot.exists()) {
            String bioStr = snapshot.child("bio").getValue(String.class);
            String nickStr = snapshot.child("nickname").getValue(String.class);
            String dobStr = snapshot.child("dob").getValue(String.class);
            String annivStr = snapshot.child("anniversary").getValue(String.class);
            String imageUrl = snapshot.child("imageUrl").getValue(String.class);
            
            if (bioStr != null) bio.setText(bioStr);
            if (nickStr != null) nick.setText("Nickname: " + nickStr);
            if (dobStr != null) dob.setText("DOB: " + dobStr);
            if (annivStr != null) anniv.setText("Anniversary: " + annivStr);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).transform(new CircleCrop()).into(img);
            }
        }
    }
    
    private void showDatePicker(TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            targetTextView.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    
    private void showEditDialog(String userId, String name) {
        currentlyEditingUserId = userId;
        selectedImageUri = null;
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        
        TextView tvTitle = dialog.findViewById(R.id.tvEditTitle);
        EditText etBio = dialog.findViewById(R.id.etBio);
        EditText etNickname = dialog.findViewById(R.id.etNickname);
        TextView tvDob = dialog.findViewById(R.id.tvDob);
        TextView tvAnniversary = dialog.findViewById(R.id.tvAnniversary);
        Button btnUploadImage = dialog.findViewById(R.id.btnUploadImage);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        ImageView imgEditProfile = dialog.findViewById(R.id.imgEditProfile);
        
        currentDialogImageView = imgEditProfile;
        currentlyEditingUserId = userId;
        selectedImageUri = null;
        
        tvTitle.setText("Edit " + name + "'s Profile");
        
        // Setup Date Pickers
        tvDob.setOnClickListener(v -> showDatePicker(tvDob));
        tvAnniversary.setOnClickListener(v -> showDatePicker(tvAnniversary));
        
        btnUploadImage.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        
        // Pre-fill existing data
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String bio = snapshot.child("bio").getValue(String.class);
                    String nick = snapshot.child("nickname").getValue(String.class);
                    String dob = snapshot.child("dob").getValue(String.class);
                    String anniv = snapshot.child("anniversary").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    
                    if (bio != null) etBio.setText(bio);
                    if (nick != null) etNickname.setText(nick);
                    if (dob != null) tvDob.setText(dob);
                    if (anniv != null) tvAnniversary.setText(anniv);
                    
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(imageUrl).transform(new CircleCrop()).into(currentDialogImageView);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            btnSave.setText("Saving...");
            btnSave.setEnabled(false);
            
            String newBio = etBio.getText().toString().trim();
            String newNick = etNickname.getText().toString().trim();
            String newDob = tvDob.getText().toString().trim();
            String newAnniv = tvAnniversary.getText().toString().trim();
            
            mDatabase.child(userId).child("bio").setValue(newBio);
            mDatabase.child(userId).child("nickname").setValue(newNick);
            mDatabase.child(userId).child("dob").setValue(newDob);
            
            mDatabase.child(userId).child("anniversary").setValue(newAnniv);
            String partnerId = userId.equals("vishal") ? "rashika" : "vishal";
            mDatabase.child(partnerId).child("anniversary").setValue(newAnniv);

            
            if (selectedImageUri != null) {
                uploadImageToSupabase(userId, dialog);
            } else {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        
        dialog.show();
    }
    
    private void uploadImageToSupabase(String userId, Dialog dialog) {
        if (SUPABASE_URL.equals("YOUR_SUPABASE_URL")) {
            Toast.makeText(this, "Please insert your Supabase credentials in ProfileActivity.java!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageBytes = byteBuffer.toByteArray();
                
                String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + filename;
                
                RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .build();
                        
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProfileActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            // Construct the public URL
                            String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + filename;
                            
                            // Save to Firebase Realtime Database
                            mDatabase.child(userId).child("imageUrl").setValue(publicUrl);
                            
                            runOnUiThread(() -> {
                                Toast.makeText(ProfileActivity.this, "Profile uploaded to Supabase!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                            runOnUiThread(() -> {
                                Toast.makeText(ProfileActivity.this, "Error " + response.code() + ": " + errorBody, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            });
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Error processing image", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }).start();
    }
}
