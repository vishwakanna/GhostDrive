# GhostDrive

A client-server app that lets you browse, stream, and download files from your computer on your Android phone over local WiFi. No internet required, no cloud, no accounts.

## Features

- Browse your file system from your phone
- Stream videos without downloading them
- View images fullscreen
- Download any file to your phone with progress notifications
- Upload files from your phone to your computer
- Search files recursively across all folders
- Resume video playback from where you left off
- Video and image thumbnail previews in the file list
- 8 built-in color themes

## How it works

The server runs on your computer and exposes a REST API on port 8080. The Android app scans your local network, finds the server automatically using UDP broadcast, and connects to it. Both devices must be on the same WiFi network.

## Requirements

**Server:**
- Java 21 or higher — download from [adoptium.net](https://adoptium.net)
- ffmpeg — required for video thumbnails

**Android:**
- Android 9 or higher
- Same WiFi network as the server

## Installation

### Step 1 — Install Java

**Linux (Arch):**
```bash
sudo pacman -S jdk21-openjdk
```

**Linux (Ubuntu / Debian):**
```bash
sudo apt install openjdk-21-jdk
```

**Linux (Fedora):**
```bash
sudo dnf install java-21-openjdk
```

**macOS:**
```bash
brew install openjdk@21
```

**Windows:**

Download and run the installer from [adoptium.net](https://adoptium.net/temurin/releases/?version=21). Choose Windows x64, JDK, .msi installer. After installing, open Command Prompt and verify:
```
java -version
```

### Step 2 — Install ffmpeg

ffmpeg is used to generate video thumbnails. Without it the server still works but videos will show a placeholder instead of a thumbnail.

**Linux (Arch):**
```bash
sudo pacman -S ffmpeg
```

**Linux (Ubuntu / Debian):**
```bash
sudo apt install ffmpeg
```

**Linux (Fedora):**
```bash
sudo dnf install ffmpeg
```

**macOS:**
```bash
brew install ffmpeg
```

**Windows:**

Download from [ffmpeg.org/download.html](https://ffmpeg.org/download.html). Extract the zip, then add the `bin` folder to your system PATH:

1. Search "environment variables" in the Start menu
2. Click "Edit the system environment variables" → "Environment Variables"
3. Under "System variables" find "Path" → click Edit → click New
4. Paste the full path to the ffmpeg `bin` folder (e.g. `C:\ffmpeg\bin`)
5. Click OK on all dialogs

Verify with:
```
ffmpeg -version
```

### Step 3 — Configure the server

Before running, open `FileController.java` and change the root directory to match your system:

**Linux / macOS:**
```java
private final String ROOT_DIRECTORY = "/home/yourusername";
```

**Windows:**
```java
private final String ROOT_DIRECTORY = "C:/Users/yourusername";
```

### Step 4 — Run the server

Download `ghostdrive-server.jar` from the [releases page](../../releases).

**Linux / macOS:**
```bash
java -jar ghostdrive-server.jar
```

**Windows:**
```
java -jar ghostdrive-server.jar
```

The server starts on port 8080 and begins broadcasting its presence on the network. Leave this terminal open while using the app.

### Step 5 — Install the Android app

Download `app-release.apk` from the [releases page](../../releases) and install it on your phone.

On most phones you need to allow installation from unknown sources. When you tap the APK your phone will ask for permission — tap Allow. If it does not ask, go to Settings → Apps → Special app access → Install unknown apps → find your file manager → allow it.

---

## Auto-start the server

### Linux (systemd)

```bash
mkdir -p ~/.config/systemd/user

cat > ~/.config/systemd/user/ghostdrive.service << EOF
[Unit]
Description=GhostDrive Server

[Service]
ExecStart=java -jar /home/yourusername/ghostdrive-server.jar
Restart=on-failure

[Install]
WantedBy=default.target
EOF

systemctl --user enable ghostdrive
systemctl --user start ghostdrive
```

### macOS (launchd)

Create `~/Library/LaunchAgents/com.ghostdrive.server.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.ghostdrive.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-jar</string>
        <string>/Users/yourusername/ghostdrive-server.jar</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
```

Then load it:
```bash
launchctl load ~/Library/LaunchAgents/com.ghostdrive.server.plist
```

### Windows (startup folder)

Create a file called `ghostdrive.bat`:
```bat
@echo off
java -jar C:\Users\yourusername\ghostdrive-server.jar
```

Press `Win + R`, type `shell:startup`, press Enter. Copy `ghostdrive.bat` into that folder. The server will start automatically on login.

---

## Building from source

**Server:**

```bash
cd server
./mvnw clean package -DskipTests
java -jar target/ghostdrive-server.jar
```

**Android:**

Open the `android` folder in Android Studio, connect your phone via USB, and click Run. To build a release APK go to Build → Generate Signed App Bundle / APK → APK.

---

## Configuration

The server runs on port 8080 by default. To change it, edit `src/main/resources/application.properties`:

```properties
server.port=9090
```

---

## Project structure

```
GhostDrive/
├── android/                         — Android app (Jetpack Compose)
│   └── app/src/main/java/com/example/ghostdrive/
│       ├── MainActivity.kt          — all UI screens
│       ├── Models.kt                — data classes
│       ├── GhostDriveApi.kt         — Retrofit API interface
│       ├── RetrofitClient.kt        — HTTP client
│       ├── NetworkScanner.kt        — auto server discovery
│       ├── ServerDiscovery.kt       — UDP broadcast listener
│       └── WatchHistoryManager.kt   — resume playback
└── server/                          — Spring Boot server
    └── src/main/java/com/ghostdrive/server/
        ├── ServerApplication.java
        ├── controller/
        │   ├── FileController.java       — file API endpoints
        │   └── ThumbnailController.java  — ffmpeg thumbnails
        └── dto/
            └── FileInfo.java
```

## API endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/files?path=` | List files in a directory |
| GET | `/api/search?query=` | Search files recursively |
| GET | `/api/details?path=` | Get file metadata |
| GET | `/api/stream?path=` | Stream a file with range support |
| GET | `/api/download?path=` | Download a file |
| GET | `/api/thumbnail?path=` | Get video thumbnail |
| POST | `/api/upload` | Upload a file |

## Tech stack

**Android:** Kotlin, Jetpack Compose, ExoPlayer, Retrofit, OkHttp, Coil

**Server:** Java 21, Spring Boot, ffmpeg
