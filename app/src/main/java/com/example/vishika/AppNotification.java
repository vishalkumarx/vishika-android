package com.example.vishika;

public class AppNotification {
    public String id;
    public String type; // "like_post", "like_comment", "comment", "reply"
    public String senderId;
    public String senderName;
    public String text;
    public long timestamp;
    public String postId;

    public AppNotification() {
        // Default constructor for Firebase
    }

    public AppNotification(String id, String type, String senderId, String senderName, String text, long timestamp, String postId) {
        this.id = id;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
        this.postId = postId;
    }
}
