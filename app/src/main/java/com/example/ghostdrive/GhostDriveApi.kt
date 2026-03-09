package com.example.ghostdrive

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface GhostDriveApi {

    @GET("api/files")
    suspend fun listFiles(@Query("path") path: String): List<FileItem>

    @GET("api/search")
    suspend fun searchFiles(@Query("query") query: String): List<FileItem>

    @GET("api/details")
    suspend fun fileDetails(@Query("path") path: String): FileDetails

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("path") destinationPath: RequestBody
    ): UploadResponse

    // Thumbnail is fetched by Coil directly using buildThumbnailUrl() — not via Retrofit
}