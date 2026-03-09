package com.example.ghostdrive

// File shown in the explorer list
data class FileItem(
    val name: String,
    val type: String,   // "file" or "directory"
    val size: Long,
    val path: String
)

// Rich metadata from /api/details
data class FileDetails(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val createdAt: Long,
    val modifiedAt: Long,
    val mimeType: String?
)

// Response from /api/upload
data class UploadResponse(
    val status: String,
    val saved_as: String,
    val path: String
)
