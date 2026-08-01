package com.ghostdrive.server.dto;

public class FileInfo {
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;

    public FileInfo(String name, String path, boolean isDirectory, long size) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = size;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public boolean isDirectory() { return isDirectory; }
    public long getSize() { return size; }
}