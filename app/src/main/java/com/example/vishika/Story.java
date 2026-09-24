package com.example.vishika;

import java.io.Serializable;

public class Story implements Serializable {
    private String id;
    private String url;
    private String type;
    private long timestamp;

    public Story() {}

    public Story(String id, String url, String type, long timestamp) {
        this.id = id;
        this.url = url;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
