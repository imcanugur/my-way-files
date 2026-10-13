<div align="center">

# 📁 MyWayFiles (`my-way-files`)

**Modern Android APK Application powered by Compose Multiplatform & Kotlin 2.x**

[![Platform](https://img.shields.io/badge/Platform-Android_Only-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12.1-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android API](https://img.shields.io/badge/Android_API-34--37-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Docker](https://img.shields.io/badge/Docker-DevContainer_Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

<p align="center">
  <a href="https://github.com/imcanugur/my-way-files"><strong>Explore Repository »</strong></a>
  <br />
  <br />
  <a href="#-project-overview">Overview</a> •
  <a href="#-prerequisites">Prerequisites</a> •
  <a href="#-step-by-step-local-setup">Setup Guide</a> •
  <a href="#-apk-build--output">APK Output</a> •
  <a href="#-hardware-debugging">Device Debugging</a> •
  <a href="#-author">Author</a>
</p>

</div>

---

## 📌 Project Overview

**MyWayFiles** is an open-source Android mobile application designed to produce high-performance Android APK binaries using **Compose Multiplatform** and **Kotlin 2.x**.

The project is built around a zero-friction developer experience:
- **Dedicated Android Focus:** Streamlined exclusively for Android devices with zero overhead from unnecessary platforms.
- **Docker & Dev Container Ready:** Work on Android without cluttering your host OS with gigabytes of Android SDKs and toolchains.
- **Persistent Caching:** Gradle dependency downloads and ADB configurations survive container restarts.
- **Direct Hardware Testing:** USB passthrough and Wireless ADB debugging out of the box.

---

## 📋 Prerequisites

Choose **either** Local IDE or Docker depending on your preferred workflow:

### A. For Local Development (Android Studio)
* **OS:** Windows, macOS, or Linux.
* **IDE:** [Android Studio](https://developer.android.com/studio) (Ladybug / 2024.2 or newer recommended).
* **JDK:** OpenJDK 17 or JetBrains Runtime 17.
* **Android SDK:** Platforms 34, 35, or 36 installed via SDK Manager.

### B. For Containerized Development (Docker / VS Code)
* **Docker:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (with WSL2 enabled on Windows) or Docker Engine on Linux.
* **Editor (Optional):** VS Code with the [Dev Containers Extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).
* **USB Debugging (Windows Host):** [usbipd-win](https://github.com/dorssel/usbipd-win) installed on Windows for physical device connection.

---

## 🚀 Step-by-Step Local Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/imcanugur/my-way-files.git
cd my-way-files
```

*(Linux / macOS users only: ensure Gradle wrapper is executable)*
```bash
chmod +x ./gradlew
```

---

### Step 2: Choose Your Development Environment

#### 🟢 Method A: Android Studio (Recommended for Visual Development & Emulators)

1. Open **Android Studio**.
2. Select **File → Open** and choose the `my-way-files` folder.
3. Wait for the initial **Gradle Sync** to finish (first run may take 2–3 minutes to download dependencies).
4. Select `:androidApp` from the run configuration dropdown.
5. Select an Android Virtual Device (AVD Emulator) or connected physical device.
6. Click the green **Run (▶️)** button.

---

#### 🐳 Method B: VS Code Dev Containers (Zero Host Setup)

1. Open the cloned folder in **VS Code**.
2. When prompted in the bottom-right corner, click **"Reopen in Container"** (or press `F1` → type `Dev Containers: Reopen in Container`).
3. VS Code builds the image with pre-installed Android SDK 34-36, Java 17, and mounts the project inside `/workspaces/mobile`.
4. Open the integrated terminal and compile directly:
   ```bash
   ./gradlew :androidApp:assembleDebug
   ```

---

#### 💻 Method C: Docker Compose CLI (Headless / Terminal)

1. Start the container in the background:
   ```bash
   docker compose up -d --build
   ```
2. Enter the container shell:
   ```bash
   docker compose exec kmp-dev bash
   # or via container name:
   docker exec -it mobile_app bash
   ```
3. Inside the container (working directory `/workspaces/mobile`):
   ```bash
   ./gradlew :androidApp:assembleDebug
   ```

---

## 📦 APK Build & Output

When you run the assemble task, Gradle outputs the ready-to-install debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

### 📍 APK File Location:
```
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

You can transfer this APK directly to any Android device or install it automatically via ADB.

---

## 💻 Gradle Build Commands Reference

| Action | Command | Purpose |
| :--- | :--- | :--- |
| **Compile Debug APK** | `./gradlew :androidApp:assembleDebug` | Builds `androidApp-debug.apk` |
| **Full Project Build** | `./gradlew build` | Compiles and validates all modules |
| **Clean Artifacts** | `./gradlew clean` | Wipes temporary build caches |
| **Run Unit Tests** | `./gradlew :androidApp:test` | Executes unit tests |
| **Direct Install to Device** | `./gradlew :androidApp:installDebug -t` | Builds and installs APK onto connected device |

---

## 📱 Hardware Debugging (Physical Android Device)

### 1. Windows USB Passthrough (`usbipd-win`)

If you develop inside Docker / WSL2 on Windows, connect your physical Android phone directly:

1. Open **PowerShell as Administrator** on Windows.
2. List attached USB devices:
   ```powershell
   usbipd list
   ```
3. Attach your device to WSL (using `--auto-attach` so it persists during ADB resets):
   ```powershell
   usbipd attach --wsl --busid <BUSID> --auto-attach
   ```
4. In your container terminal, start ADB and verify:
   ```bash
   adb kill-server
   adb start-server
   adb devices -l
   ```
5. When your device status shows `device` (e.g. `xxxx device usb:...`), install and run:
   ```bash
   ./gradlew :androidApp:installDebug -t
   ```

---

### 2. Wireless ADB Debugging (Over Wi-Fi)

#### A. Via USB Switch (Port 1111 or 5555)
1. Connect via USB first and ensure it appears in `adb devices`.
2. Discover phone's Wi-Fi IP:
   ```bash
   adb shell ip -4 addr show wlan0
   ```
3. Enable TCP mode:
   ```bash
   adb tcpip 1111
   ```
4. Connect wirelessly:
   ```bash
   adb connect <PHONE_IP_ADDRESS>:1111
   adb devices -l
   ```

#### B. Android 11+ Pairing Code (No USB Cable Needed)
1. On phone: **Developer Options → Wireless debugging → Pair device with pairing code**.
2. In container terminal:
   ```bash
   adb pair <PHONE_IP_ADDRESS>:<PAIRING_PORT>
   # Enter the 6-digit code shown on your phone
   adb connect <PHONE_IP_ADDRESS>:<CONNECTION_PORT>
   adb devices -l
   ```

---

## 📂 Project Architecture

```
my-way-files/
├── .devcontainer/               # Containerized dev environment
│   ├── Dockerfile               # Pre-baked Java 17 & Android SDKs (API 34-36)
│   └── devcontainer.json        # Workspace configuration
├── androidApp/                  # Standalone Android Application Module
│   ├── build.gradle.kts         # Compose dependencies, packaging, minSdk & targetSdk
│   └── src/main/
│       ├── kotlin/              # App entry & screens (MainActivity.kt, App.kt)
│       └── res/                 # Android resources, vectors, icons
├── gradle/
│   └── libs.versions.toml       # Centralized Gradle version catalog
├── docker-compose.yml           # Persistent caches & USB bus volume mapping
└── README.md                    # Project documentation
```

---

## 🛠️ Tech Stack & Version Matrix

| Component | Version | Purpose |
| :--- | :--- | :--- |
| **Target Platform** | `Android (APK)` | Pure Android delivery |
| **Kotlin** | `2.4.20` | Core language |
| **Compose Multiplatform** | `1.12.1` | Declarative UI framework |
| **Material 3** | `1.12.0-alpha03` | Android Material 3 components |
| **Android Gradle Plugin** | `9.1.1` | Android build system |
| **Compile / Target SDK** | `36 / 37` | Latest Android API support |
| **Minimum SDK** | `24` | Android 7.0+ compatibility |
| **Java Runtime** | `OpenJDK 17` | Compilation target & container JVM |

---

## 👨‍💻 Author

**Can Ugur**  
*Full-stack Architect & Open Source Craftsman*  

[![GitHub](https://img.shields.io/badge/GitHub-Profile-181717?style=flat&logo=github)](https://github.com/imcanugur) 
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=flat&logo=linkedin)](https://linkedin.com/in/can-ugur)

> *"Stop configuring. Start building."*

---

## ⚖️ License

Distributed under the **MIT License**. See `LICENSE` for more information.

<p align="center">
  <i>Made with ☕ and passion in the dev cave.</i>
</p>