package com.example.vishika;

public class ChatMessage {
    public String id;
    public String senderId;
    public String text;
    public long timestamp;
    public String replyToText;
    public String replyToSender;
    public String imageUrl;
    public String videoUrl;
    public boolean isLiked;
    public long storyTimestamp;

    public ChatMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(ChatMessage.class)
    }

    public ChatMessage(String id, String senderId, String text, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }
    
    public ChatMessage(String id, String senderId, String text, long timestamp, String replyToText, String replyToSender) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.replyToText = replyToText;
        this.replyToSender = replyToSender;
    }
}
