package com.example.vishika;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final Context context;
    private final List<FeedComment> comments;
    private final String currentUserId;
    private final DatabaseReference commentsRef;
    private final OnReplyClickListener replyListener;
    private final Set<String> expandedComments = new HashSet<>();

    public interface OnReplyClickListener {
        void onReplyClick(FeedComment comment);
    }

    public CommentsAdapter(Context context, List<FeedComment> comments, String currentUserId, DatabaseReference commentsRef, OnReplyClickListener replyListener) {
        this.context = context;
        this.comments = comments;
        this.currentUserId = currentUserId;
        this.commentsRef = commentsRef;
        this.replyListener = replyListener;
    }

    public void expandComment(String commentId) {
        expandedComments.add(commentId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        FeedComment comment = comments.get(position);

        String formattedText = comment.text != null ? comment.text.replace("\n", "<br>") : "";
        String processedText = formattedText.replaceAll("@(\\w+)", "<font color='#1DA1F2'>@$1</font>");
        String commentText = "<b>" + comment.authorName + "</b><br>" + processedText;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvCommentText.setText(android.text.Html.fromHtml(commentText, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvCommentText.setText(android.text.Html.fromHtml(commentText));
        }

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                comment.timestamp,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS);
        holder.tvCommentTimestamp.setText(timeAgo);

        loadProfilePicture(comment.authorId, holder.ivCommentAvatar);

        holder.tvReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyClick(comment);
            }
        });

        // Delete Logic for Parent Comment
        if (comment.authorId.equals(currentUserId)) {
            holder.tvCommentDelete.setVisibility(View.VISIBLE);
            holder.tvCommentDelete.setOnClickListener(v -> {
                showDeleteDialog(() -> commentsRef.child(comment.id).removeValue());
            });
        } else {
            holder.tvCommentDelete.setVisibility(View.GONE);
        }

        // Like Logic for Parent Comment
        updateLikeUI(comment.likes, holder.ivCommentLike, holder.tvCommentLikeCount);
        holder.ivCommentLike.setOnClickListener(v -> {
            DatabaseReference likeRef = commentsRef.child(comment.id).child("likes").child(currentUserId);
            if (comment.likes != null && comment.likes.containsKey(currentUserId)) {
                likeRef.removeValue();
            } else {
                likeRef.setValue(true);
                
                if (!comment.authorId.equals(currentUserId)) {
                    DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                        .child("notifications").child(comment.authorId);
                    String notifId = notifRef.push().getKey();
                    if (notifId != null) {
                        AppNotification notification = new AppNotification(notifId, "like_comment", currentUserId, "", "liked your comment: " + comment.text, System.currentTimeMillis(), "");
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

        holder.layoutReplies.removeAllViews();
        if (comment.replies != null && !comment.replies.isEmpty()) {
            List<FeedComment> replyList = new ArrayList<>(comment.replies.values());
            Collections.sort(replyList, (c1, c2) -> Long.compare(c1.timestamp, c2.timestamp));
            
            holder.tvViewAllReplies.setVisibility(View.VISIBLE);
            
            if (expandedComments.contains(comment.id)) {
                holder.layoutReplies.setVisibility(View.VISIBLE);
                holder.tvViewAllReplies.setText("Hide replies");
            } else {
                holder.layoutReplies.setVisibility(View.GONE);
                holder.tvViewAllReplies.setText("View all replies (" + replyList.size() + ")");
            }
            
            holder.tvViewAllReplies.setOnClickListener(v -> {
                if (holder.layoutReplies.getVisibility() == View.VISIBLE) {
                    holder.layoutReplies.setVisibility(View.GONE);
                    holder.tvViewAllReplies.setText("View all replies (" + replyList.size() + ")");
                    expandedComments.remove(comment.id);
                } else {
                    holder.layoutReplies.setVisibility(View.VISIBLE);
                    holder.tvViewAllReplies.setText("Hide replies");
                    expandedComments.add(comment.id);
                }
            });
            
            for (FeedComment reply : replyList) {
                View replyView = LayoutInflater.from(context).inflate(R.layout.item_comment, holder.layoutReplies, false);
                
                ImageView ivReplyAvatar = replyView.findViewById(R.id.ivCommentAvatar);
                TextView tvReplyText = replyView.findViewById(R.id.tvCommentText);
                TextView tvReplyTimestamp = replyView.findViewById(R.id.tvCommentTimestamp);
                TextView tvReplyBtn = replyView.findViewById(R.id.tvReply);
                TextView tvReplyDelete = replyView.findViewById(R.id.tvCommentDelete);
                ImageView ivReplyLike = replyView.findViewById(R.id.ivCommentLike);
                TextView tvReplyLikeCount = replyView.findViewById(R.id.tvCommentLikeCount);
                LinearLayout layoutNestedReplies = replyView.findViewById(R.id.layoutReplies);
                
                // Customize reply appearance
                int sizePx = (int) (28 * context.getResources().getDisplayMetrics().density);
                ViewGroup.LayoutParams avatarParams = ivReplyAvatar.getLayoutParams();
                avatarParams.width = sizePx;
                avatarParams.height = sizePx;
                ivReplyAvatar.setLayoutParams(avatarParams);
                
                layoutNestedReplies.setVisibility(View.GONE);
                
                tvReplyBtn.setOnClickListener(v -> {
                    if (replyListener != null) {
                        FeedComment proxyComment = new FeedComment(comment.id, reply.authorId, reply.authorName, reply.text, reply.timestamp);
                        replyListener.onReplyClick(proxyComment);
                    }
                });

                // Delete Logic for Reply
                if (reply.authorId.equals(currentUserId)) {
                    tvReplyDelete.setVisibility(View.VISIBLE);
                    tvReplyDelete.setOnClickListener(v -> {
                        showDeleteDialog(() -> commentsRef.child(comment.id).child("replies").child(reply.id).removeValue());
                    });
                } else {
                    tvReplyDelete.setVisibility(View.GONE);
                }

                // Like Logic for Reply
                updateLikeUI(reply.likes, ivReplyLike, tvReplyLikeCount);
                ivReplyLike.setOnClickListener(v -> {
                    DatabaseReference likeRef = commentsRef.child(comment.id).child("replies").child(reply.id).child("likes").child(currentUserId);
                    if (reply.likes != null && reply.likes.containsKey(currentUserId)) {
                        likeRef.removeValue();
                    } else {
                        likeRef.setValue(true);
                        
                        if (!reply.authorId.equals(currentUserId)) {
                            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference()
                                .child("notifications").child(reply.authorId);
                            String notifId = notifRef.push().getKey();
                            if (notifId != null) {
                                AppNotification notification = new AppNotification(notifId, "like_reply", currentUserId, "", "liked your reply: " + reply.text, System.currentTimeMillis(), "");
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

                String replyFormattedText = reply.text != null ? reply.text.replace("\n", "<br>") : "";
                String replyProcessedText = replyFormattedText.replaceAll("@(\\w+)", "<font color='#1DA1F2'>@$1</font>");
                String replyCombinedText = "<b>" + reply.authorName + "</b><br>" + replyProcessedText;
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    tvReplyText.setText(android.text.Html.fromHtml(replyCombinedText, android.text.Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvReplyText.setText(android.text.Html.fromHtml(replyCombinedText));
                }

                CharSequence rTimeAgo = DateUtils.getRelativeTimeSpanString(
                        reply.timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
                tvReplyTimestamp.setText(rTimeAgo);

                loadProfilePicture(reply.authorId, ivReplyAvatar);

                holder.layoutReplies.addView(replyView);
            }
        } else {
            holder.tvViewAllReplies.setVisibility(View.GONE);
            holder.layoutReplies.setVisibility(View.GONE);
        }
    }

    private void updateLikeUI(java.util.Map<String, Boolean> likes, ImageView ivLike, TextView tvLikeCount) {
        if (likes != null && likes.containsKey(currentUserId)) {
            ivLike.setImageResource(R.drawable.ic_heart_filled);
            ivLike.setColorFilter(android.graphics.Color.parseColor("#FF5C81"));
        } else {
            ivLike.setImageResource(R.drawable.ic_heart); // Or ic_heart_outline if that's the name
            ivLike.setColorFilter(android.graphics.Color.parseColor("#888888"));
        }

        if (likes != null && !likes.isEmpty()) {
            tvLikeCount.setVisibility(View.VISIBLE);
            tvLikeCount.setText(String.valueOf(likes.size()));
        } else {
            tvLikeCount.setVisibility(View.GONE);
        }
    }

    private void loadProfilePicture(String authorId, ImageView imageView) {
        FirebaseDatabase.getInstance().getReference()
                .child("profiles")
                .child(authorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        String profileUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(context)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(imageView);
                        }
                    }
                    @Override
                    public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {}
                });
    }

    private void showDeleteDialog(Runnable onDelete) {
        android.view.View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_dialog_confirm_delete, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tvDeleteTitle);
        TextView tvSubtitle = dialogView.findViewById(R.id.tvDeleteSubtitle);
        if (tvTitle != null) tvTitle.setText("Delete Comment?");
        if (tvSubtitle != null) tvSubtitle.setText("Are you sure you want to delete this comment? This action cannot be undone.");
        
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancelDelete);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirmDelete);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
                
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            onDelete.run();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentAvatar, ivCommentLike;
        TextView tvCommentText, tvCommentTimestamp, tvReply, tvCommentDelete, tvCommentLikeCount, tvViewAllReplies;
        LinearLayout layoutReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCommentAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
            tvCommentTimestamp = itemView.findViewById(R.id.tvCommentTimestamp);
            tvReply = itemView.findViewById(R.id.tvReply);
            tvCommentDelete = itemView.findViewById(R.id.tvCommentDelete);
            ivCommentLike = itemView.findViewById(R.id.ivCommentLike);
            tvCommentLikeCount = itemView.findViewById(R.id.tvCommentLikeCount);
            tvViewAllReplies = itemView.findViewById(R.id.tvViewAllReplies);
            layoutReplies = itemView.findViewById(R.id.layoutReplies);
        }
    }
}
