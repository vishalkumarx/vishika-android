package com.example.vishika;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private final Context context;
    private final List<FeedPost> posts;
    private final String currentUserId;
    private final DatabaseReference postsRef;
    private final OnCommentClickListener commentClickListener;
    
    public interface OnCommentClickListener {
        void onCommentClick(FeedPost post);
    }

    public FeedAdapter(Context context, List<FeedPost> posts, String currentUserId, OnCommentClickListener listener) {
        this.context = context;
        this.posts = posts;
        this.currentUserId = currentUserId;
        this.postsRef = FirebaseDatabase.getInstance().getReference().child("feed_posts");
        this.commentClickListener = listener;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feed_post, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedPost post = posts.get(position);

        holder.tvAuthorName.setText(post.authorName);
        

        
        // Formatted caption with bold username (Instagram style)
        String formattedCaption = post.caption != null ? post.caption.replace("\n", "<br>") : "";
        String captionText = "<b>" + post.authorName + "</b> ";
        if ("Culture Exchange".equals(post.location)) {
            captionText += "<font color=\"#888888\">[Culture Exchange]</font> ";
        }
        captionText += formattedCaption;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvCaption.setText(android.text.Html.fromHtml(captionText, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvCaption.setText(android.text.Html.fromHtml(captionText));
        }

        // Timestamp
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                post.timestamp,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS);
        holder.tvTimestamp.setText(timeAgo);

        // Load post image
        if ("youtube".equals(post.mediaType) && post.imageUrl != null) {
            holder.ivPostImage.setVisibility(View.GONE);
            holder.wvPostVideo.setVisibility(View.VISIBLE);
            holder.wvPostVideo.getSettings().setJavaScriptEnabled(true);
            holder.wvPostVideo.getSettings().setDomStorageEnabled(true);
            holder.wvPostVideo.getSettings().setMediaPlaybackRequiresUserGesture(false);
            holder.wvPostVideo.setWebChromeClient(new android.webkit.WebChromeClient());
            String videoId = extractYoutubeId(post.imageUrl);
            if (videoId != null) {
                String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body style='margin:0;padding:0;'><iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/" + videoId + "?autoplay=0&controls=1&rel=0&origin=https://www.youtube.com\" frameborder=\"0\" allow=\"fullscreen\"></iframe></body></html>";
                holder.wvPostVideo.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null);
            }
        } else {
            holder.wvPostVideo.setVisibility(View.GONE);
            holder.ivPostImage.setVisibility(View.VISIBLE);
            if (post.imageUrl != null) {
                Glide.with(context)
                        .load(post.imageUrl)
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .into(holder.ivPostImage);
            }
        }

        // Load author profile picture
        FirebaseDatabase.getInstance().getReference()
                .child("profiles")
                .child(post.authorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(holder.ivAuthorAvatar);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Like button logic
        boolean isLikedByMe = post.likes != null && post.likes.containsKey(currentUserId) && post.likes.get(currentUserId);
        holder.ivLike.setImageResource(isLikedByMe ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        if (isLikedByMe) {
            holder.ivLike.setColorFilter(android.graphics.Color.parseColor("#FF5C81"));
        } else {
            holder.ivLike.setColorFilter(android.graphics.Color.parseColor("#000000"));
        }

        int likeCount = post.likes != null ? post.likes.size() : 0;
        if (likeCount > 0) {
            String partnerId = currentUserId.equals("vishal") ? "rashika" : "vishal";
            String partnerName = partnerId.substring(0, 1).toUpperCase() + partnerId.substring(1).toLowerCase();
            
            boolean isLikedByPartner = post.likes.containsKey(partnerId);
            
            if (isLikedByMe && isLikedByPartner) {
                holder.tvLikesCount.setText("You and " + partnerName + " liked this post.");
            } else if (isLikedByMe) {
                holder.tvLikesCount.setText("You liked this post.");
            } else if (isLikedByPartner) {
                holder.tvLikesCount.setText(partnerName + " liked this post.");
            } else {
                holder.tvLikesCount.setText("Someone liked this post.");
            }
            holder.tvLikesCount.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.tvLikesCount.setVisibility(android.view.View.GONE);
        }
        holder.ivLike.setOnClickListener(v -> {
            boolean currentlyLiked = post.likes != null && post.likes.containsKey(currentUserId) && post.likes.get(currentUserId);
            if (currentlyLiked) {
                postsRef.child(post.id).child("likes").child(currentUserId).removeValue();
            } else {
                postsRef.child(post.id).child("likes").child(currentUserId).setValue(true);
                
                if (!post.authorId.equals(currentUserId)) {
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                        .child("notifications").child(post.authorId);
                    String notifId = notifRef.push().getKey();
                    if (notifId != null) {
                        AppNotification notification = new AppNotification(notifId, "like_post", currentUserId, "", "liked your post.", System.currentTimeMillis(), post.id);
                        FirebaseDatabase.getInstance().getReference().child("profiles").child(currentUserId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    if (name != null && !name.trim().isEmpty()) {
                                        notification.senderName = name;
                                    } else {
                                        notification.senderName = currentUserId.substring(0, 1).toUpperCase() + currentUserId.substring(1);
                                    }
                                    notifRef.child(notifId).setValue(notification);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                    }
                }
            }
        });
        int commentCount = 0;
        if (post.comments != null) {
            commentCount += post.comments.size();
            for (FeedComment comment : post.comments.values()) {
                if (comment.replies != null) {
                    commentCount += comment.replies.size();
                }
            }
        }
        
        if (commentCount > 0) {
            holder.tvCommentCount.setText(String.valueOf(commentCount));
            holder.tvCommentCount.setVisibility(View.VISIBLE);
            holder.tvViewComments.setVisibility(View.VISIBLE);
            holder.tvViewComments.setText("View all " + commentCount + (commentCount == 1 ? " comment" : " comments"));
        } else {
            holder.tvCommentCount.setVisibility(View.GONE);
            holder.tvViewComments.setVisibility(View.GONE);
        }

        View.OnClickListener openComments = v -> {
            if (commentClickListener != null) {
                commentClickListener.onCommentClick(post);
            }
        };

        holder.ivComment.setOnClickListener(openComments);
        holder.tvViewComments.setOnClickListener(openComments);
        
        // Double Tap to Like
        holder.ivPostImage.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - holder.lastClickTime < 300) { // Double tap detected
                // Save like
                if (!isLikedByMe) {
                    // Locally update so repeated taps don't fire multiple times during delay
                    if (post.likes == null) post.likes = new java.util.HashMap<>();
                    post.likes.put(currentUserId, true);
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        postsRef.child(post.id).child("likes").child(currentUserId).setValue(true);
                        
                        if (!post.authorId.equals(currentUserId)) {
                            com.google.firebase.database.DatabaseReference notifRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                                .child("notifications").child(post.authorId);
                            String notifId = notifRef.push().getKey();
                            if (notifId != null) {
                                com.example.vishika.AppNotification notification = new com.example.vishika.AppNotification(notifId, "like_post", currentUserId, "", "liked your post.", System.currentTimeMillis(), post.id);
                                
                                com.google.firebase.database.FirebaseDatabase.getInstance().getReference().child("profiles").child(currentUserId)
                                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                        @Override
                                        public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                            String name = snapshot.child("name").getValue(String.class);
                                            if (name != null && !name.trim().isEmpty()) {
                                                notification.senderName = name;
                                            } else {
                                                notification.senderName = currentUserId.substring(0, 1).toUpperCase() + currentUserId.substring(1);
                                            }
                                            notifRef.child(notifId).setValue(notification);
                                        }
                                        @Override
                                        public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
                                    });
                            }
                        }
                    }, 600);
                }
                
                // Show Animation
                holder.ivHeartAnim.setAlpha(1f);
                holder.ivHeartAnim.setScaleX(0.5f);
                holder.ivHeartAnim.setScaleY(0.5f);
                holder.ivHeartAnim.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .withEndAction(() -> {
                        holder.ivHeartAnim.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(0f)
                            .setDuration(200)
                            .setStartDelay(150)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .start();
                    })
                    .start();
            }
            holder.lastClickTime = clickTime;
        });
        // Three dot menu
        if (post.authorId.equals(currentUserId)) {
            holder.ivMore.setVisibility(View.VISIBLE);
            holder.ivMore.setOnClickListener(view -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(context, holder.ivMore);
                popup.getMenu().add("Delete Post");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Delete Post")) {
                        showDeleteConfirmationDialog(post);
                    }
                    return true;
                });
                popup.show();
            });
        } else {
            holder.ivMore.setVisibility(View.GONE);
        }
    }
    
    private void showDeleteConfirmationDialog(FeedPost post) {
        android.view.View dialogView = android.view.LayoutInflater.from(context).inflate(R.layout.layout_dialog_confirm_delete, null);
        
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancelDelete);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDelete);
        
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
                
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            postsRef.child(post.id).removeValue();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private String extractYoutubeId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) return matcher.group();
        return null;
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAuthorAvatar, ivPostImage, ivLike, ivComment, ivHeartAnim, ivMore;
        android.webkit.WebView wvPostVideo;
        TextView tvAuthorName, tvLikesCount, tvCaption, tvTimestamp, tvViewComments, tvCommentCount;
        long lastClickTime = 0;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAuthorAvatar = itemView.findViewById(R.id.ivAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);

            ivMore = itemView.findViewById(R.id.ivMore);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            wvPostVideo = itemView.findViewById(R.id.wvPostVideo);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivComment = itemView.findViewById(R.id.ivComment);
            ivHeartAnim = itemView.findViewById(R.id.ivHeartAnim);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvViewComments = itemView.findViewById(R.id.tvViewComments);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        }
    }
}
