package com.example.vishika;

import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static boolean isVisible = false;

    private static final String SUPABASE_URL = "https://rftikfmjugzckousuyda.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmdGlrZm1qdWd6Y2tvdXN1eWRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgyNTQ3MTAsImV4cCI6MjEwMzgzMDcxMH0.esfpZIkGdf9CG-F_KpeYn3bx8xTqxOCoHJ-lbE0kk9Q";

    private ProgressBar pbUpload;
    private ImageView btnAttach;
    private ImageView btnSend;

    private EditText etMessage;

    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        String caption = etMessage != null ? etMessage.getText().toString().trim() : "";
                        uploadImageToSupabase(selectedImageUri, caption);
                    }
                }
            });

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private DatabaseReference chatsRef;
    
    private String currentUserId;
    private String partnerId;
    
    // Pagination state
    private String oldestMessageKey = null;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;
    private com.google.firebase.database.ChildEventListener newMessagesListener;
    private com.google.firebase.database.Query newMessagesQuery;
    private static final int PAGE_SIZE = 30;
    
    private ChatMessage replyingToMessage = null;
    private View layoutReplyPreview;
    private TextView tvReplyPreviewSender;
    private TextView tvReplyPreviewText;
    private ImageView btnCancelReply;

    private long partnerLastSeen = 0; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = MainActivity.IS_VISHAL ? "vishal" : "rashika";
        partnerId = MainActivity.IS_VISHAL ? "rashika" : "vishal";

        chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView ivPartnerAvatar = findViewById(R.id.ivPartnerAvatar);
        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        pbUpload = findViewById(R.id.pbUpload);
        btnAttach = findViewById(R.id.btnAttach);
        
        btnAttach.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(android.content.Intent.createChooser(intent, "Select Image for Chat"));
        });

        recyclerView = findViewById(R.id.recyclerViewChat);
        
        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplyPreviewSender = findViewById(R.id.tvReplyPreviewSender);
        tvReplyPreviewText = findViewById(R.id.tvReplyPreviewText);
        btnCancelReply = findViewById(R.id.btnCancelReply);

        btnCancelReply.setOnClickListener(v -> cancelReply());

        btnBack.setOnClickListener(v -> finish());
        
        tvPartnerName.setText(MainActivity.IS_VISHAL ? "Rashika" : "Vishal");

        // Load partner avatar
        FirebaseDatabase.getInstance().getReference().child("profiles").child(partnerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty() && !isDestroyed()) {
                            Glide.with(ChatActivity.this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(ivPartnerAvatar);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        
        messages = new ArrayList<>();
        adapter = new ChatAdapter(this, messages, currentUserId);
        
        adapter.setOnReplySwipeListener(message -> {
            replyingToMessage = message;
            layoutReplyPreview.setVisibility(View.VISIBLE);
            tvReplyPreviewSender.setText(message.senderId.equals(currentUserId) ? "You" : tvPartnerName.getText().toString());
            tvReplyPreviewText.setText(message.text);
            etMessage.requestFocus();
            
            // Show keyboard
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
        
        adapter.setOnReplyClickListener(replyText -> {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (replyText.equals(messages.get(i).text)) {
                    recyclerView.smoothScrollToPosition(i);
                    // Add a small delay so it highlights after the scroll starts
                    final String msgId = messages.get(i).id;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        adapter.setHighlightedMessage(msgId);
                    }, 300);
                    break;
                }
            }
        });
        
        recyclerView.setAdapter(adapter);
        
        ItemTouchHelper touchHelper = new ItemTouchHelper(new MessageSwipeController(this, message -> {
            replyingToMessage = message;
            layoutReplyPreview.setVisibility(View.VISIBLE);
            tvReplyPreviewSender.setText(message.senderId.equals(currentUserId) ? "You" : tvPartnerName.getText().toString());
            tvReplyPreviewText.setText(message.text);
            etMessage.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }));
        touchHelper.attachToRecyclerView(recyclerView);

        loadMessages();

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etMessage.setText("");
            }
        });

        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                FirebaseDatabase.getInstance().getReference()
                    .child("typing")
                    .child(currentUserId)
                    .setValue(s.length() > 0);
            }
        });

        TextView tvTypingIndicator = findViewById(R.id.tvTypingIndicator);
        String partnerName = MainActivity.IS_VISHAL ? "Rashika" : "Vishal";
        FirebaseDatabase.getInstance().getReference().child("typing").child(partnerId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isTyping = snapshot.getValue(Boolean.class);
                    if (isTyping != null && isTyping) {
                        tvTypingIndicator.setText(partnerName + " is typing...");
                        tvTypingIndicator.setVisibility(View.VISIBLE);
                    } else {
                        tvTypingIndicator.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        TextView tvStickyDateBanner = findViewById(R.id.tvStickyDateBanner);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null || adapter == null || messages.isEmpty()) return;
                
                int firstVisiblePos = lm.findFirstVisibleItemPosition();
                if (firstVisiblePos == RecyclerView.NO_POSITION) {
                    tvStickyDateBanner.setVisibility(View.GONE);
                    return;
                }
                
                // Reset all visible banners
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    ChatAdapter.ViewHolder holder = (ChatAdapter.ViewHolder) recyclerView.getChildViewHolder(child);
                    if ("visible".equals(holder.tvDateBanner.getTag())) {
                        holder.tvDateBanner.setVisibility(View.VISIBLE);
                    }
                }
                
                String currentDate = adapter.getDateBannerString(messages.get(firstVisiblePos).timestamp);
                tvStickyDateBanner.setText(currentDate);
                
                View firstView = lm.findViewByPosition(firstVisiblePos);
                float stickyBaseY = tvStickyDateBanner.getY() - tvStickyDateBanner.getTranslationY();
                
                if (firstView != null) {
                    ChatAdapter.ViewHolder firstHolder = (ChatAdapter.ViewHolder) recyclerView.getChildViewHolder(firstView);
                    float bannerAbsoluteY = recyclerView.getY() + firstView.getY() + firstHolder.tvDateBanner.getY();
                    
                    if ("visible".equals(firstHolder.tvDateBanner.getTag())) {
                        if (bannerAbsoluteY <= stickyBaseY) {
                            firstHolder.tvDateBanner.setVisibility(View.INVISIBLE);
                            tvStickyDateBanner.setVisibility(View.VISIBLE);
                        } else {
                            firstHolder.tvDateBanner.setVisibility(View.VISIBLE);
                            tvStickyDateBanner.setVisibility(View.GONE);
                        }
                    } else {
                        tvStickyDateBanner.setVisibility(View.VISIBLE);
                    }
                }
                
                if (tvStickyDateBanner.getVisibility() == View.VISIBLE) {
                    View secondView = lm.findViewByPosition(firstVisiblePos + 1);
                    if (secondView != null && firstVisiblePos + 1 < messages.size()) {
                        String nextDate = adapter.getDateBannerString(messages.get(firstVisiblePos + 1).timestamp);
                        if (!currentDate.equals(nextDate)) {
                            ChatAdapter.ViewHolder secondHolder = (ChatAdapter.ViewHolder) recyclerView.getChildViewHolder(secondView);
                            float nextBannerAbsoluteY = recyclerView.getY() + secondView.getY() + secondHolder.tvDateBanner.getY();
                            float stickyHeight = tvStickyDateBanner.getHeight();
                            
                            if (nextBannerAbsoluteY < stickyBaseY + stickyHeight) {
                                tvStickyDateBanner.setTranslationY(nextBannerAbsoluteY - (stickyBaseY + stickyHeight));
                            } else {
                                tvStickyDateBanner.setTranslationY(0);
                            }
                        } else {
                            tvStickyDateBanner.setTranslationY(0);
                        }
                    } else {
                        tvStickyDateBanner.setTranslationY(0);
                    }
                } else {
                    tvStickyDateBanner.setTranslationY(0);
                }
                
                // Pagination check
                if (!isLoading && hasMoreMessages && firstVisiblePos < 5) {
                    loadOlderMessages();
                }
            }
        });
    }

    private void loadMessages() {
        chatsRef.orderByKey().limitToLast(PAGE_SIZE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage message = data.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
                        if (oldestMessageKey == null) {
                            oldestMessageKey = data.getKey();
                        }
                    }
                }
                
                if (messages.size() < PAGE_SIZE) {
                    hasMoreMessages = false;
                }
                
                adapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                    recyclerView.post(() -> recyclerView.scrollBy(0, 0));
                    if (isVisible) {
                        FirebaseDatabase.getInstance().getReference().child("chat_seen").child(currentUserId).setValue(messages.get(messages.size() - 1).timestamp);
                    }
                }
                
                String newestMessageKey = messages.isEmpty() ? null : messages.get(messages.size() - 1).id;
                listenForNewMessages(newestMessageKey);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseDatabase.getInstance().getReference().child("chat_seen").child(partnerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long timestamp = snapshot.getValue(Long.class);
                        if (timestamp != null) {
                            adapter.setPartnerLastSeen(timestamp);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForNewMessages(String newestMessageKey) {
        if (newMessagesQuery != null && newMessagesListener != null) {
            newMessagesQuery.removeEventListener(newMessagesListener);
        }
        
        if (newestMessageKey != null) {
            newMessagesQuery = chatsRef.orderByKey().startAfter(newestMessageKey);
        } else {
            newMessagesQuery = chatsRef.orderByKey();
        }
        
        newMessagesListener = new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    messages.add(message);
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.smoothScrollToPosition(messages.size() - 1);
                    if (isVisible) {
                        FirebaseDatabase.getInstance().getReference().child("chat_seen").child(currentUserId).setValue(message.timestamp);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @androidx.annotation.Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        
        newMessagesQuery.addChildEventListener(newMessagesListener);
    }

    private void loadOlderMessages() {
        if (oldestMessageKey == null || !hasMoreMessages) return;
        isLoading = true;

        chatsRef.orderByKey().endBefore(oldestMessageKey).limitToLast(PAGE_SIZE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> olderMessages = new java.util.ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage message = data.getValue(ChatMessage.class);
                    if (message != null) {
                        olderMessages.add(message);
                    }
                }

                if (!olderMessages.isEmpty()) {
                    oldestMessageKey = olderMessages.get(0).id;
                    messages.addAll(0, olderMessages);
                    adapter.notifyItemRangeInserted(0, olderMessages.size());
                }

                if (olderMessages.size() < PAGE_SIZE) {
                    hasMoreMessages = false;
                }

                isLoading = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
                Toast.makeText(ChatActivity.this, "Failed to load older messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelReply() {
        replyingToMessage = null;
        layoutReplyPreview.setVisibility(View.GONE);
    }

    private void sendMessage(String text) {
        sendImageMessage(text, null);
    }
    
    private void sendImageMessage(String text, String imageUrl) {
        String messageId = chatsRef.push().getKey();
        if (messageId != null) {
            ChatMessage msg;
            if (replyingToMessage != null) {
                String replySender = replyingToMessage.senderId.equals(currentUserId) ? "You" : (MainActivity.IS_VISHAL ? "Rashika" : "Vishal");
                msg = new ChatMessage(messageId, currentUserId, text, System.currentTimeMillis(), replyingToMessage.text, replySender);
                cancelReply();
            } else {
                msg = new ChatMessage(messageId, currentUserId, text, System.currentTimeMillis());
            }
            if (imageUrl != null) {
                msg.imageUrl = imageUrl;
            }
            chatsRef.child(messageId).setValue(msg);
            
            // Note: Background push notification will be handled by MoodService on partner's device
        }
    }
    
    private void uploadImageToSupabase(android.net.Uri imageUri, String caption) {
        pbUpload.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        btnAttach.setEnabled(false);
        
        new Thread(() -> {
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                android.graphics.Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();
                
                int maxDim = 1024;
                float scale = Math.min((float)maxDim/originalBitmap.getWidth(), (float)maxDim/originalBitmap.getHeight());
                android.graphics.Bitmap scaledBitmap;
                if (scale < 1.0f) {
                    scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, (int)(originalBitmap.getWidth()*scale), (int)(originalBitmap.getHeight()*scale), true);
                } else {
                    scaledBitmap = originalBitmap;
                }
                
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] bytes = baos.toByteArray();

                String filename = "chat_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/profile_images/" + filename;

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), bytes);

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Content-Type", "image/jpeg")
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/profile_images/" + filename;
                    runOnUiThread(() -> {
                        sendImageMessage(caption, publicUrl);
                        pbUpload.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        btnAttach.setEnabled(true);
                        etMessage.setText("");
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        pbUpload.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        btnAttach.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Upload error", Toast.LENGTH_SHORT).show();
                    pbUpload.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    btnAttach.setEnabled(true);
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        
        android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(1001);
        }
        
        FirebaseDatabase.getInstance().getReference().child("chat_seen").child(currentUserId).setValue(System.currentTimeMillis());
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
        
        if (currentUserId != null) {
            FirebaseDatabase.getInstance().getReference()
                .child("typing")
                .child(currentUserId)
                .setValue(false);
        }
    }
}
