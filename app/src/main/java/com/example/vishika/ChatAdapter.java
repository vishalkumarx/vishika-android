package com.example.vishika;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final Context context;
    private final List<ChatMessage> messages;
    private final String currentUserId;
    private long partnerLastSeen = 0;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private OnReplySwipeListener replySwipeListener;
    private OnReplyClickListener replyClickListener;
    private String highlightedMessageId = null;

    public interface OnReplySwipeListener {
        void onReplySwiped(ChatMessage message);
    }
    
    public interface OnReplyClickListener {
        void onReplyClicked(String replyText);
    }

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    public void setHighlightedMessage(String messageId) {
        this.highlightedMessageId = messageId;
        notifyDataSetChanged();
        
        // Remove highlight after 2 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (messageId.equals(this.highlightedMessageId)) {
                this.highlightedMessageId = null;
                notifyDataSetChanged();
            }
        }, 2000);
    }

    public void setPartnerLastSeen(long timestamp) {
        this.partnerLastSeen = timestamp;
        notifyDataSetChanged();
    }

    public void setOnReplySwipeListener(OnReplySwipeListener listener) {
        this.replySwipeListener = listener;
    }
    
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public ChatMessage getMessageAt(int position) {
        if (position >= 0 && position < messages.size()) {
            return messages.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean isMine = message.senderId != null && message.senderId.equals(currentUserId);
        
        String currentDateStr = getDateBannerString(message.timestamp);
        boolean showDateBanner = false;
        
        if (position == 0) {
            showDateBanner = true;
        } else {
            String previousDateStr = getDateBannerString(messages.get(position - 1).timestamp);
            if (!currentDateStr.equals(previousDateStr)) {
                showDateBanner = true;
            }
        }
        
        if (showDateBanner) {
            holder.tvDateBanner.setVisibility(View.VISIBLE);
            holder.tvDateBanner.setText(currentDateStr);
            holder.tvDateBanner.setTag("visible");
        } else {
            holder.tvDateBanner.setVisibility(View.GONE);
            holder.tvDateBanner.setTag("gone");
        }
        
        
        String timeStr = timeFormat.format(new Date(message.timestamp));

        boolean emojisOnly = isOnlyEmojis(message.text);

        boolean isHighlighted = message.id != null && message.id.equals(highlightedMessageId);
        
        if (emojisOnly) {
            holder.tvOutgoingText.setTextSize(48f);
            holder.layoutOutgoingBubble.setBackground(null);
            holder.tvIncomingText.setTextSize(48f);
            holder.layoutIncomingBubble.setBackground(null);
            holder.tvOutgoingText.setPadding(0, 0, 0, 0);
            holder.tvIncomingText.setPadding(0, 0, 0, 0);
        } else {
            holder.tvOutgoingText.setTextSize(16f);
            holder.layoutOutgoingBubble.setBackgroundResource(R.drawable.bg_bottom_nav);
            holder.layoutOutgoingBubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(isHighlighted ? "#FF8DA5" : "#FF5C81")));
            
            holder.tvIncomingText.setTextSize(16f);
            holder.layoutIncomingBubble.setBackgroundResource(R.drawable.bg_bottom_nav);
            holder.layoutIncomingBubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(isHighlighted ? "#D0D0D0" : "#F0F0F0")));
            
            int paddingH = (int) (16 * context.getResources().getDisplayMetrics().density);
            int paddingV = (int) (10 * context.getResources().getDisplayMetrics().density);
            holder.tvOutgoingText.setPadding(paddingH, paddingV, paddingH, paddingV);
            holder.tvIncomingText.setPadding(paddingH, paddingV, paddingH, paddingV);
        }

        if (isMine) {
            holder.layoutOutgoing.setVisibility(View.VISIBLE);
            holder.layoutIncoming.setVisibility(View.GONE);
            
            boolean hasReply = message.replyToText != null && !message.replyToText.isEmpty();
            if (hasReply) {
                holder.layoutReplyOutgoing.setVisibility(View.VISIBLE);
                holder.tvReplySenderOutgoing.setText(message.replyToSender);
                holder.tvReplyTextOutgoing.setText(message.replyToText);
                
                holder.layoutReplyOutgoing.setOnClickListener(v -> {
                    if (replyClickListener != null) {
                        replyClickListener.onReplyClicked(message.replyToText);
                    }
                });
            } else {
                holder.layoutReplyOutgoing.setVisibility(View.GONE);
                holder.layoutReplyOutgoing.setOnClickListener(null);
            }

            boolean hasText = message.text != null && !message.text.trim().isEmpty();
            if (hasText) {
                holder.tvOutgoingText.setVisibility(View.VISIBLE);
                holder.tvOutgoingText.setText(message.text);
            } else {
                holder.tvOutgoingText.setVisibility(View.GONE);
            }
            
            if (hasText || hasReply) {
                holder.layoutOutgoingBubble.setVisibility(View.VISIBLE);
            } else {
                holder.layoutOutgoingBubble.setVisibility(View.GONE);
            }
            
            if (message.imageUrl != null && !message.imageUrl.isEmpty()) {
                holder.ivOutgoingImage.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(context)
                        .load(message.imageUrl)
                        .into(holder.ivOutgoingImage);
                holder.ivOutgoingImage.setOnClickListener(v -> showFullScreenImage(message.imageUrl));
            } else {
                holder.ivOutgoingImage.setVisibility(View.GONE);
                holder.ivOutgoingImage.setOnClickListener(null);
            }
            holder.tvOutgoingTime.setText(timeStr);
            
            if (position == messages.size() - 1 && message.timestamp <= partnerLastSeen) {
                holder.tvSeenStatus.setVisibility(View.VISIBLE);
                long now = System.currentTimeMillis();
                if (now - partnerLastSeen < 60000) {
                    holder.tvSeenStatus.setText("Seen just now");
                } else {
                    CharSequence timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
                            partnerLastSeen, now, android.text.format.DateUtils.MINUTE_IN_MILLIS);
                    holder.tvSeenStatus.setText("Seen " + timeAgo.toString().toLowerCase());
                }
            } else {
                holder.tvSeenStatus.setVisibility(View.GONE);
            }
        } else {
            holder.layoutIncoming.setVisibility(View.VISIBLE);
            holder.layoutOutgoing.setVisibility(View.GONE);
            
            boolean hasReply = message.replyToText != null && !message.replyToText.isEmpty();
            if (hasReply) {
                holder.layoutReplyIncoming.setVisibility(View.VISIBLE);
                holder.tvReplySenderIncoming.setText(message.replyToSender);
                holder.tvReplyTextIncoming.setText(message.replyToText);
                
                holder.layoutReplyIncoming.setOnClickListener(v -> {
                    if (replyClickListener != null) {
                        replyClickListener.onReplyClicked(message.replyToText);
                    }
                });
            } else {
                holder.layoutReplyIncoming.setVisibility(View.GONE);
                holder.layoutReplyIncoming.setOnClickListener(null);
            }

            boolean hasText = message.text != null && !message.text.trim().isEmpty();
            if (hasText) {
                holder.tvIncomingText.setVisibility(View.VISIBLE);
                holder.tvIncomingText.setText(message.text);
            } else {
                holder.tvIncomingText.setVisibility(View.GONE);
            }
            
            if (hasText || hasReply) {
                holder.layoutIncomingBubble.setVisibility(View.VISIBLE);
            } else {
                holder.layoutIncomingBubble.setVisibility(View.GONE);
            }
            
            if (message.imageUrl != null && !message.imageUrl.isEmpty()) {
                holder.ivIncomingImage.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(context)
                        .load(message.imageUrl)
                        .into(holder.ivIncomingImage);
                holder.ivIncomingImage.setOnClickListener(v -> showFullScreenImage(message.imageUrl));
            } else {
                holder.ivIncomingImage.setVisibility(View.GONE);
                holder.ivIncomingImage.setOnClickListener(null);
            }
            holder.tvIncomingTime.setText(timeStr);
        }
    }
    
    private void showFullScreenImage(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        android.widget.ImageView imageView = new android.widget.ImageView(context);
        imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        com.bumptech.glide.Glide.with(context).load(imageUrl).into(imageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(imageView);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private boolean isOnlyEmojis(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        boolean hasEmoji = false;
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int type = Character.getType(codePoint);
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                hasEmoji = true;
            } else if (!Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '\u200d') {
                return false;
            }
            if (Character.isHighSurrogate(text.charAt(i))) {
                i++; // skip low surrogate
            }
        }
        return hasEmoji && text.length() <= 15;
    }

    public String getDateBannerString(long timestamp) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        java.util.Calendar msgCal = java.util.Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        if (cal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "Today";
        }
        
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
        if (cal.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        }
        
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1); // Reset to today
        
        long diff = cal.getTimeInMillis() - timestamp;
        long days = diff / (24 * 60 * 60 * 1000);
        if (days < 7) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(msgCal.getTime());
        }
        
        return new SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(msgCal.getTime());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layoutOutgoing;
        public LinearLayout layoutOutgoingBubble;
        public TextView tvOutgoingText;
        public TextView tvOutgoingTime;
        public TextView tvSeenStatus;
        public LinearLayout layoutReplyOutgoing;
        public TextView tvReplySenderOutgoing;
        public TextView tvReplyTextOutgoing;
        public ImageView ivOutgoingImage;

        public LinearLayout layoutIncoming;
        public LinearLayout layoutIncomingBubble;
        public TextView tvIncomingText;
        public TextView tvIncomingTime;
        public LinearLayout layoutReplyIncoming;
        public TextView tvReplySenderIncoming;
        public TextView tvReplyTextIncoming;
        public ImageView ivIncomingImage;

        public TextView tvDateBanner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutOutgoing = itemView.findViewById(R.id.layoutOutgoing);
            layoutOutgoingBubble = itemView.findViewById(R.id.layoutOutgoingBubble);
            tvOutgoingText = itemView.findViewById(R.id.tvOutgoingText);
            tvOutgoingTime = itemView.findViewById(R.id.tvOutgoingTime);
            tvSeenStatus = itemView.findViewById(R.id.tvSeenStatus);
            layoutReplyOutgoing = itemView.findViewById(R.id.layoutReplyOutgoing);
            tvReplySenderOutgoing = itemView.findViewById(R.id.tvReplySenderOutgoing);
            tvReplyTextOutgoing = itemView.findViewById(R.id.tvReplyTextOutgoing);
            ivOutgoingImage = itemView.findViewById(R.id.ivOutgoingImage);

            layoutIncoming = itemView.findViewById(R.id.layoutIncoming);
            layoutIncomingBubble = itemView.findViewById(R.id.layoutIncomingBubble);
            tvIncomingText = itemView.findViewById(R.id.tvIncomingText);
            tvIncomingTime = itemView.findViewById(R.id.tvIncomingTime);
            layoutReplyIncoming = itemView.findViewById(R.id.layoutReplyIncoming);
            tvReplySenderIncoming = itemView.findViewById(R.id.tvReplySenderIncoming);
            tvReplyTextIncoming = itemView.findViewById(R.id.tvReplyTextIncoming);
            ivIncomingImage = itemView.findViewById(R.id.ivIncomingImage);

            tvDateBanner = itemView.findViewById(R.id.tvDateBanner);
        }
    }
}
