# GhostDrive

A self-hosted file explorer for Android. Browse, stream, and download files from your laptop over local WiFi — no cloud, no accounts, no subscription.

---

## What it does

- Browse your laptop's file system from your phone
- Stream videos directly without downloading them first
- View images fullscreen
- Download any file to your phone
- Upload files from your phone to your laptop
- Search across all files recursively
- Auto-discovers the server on your local network — no IP setup needed
- Resumes video playback from where you stopped

---

## Requirements

**Laptop (server)**
- Java 21 or later
- ffmpeg (for video thumbnails)
- Both devices on the same WiFi network

**Phone (client)**
- Android 8.0 or later

---

## Setup

### Server

Download `ghostdrive-server.jar` from the releases page and run it:

```bash
java -jar ghostdrive-server.jar
```

The server starts on port 8080 and begins broadcasting its presence on the local network automatically.

To change which folder is shared, open `FileController.java` and edit the `ROOT_DIRECTORY` field before building.

**Run on startup (Linux)**

Create a systemd service at `~/.config/systemd/user/ghostdrive.service`:

```ini
[Unit]
Description=GhostDrive Server

[Service]
ExecStart=java -jar /path/to/ghostdrive-server.jar
Restart=on-failure

[Install]
WantedBy=default.target
```

Enable it:

```bash
systemctl --user enable ghostdrive
systemctl --user start ghostdrive
```

**Run on startup (Windows)**

Create a `.bat` file containing:

```bat
java -jar C:\path\to\ghostdrive-server.jar
```

Press `Win + R`, type `shell:startup`, and place the `.bat` file in that folder.

---

### Android App

Install the APK from the releases page. Open the app and it will scan your local network and connect to the server automatically within a few seconds.

---

## Building from source

**Server**

```bash
cd server
./mvnw clean package -DskipTests
```

The JAR will be at `target/ghostdrive-server.jar`.

**Android**

Open the `android` folder in Android Studio and build normally. To produce a release APK: Build > Generate Signed App Bundle / APK.

---

## Project structure

```
ghostdrive/
├── android/                       Android app (Jetpack Compose)
│   ├── MainActivity.kt            All UI screens
│   ├── Models.kt                  Data classes
│   ├── GhostDriveApi.kt           Retrofit API interface
│   ├── RetrofitClient.kt          HTTP client
│   ├── NetworkScanner.kt          LAN subnet scanner
│   ├── ServerDiscovery.kt         UDP broadcast listener
│   └── WatchHistoryManager.kt     Resume playback storage
│
└── server/                        Spring Boot server
    ├── FileController.java        Browse, stream, download, upload endpoints
    ├── ThumbnailController.java   Video thumbnail generation via ffmpeg
    ├── DiscoveryBroadcaster.java  UDP broadcast for auto-discovery
    └── ServerApplication.java    Entry point
```

---

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/files?path=` | List files in a directory |
| GET | `/api/search?query=` | Search files recursively |
| GET | `/api/details?path=` | File metadata |
| GET | `/api/stream?path=` | Stream file with Range request support |
| GET | `/api/download?path=` | Download file |
| GET | `/api/thumbnail?path=` | Video thumbnail JPEG |
| POST | `/api/upload` | Upload file to a directory |

---

## Tech stack

**Android:** Kotlin, Jetpack Compose, Retrofit, OkHttp, ExoPlayer, Coil

**Server:** Java 21, Spring Boot 3, ffmpeg
