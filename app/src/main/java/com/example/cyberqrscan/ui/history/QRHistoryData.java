package com.example.cyberqrscan.ui.history;

public class QRHistoryData {
    private String type, data;
    private long creationTime;

    public QRHistoryData(String type, String data, long creationTime) {
        this.type = type;
        this.data = data;
        this.creationTime = creationTime;
    }

    public String getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
