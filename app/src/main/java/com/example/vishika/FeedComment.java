package com.example.vishika;

import java.util.HashMap;
import java.util.Map;

public class FeedComment {
    public String id;
    public String authorId;
    public String authorName;
    public String text;
    public long timestamp;
    public Map<String, FeedComment> replies;
    public Map<String, Boolean> likes;

    public FeedComment() {
    }

    public FeedComment(String id, String authorId, String authorName, String text, long timestamp) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
        this.replies = new HashMap<>();
        this.likes = new HashMap<>();
    }
}
