package com.example.vishika;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class NewPostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_new_post);

        ImageView ivPostImage = findViewById(R.id.ivPostImage);
        EditText etCaption = findViewById(R.id.etCaption);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnPost = findViewById(R.id.btnPost);

        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        if (imageUriString == null) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri imageUri = Uri.parse(imageUriString);

        Glide.with(this)
                .load(imageUri)
                .into(ivPostImage);



        btnBack.setOnClickListener(v -> finish());

        btnPost.setOnClickListener(v -> {
            String caption = etCaption.getText().toString().trim();
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("CAPTION", caption);

            resultIntent.putExtra("IMAGE_URI", imageUriString);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
