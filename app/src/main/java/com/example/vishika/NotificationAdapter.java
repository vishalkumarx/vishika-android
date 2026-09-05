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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;
    private final List<AppNotification> notifications;

    public NotificationAdapter(Context context, List<AppNotification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);

        String styledText = "<b>" + notification.senderName + "</b> " + notification.text;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvNotificationText.setText(android.text.Html.fromHtml(styledText, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvNotificationText.setText(android.text.Html.fromHtml(styledText));
        }

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS);
        holder.tvNotificationTimestamp.setText(timeAgo);

        // Load avatar
        FirebaseDatabase.getInstance().getReference()
                .child("profiles")
                .child(notification.senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileUrl = snapshot.child("imageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(holder.ivNotificationAvatar);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
                
        holder.itemView.setOnClickListener(v -> {
            if (notification.postId != null && !notification.postId.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(context, FeedActivity.class);
                intent.putExtra("SCROLL_TO_POST", notification.postId);
                if (notification.type != null && (notification.type.equals("comment") || notification.type.equals("reply") || notification.type.equals("like_comment"))) {
                    intent.putExtra("OPEN_COMMENTS", true);
                }
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivNotificationAvatar;
        TextView tvNotificationText;
        TextView tvNotificationTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivNotificationAvatar = itemView.findViewById(R.id.ivNotificationAvatar);
            tvNotificationText = itemView.findViewById(R.id.tvNotificationText);
            tvNotificationTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
        }
    }
}
