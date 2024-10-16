package com.example.code_versioning_plugin;

public class FileStatus {
    private String status;
    private String timestamp;
    final private String filenum;

    public String getStatus() {
        return status;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public String getFilenum() {
        return filenum;
    }

    public FileStatus(String status, String timestamp, String filenum) {
        this.status = status;
        this.timestamp = timestamp;
        this.filenum = filenum;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
