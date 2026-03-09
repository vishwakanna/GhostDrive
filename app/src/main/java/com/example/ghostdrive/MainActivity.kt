package com.example.ghostdrive

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Explorer                        : Screen()
    object Search                          : Screen()
    object ThemePicker                     : Screen()
    data class Video(val file: FileItem)   : Screen()
    data class Image(val file: FileItem)   : Screen()
    data class Details(val file: FileItem) : Screen()
}

data class AppTheme(val name: String, val bg: Color, val fg: Color, val card: Color, val accent: Color)

val themes = listOf(
    AppTheme("Dark",   Color(0xFF121212), Color(0xFFEEEEEE), Color(0xFF1E1E1E), Color(0xFF6C63FF)),
    AppTheme("Light",  Color(0xFFF8F8F8), Color(0xFF111111), Color(0xFFFFFFFF), Color(0xFF6C63FF)),
    AppTheme("Ocean",  Color(0xFF0A1628), Color(0xFFE0F0FF), Color(0xFF0D2040), Color(0xFF00BFFF)),
    AppTheme("Forest", Color(0xFF0D1F0D), Color(0xFFD4EDDA), Color(0xFF162916), Color(0xFF4CAF50)),
    AppTheme("Amoled", Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF0A0A0A), Color(0xFFFF4444)),
    AppTheme("Pink",   Color(0xFF1A0A12), Color(0xFFFFE4F0), Color(0xFF2A0F1F), Color(0xFFFF69B4)),
    AppTheme("Sunset", Color(0xFF1A0A00), Color(0xFFFFE5CC), Color(0xFF2A1200), Color(0xFFFF6B00)),
    AppTheme("Slate",  Color(0xFF1C1F26), Color(0xFFCDD5E0), Color(0xFF252A35), Color(0xFF748AFF)),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { GhostDriveApp() }
    }
}

fun hideSystemBars(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun showSystemBars(activity: Activity) {
    WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        .show(WindowInsetsCompat.Type.systemBars())
}

fun buildStreamUrl(serverIp: String, filePath: String) =
    "http://$serverIp:8080/api/stream?path=${Uri.encode(filePath)}"

fun buildThumbnailUrl(serverIp: String, filePath: String) =
    "http://$serverIp:8080/api/thumbnail?path=${Uri.encode(filePath)}"

fun buildDownloadUrl(serverIp: String, filePath: String) =
    "http://$serverIp:8080/api/download?path=${Uri.encode(filePath, "/")}"

fun downloadFile(context: Context, file: FileItem, serverIp: String) {
    val url       = buildDownloadUrl(serverIp, file.path)
    val notifId   = file.name.hashCode()
    val channelId = "ghostdrive_downloads"
    val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager

    val channel = android.app.NotificationChannel(
        channelId, "GhostDrive Downloads",
        android.app.NotificationManager.IMPORTANCE_LOW
    ).apply { description = "File download progress" }
    manager.createNotificationChannel(channel)

    fun buildNotif(
        title: String, text: String,
        progress: Int = 0, maxProgress: Int = 100, indeterminate: Boolean = true
    ) = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(maxProgress, progress, indeterminate)
        .build()

    manager.notify(notifId, buildNotif("Downloading...", file.name))
    android.widget.Toast.makeText(context, "Starting: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()

    @Suppress("OPT_IN_USAGE")
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val response = client.newCall(
                okhttp3.Request.Builder().url(url).get().build()
            ).execute()

            if (!response.isSuccessful) {
                manager.cancel(notifId)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed: HTTP ${response.code}", android.widget.Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val body        = response.body ?: throw Exception("Empty response")
            val totalBytes  = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            val inputStream = body.byteStream()
            var bytesWritten = 0L
            var lastNotifAt  = 0L

            val updateEveryBytes = when {
                totalBytes <= 0       -> 1048576L
                totalBytes < 10485760 -> totalBytes / 10
                else                  -> (totalBytes / 20).coerceIn(1048576L, 52428800L)
            }

            fun writeWithProgress(outputStream: java.io.OutputStream) {
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesWritten += bytesRead
                    if (bytesWritten - lastNotifAt >= updateEveryBytes) {
                        lastNotifAt = bytesWritten
                        val progressText = if (totalBytes > 0)
                            "${formatSize(bytesWritten)} / ${formatSize(totalBytes)}"
                        else "${formatSize(bytesWritten)} downloaded"
                        val pct = if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0
                        manager.notify(notifId, buildNotif(
                            file.name, progressText,
                            progress      = pct,
                            indeterminate = totalBytes <= 0
                        ))
                    }
                }
                outputStream.flush()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, getMimeType(file.name))
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: throw Exception("Could not create file in Downloads")
                context.contentResolver.openOutputStream(uri)!!.use { writeWithProgress(it) }
                values.clear()
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                val destDir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = java.io.File(destDir, file.name)
                java.io.FileOutputStream(destFile).use { writeWithProgress(it) }
            }

            val doneNotif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete")
                .setContentText(file.name)
                .setAutoCancel(true)
                .build()
            manager.notify(notifId, doneNotif)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Saved: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            manager.cancel(notifId)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun getMimeType(fileName: String): String = when {
    fileName.endsWith(".pdf",  true) -> "application/pdf"
    fileName.endsWith(".mp4",  true) -> "video/mp4"
    fileName.endsWith(".mkv",  true) -> "video/x-matroska"
    fileName.endsWith(".mp3",  true) -> "audio/mpeg"
    fileName.endsWith(".jpg",  true) ||
            fileName.endsWith(".jpeg", true) -> "image/jpeg"
    fileName.endsWith(".png",  true) -> "image/png"
    fileName.endsWith(".zip",  true) -> "application/zip"
    fileName.endsWith(".txt",  true) -> "text/plain"
    else                             -> "application/octet-stream"
}

fun isVideo(name: String) = name.endsWith(".mp4", true) ||
        name.endsWith(".mkv", true) || name.endsWith(".avi", true)

fun isImage(name: String) = name.endsWith(".png", true) ||
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true)

fun formatSize(bytes: Long): String = when {
    bytes < 1024               -> "$bytes B"
    bytes < 1024 * 1024        -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    else                       -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
}

fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(epochMs))

fun formatDuration(ms: Long): String {
    val s = ms / 1000; val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
fun GhostDriveApp() {
    val context  = LocalContext.current
    val activity = context as Activity
    val rootPath = "/home/vishwa"

    var currentPath by remember { mutableStateOf(rootPath) }
    var serverIp    by remember { mutableStateOf<String?>(null) }
    var screen      by remember { mutableStateOf<Screen>(Screen.Explorer) }
    val files       = remember { mutableStateListOf<FileItem>() }
    var themeIndex  by remember { mutableStateOf(0) }
    val theme       = themes[themeIndex]

    val multicastLock = remember {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifi.createMulticastLock("ghostdrive-lock").apply { setReferenceCounted(true); acquire() }
    }
    DisposableEffect(Unit) { onDispose { multicastLock.release() } }

    BackHandler {
        when (screen) {
            is Screen.Video, is Screen.Image -> { showSystemBars(activity); screen = Screen.Explorer }
            is Screen.Details, is Screen.Search, is Screen.ThemePicker -> screen = Screen.Explorer
            is Screen.Explorer -> {
                if (currentPath != rootPath)
                    currentPath = currentPath.substringBeforeLast("/").ifEmpty { rootPath }
                else activity.finish()
            }
        }
    }

    LaunchedEffect(Unit) {
        val ip = NetworkScanner.discoverServer(context)
        if (ip != null) { RetrofitClient.setServerIp(ip); serverIp = ip }
    }

    LaunchedEffect(currentPath, serverIp) {
        if (serverIp == null) return@LaunchedEffect
        files.clear()
        try {
            val result = RetrofitClient.api.listFiles(currentPath)
            files.addAll(result.sortedWith(compareBy<FileItem> { it.type != "directory" }.thenBy { it.name }))
        } catch (e: Exception) { e.printStackTrace() }
    }

    if (serverIp == null) {
        Box(Modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = theme.accent)
                Text("Scanning for GhostDrive server...", color = theme.fg)
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(theme.bg)) {
        when (val s = screen) {
            is Screen.Explorer -> FileExplorerScreen(
                currentPath = currentPath, files = files, serverIp = serverIp!!, theme = theme,
                onNavigate        = { currentPath = it },
                onVideoSelected   = { hideSystemBars(activity); screen = Screen.Video(it) },
                onImageSelected   = { hideSystemBars(activity); screen = Screen.Image(it) },
                onDetailsSelected = { screen = Screen.Details(it) },
                onSearchOpen      = { screen = Screen.Search },
                onThemeOpen       = { screen = Screen.ThemePicker },
                onBack = { if (currentPath != rootPath) currentPath = currentPath.substringBeforeLast("/").ifEmpty { rootPath } }
            )
            is Screen.Search -> SearchScreen(
                serverIp = serverIp!!, theme = theme,
                onVideoSelected   = { hideSystemBars(activity); screen = Screen.Video(it) },
                onImageSelected   = { hideSystemBars(activity); screen = Screen.Image(it) },
                onDetailsSelected = { screen = Screen.Details(it) },
                onClose = { screen = Screen.Explorer }
            )
            is Screen.ThemePicker -> ThemePickerScreen(
                themes = themes, selectedIndex = themeIndex,
                onSelect = { themeIndex = it; screen = Screen.Explorer },
                onClose  = { screen = Screen.Explorer }
            )
            is Screen.Video   -> VideoPlayerScreen(s.file, serverIp!!) { showSystemBars(activity); screen = Screen.Explorer }
            is Screen.Image   -> ImageViewerScreen(s.file, serverIp!!) { showSystemBars(activity); screen = Screen.Explorer }
            is Screen.Details -> FileDetailsScreen(s.file, serverIp!!, theme) { screen = Screen.Explorer }
        }
    }
}

@Composable
fun ThemePickerScreen(themes: List<AppTheme>, selectedIndex: Int, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    val t = themes[selectedIndex]
    BackHandler { onClose() }
    Column(Modifier.fillMaxSize().background(t.bg).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            Text("⬅", fontSize = 18.sp, color = t.fg, modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
            Text("Choose Theme", color = t.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        themes.forEachIndexed { i, theme ->
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.card),
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onSelect(i) }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(theme.accent))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(theme.name, color = theme.fg, fontWeight = FontWeight.Medium)
                        Text("Tap to apply", color = theme.fg.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    if (i == selectedIndex) Text("✓", color = theme.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FileExplorerScreen(
    currentPath: String, files: List<FileItem>, serverIp: String, theme: AppTheme,
    onNavigate: (String) -> Unit, onVideoSelected: (FileItem) -> Unit,
    onImageSelected: (FileItem) -> Unit, onDetailsSelected: (FileItem) -> Unit,
    onSearchOpen: () -> Unit, onThemeOpen: () -> Unit, onBack: () -> Unit
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var uploading      by remember { mutableStateOf(false) }
    var uploadStatus   by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true; uploadStatus = null
        coroutineScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)!!
                val fileName    = uri.lastPathSegment?.substringAfterLast("/") ?: "upload_${System.currentTimeMillis()}"
                val tempFile    = File(context.cacheDir, fileName)
                tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                val mimeType    = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val part        = MultipartBody.Part.createFormData("file", tempFile.name, tempFile.asRequestBody(mimeType.toMediaTypeOrNull()))
                val pathBody    = currentPath.toRequestBody("text/plain".toMediaTypeOrNull())
                RetrofitClient.api.uploadFile(part, pathBody)
                uploadStatus = "Uploaded: ${tempFile.name}"
                val result = RetrofitClient.api.listFiles(currentPath)
                (files as MutableList).clear()
                files.addAll(result.sortedWith(compareBy<FileItem> { it.type != "directory" }.thenBy { it.name }))
                tempFile.delete()
            } catch (e: Exception) {
                uploadStatus = "Upload failed: ${e.message}"
            } finally { uploading = false }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Surface(color = theme.accent, shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ghost),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("GhostDrive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        IconButton(onClick = onThemeOpen) {
                            Icon(Icons.Rounded.Palette, contentDescription = "Theme", tint = Color.White)
                        }
                        IconButton(onClick = onSearchOpen) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
                        }
                        IconButton(onClick = { filePicker.launch("*/*") }, enabled = !uploading) {
                            Icon(
                                if (uploading) Icons.Rounded.HourglassTop else Icons.Rounded.Upload,
                                contentDescription = "Upload",
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Text("⬅ ", color = Color.White, modifier = Modifier.clickable { onBack() })
                    val parts = currentPath.split("/").filter { it.isNotEmpty() }
                    parts.forEachIndexed { i, part ->
                        val path = "/" + parts.take(i + 1).joinToString("/")
                        Text(
                            part,
                            color = if (i == parts.lastIndex) Color.White.copy(.6f) else Color.White,
                            modifier = if (i == parts.lastIndex) Modifier.padding(end = 4.dp)
                            else Modifier.clickable { onNavigate(path) }.padding(end = 4.dp)
                        )
                        if (i != parts.lastIndex) Text(" › ", color = Color.White)
                    }
                }
            }
        }

        uploadStatus?.let {
            Text(
                it, color = theme.fg,
                modifier = Modifier.fillMaxWidth()
                    .background(if (it.startsWith("Uploaded")) Color(0xFFDFF5E1) else Color(0xFFFFE0E0))
                    .padding(12.dp)
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(files) { file ->
                FileListRow(
                    file = file, serverIp = serverIp, theme = theme,
                    onClick = {
                        when {
                            file.type == "directory" -> onNavigate(file.path)
                            isVideo(file.name)       -> onVideoSelected(file)
                            isImage(file.name)       -> onImageSelected(file)
                            else                     -> downloadFile(context, file, serverIp)
                        }
                    },
                    onDetails = { onDetailsSelected(file) }
                )
                HorizontalDivider(color = theme.fg.copy(alpha = 0.07f))
            }
        }
    }
}

@Composable
fun FileListRow(file: FileItem, serverIp: String, theme: AppTheme, onClick: () -> Unit, onDetails: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            when {
                isVideo(file.name) -> {
                    SubcomposeAsyncImage(
                        model = buildThumbnailUrl(serverIp, file.path), contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        loading = { CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp) },
                        error   = { Text("🎬", fontSize = 24.sp) }
                    )
                    Text("▶", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
                isImage(file.name) -> {
                    SubcomposeAsyncImage(
                        model = buildStreamUrl(serverIp, file.path), contentDescription = "Preview",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        loading = { CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp) },
                        error   = { Text("🖼", fontSize = 24.sp) }
                    )
                }
                file.type == "directory"          -> Text("📁", fontSize = 28.sp)
                file.name.endsWith(".pdf",  true) -> Text("📕", fontSize = 28.sp)
                file.name.endsWith(".mp3",  true) ||
                        file.name.endsWith(".wav",  true) -> Text("🎵", fontSize = 28.sp)
                file.name.endsWith(".zip",  true) ||
                        file.name.endsWith(".tar",  true) -> Text("🗜", fontSize = 28.sp)
                else                              -> Text("📄", fontSize = 28.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, color = theme.fg, fontWeight = FontWeight.Medium, maxLines = 2)
            if (file.type == "file") Text(formatSize(file.size), color = theme.fg.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        Text("ℹ️", fontSize = 16.sp, modifier = Modifier.clickable { onDetails() }.padding(8.dp))
    }
}

@Composable
fun SearchScreen(
    serverIp: String, theme: AppTheme,
    onVideoSelected: (FileItem) -> Unit, onImageSelected: (FileItem) -> Unit,
    onDetailsSelected: (FileItem) -> Unit, onClose: () -> Unit
) {
    val context   = LocalContext.current
    var query     by remember { mutableStateOf("") }
    val results   = remember { mutableStateListOf<FileItem>() }
    var searching by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    BackHandler { onClose() }

    Column(Modifier.fillMaxSize().background(theme.bg).windowInsetsPadding(WindowInsets.statusBars)) {
        Surface(shadowElevation = 4.dp, color = theme.card) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⬅", fontSize = 18.sp, color = theme.fg,
                    modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { q ->
                        query = q
                        if (q.length >= 2) {
                            searching = true
                            scope.launch {
                                try { results.clear(); results.addAll(RetrofitClient.api.searchFiles(q)) }
                                catch (e: Exception) { e.printStackTrace() }
                                finally { searching = false }
                            }
                        } else results.clear()
                    },
                    placeholder = { Text("Search files...", color = theme.fg.copy(alpha = 0.5f)) },
                    singleLine  = true,
                    modifier    = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor        = theme.fg,
                        unfocusedTextColor      = theme.fg,
                        focusedBorderColor      = theme.accent,
                        unfocusedBorderColor    = theme.fg.copy(alpha = 0.3f),
                        cursorColor             = theme.accent,
                        focusedContainerColor   = theme.card,
                        unfocusedContainerColor = theme.card,
                    )
                )
            }
        }
        if (searching) Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(results) { file ->
                FileListRow(
                    file = file, serverIp = serverIp, theme = theme,
                    onClick = {
                        when {
                            isVideo(file.name) -> onVideoSelected(file)
                            isImage(file.name) -> onImageSelected(file)
                            else               -> downloadFile(context, file, serverIp)
                        }
                    },
                    onDetails = { onDetailsSelected(file) }
                )
                HorizontalDivider(color = theme.fg.copy(alpha = 0.07f))
            }
        }
    }
}

@Composable
fun FileDetailsScreen(file: FileItem, serverIp: String, theme: AppTheme, onClose: () -> Unit) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<FileDetails?>(null) }
    val scope   = rememberCoroutineScope()
    BackHandler { onClose() }
    LaunchedEffect(file.path) {
        scope.launch {
            try { details = RetrofitClient.api.fileDetails(file.path) }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    Column(Modifier.fillMaxSize().background(theme.bg).windowInsetsPadding(WindowInsets.statusBars).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⬅", fontSize = 18.sp, color = theme.fg,
                modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
            Text("File Details", color = theme.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        if (details == null) {
            CircularProgressIndicator(color = theme.accent)
        } else {
            val d = details!!
            if (isVideo(file.name)) {
                AsyncImage(
                    model = buildThumbnailUrl(serverIp, file.path), contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)
                )
                Spacer(Modifier.height(16.dp))
            }
            DetailRow("Name",     d.name,                  theme)
            DetailRow("Path",     d.path,                  theme)
            DetailRow("Size",     formatSize(d.size),      theme)
            DetailRow("Type",     d.mimeType ?: "Unknown", theme)
            DetailRow("Created",  formatDate(d.createdAt), theme)
            DetailRow("Modified", formatDate(d.modifiedAt),theme)
            Spacer(Modifier.height(24.dp))
            if (file.type == "file") {
                Button(
                    onClick  = { downloadFile(context, file, serverIp) },
                    colors   = ButtonDefaults.buttonColors(containerColor = theme.accent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download", color = Color.White) }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, theme: AppTheme) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.card),
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = theme.fg.copy(alpha = 0.5f), fontSize = 11.sp)
            Text(value, color = theme.fg, fontSize = 14.sp)
        }
    }
}

@Composable
fun VideoPlayerScreen(file: FileItem, serverIp: String, onClose: () -> Unit) {
    val context  = LocalContext.current
    val videoUrl = remember(file.path, serverIp) { buildStreamUrl(serverIp, file.path) }
    val player   = remember { ExoPlayer.Builder(context).build() }
    val savedPos   = remember { WatchHistoryManager.getPosition(context, file.path) }
    var showResume by remember { mutableStateOf(savedPos > 3000) }

    LaunchedEffect(videoUrl) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(videoUrl.toUri()))
        player.prepare()
        if (savedPos > 0) player.seekTo(savedPos)
        player.playWhenReady = true
    }

    LaunchedEffect(showResume) {
        if (showResume) {
            kotlinx.coroutines.delay(3000)
            showResume = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            WatchHistoryManager.savePosition(context, file.path, player.currentPosition)
            player.release()
        }
    }
    BackHandler {
        WatchHistoryManager.savePosition(context, file.path, player.currentPosition)
        onClose()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx: Context -> PlayerView(ctx).apply { this.player = player } }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color    = Color.Black.copy(alpha = 0.5f),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    WatchHistoryManager.savePosition(context, file.path, player.currentPosition)
                    onClose()
                }.padding(4.dp)
            ) {
                Text("Back", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Surface(
                color    = Color.Black.copy(alpha = 0.5f),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { downloadFile(context, file, serverIp) }.padding(4.dp)
            ) {
                Text("Download", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        if (showResume) {
            Text(
                "Resuming from ${formatDuration(savedPos)}",
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ImageViewerScreen(file: FileItem, serverIp: String, onClose: () -> Unit) {
    val context  = LocalContext.current
    val imageUrl = remember(file.path, serverIp) { buildStreamUrl(serverIp, file.path) }
    BackHandler { onClose() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = imageUrl, contentDescription = file.name,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color    = Color.Black.copy(alpha = 0.5f),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onClose() }.padding(4.dp)
            ) {
                Text("Back", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Surface(
                color    = Color.Black.copy(alpha = 0.5f),
                shape    = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { downloadFile(context, file, serverIp) }.padding(4.dp)
            ) {
                Text("Download", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Text(
            file.name, color = Color.White, fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}