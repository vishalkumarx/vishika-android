package com.example.vishika;

public class Gift {
    private String content;
    private String sender;
    private long timestamp;
    private String type;

    public Gift() {}

    public Gift(String content, String sender, long timestamp, String type) {
        this.content = content;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }
}
