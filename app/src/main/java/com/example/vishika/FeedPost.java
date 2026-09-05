package com.example.vishika;

public class FeedPost {
    public String id;
    public String authorId;
    public String authorName;
    public String imageUrl;
    public String caption;
    public String location;
    public long timestamp;
    public java.util.Map<String, Boolean> likes;
    public java.util.Map<String, FeedComment> comments;

    public FeedPost() {
        // Default constructor required for calls to DataSnapshot.getValue(FeedPost.class)
    }

    public FeedPost(String id, String authorId, String authorName, String imageUrl, String caption, String location, long timestamp) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.location = location;
        this.timestamp = timestamp;
        this.likes = new java.util.HashMap<>();
        this.comments = new java.util.HashMap<>();
    }
}
