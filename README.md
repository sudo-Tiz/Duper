# Duper

Android app to find your phone remotely via SMS commands.

> **Why "Duper"?** It's French *verlan* (slang made by reversing syllables) for *perdu* → **du-per**. *Perdu* means "lost" in French. You get it.

|  |  |
| :---: | :---: |
| ![screenshot1](assets/screenshot1.png) | ![screenshot2](assets/screenshot2.png) |   
| | |

## Features

- **Remote Ring** – Trigger alarm and LED flash at max volume
- **Remote Locate** – Get GPS coordinates via SMS
- **SMS-Based** – No internet required on target device
- **Customizable** – Set your own command prefix and optional password
- **Auto-GPS** – Optionally enable location services automatically (requires ADB setup)


## Quick Start

1. Download latest APK from [Releases](https://github.com/sudo-tiz/Duper/releases)
2. Enable Ring or Locate in the app and grant only the requested permissions
3. From another phone, send a configured command


## Commands

| Command | Action |
|---------|--------|
| `<prefix>` or `<prefix> ring` | Triggers alarm when Ring Mode is enabled and no Ring password is set |
| `<prefix> <ring-password>` or `<prefix> ring <ring-password>` | Triggers alarm when Ring Mode is enabled and a Ring password is set |
| `<prefix> locate <locate-secret>` | Sends GPS coordinates via SMS when Locate Mode is enabled |

Ring and Locate are disabled by default. Ring can use an optional password. Locate requires its own non-empty secret.

## Permissions

- **SMS** – Requested when enabling Ring or Locate to receive commands and send responses
- **Camera** – Requested when enabling Ring to control the device torch; Duper does not capture photos or video
- **Location** – Requested when enabling Locate for GPS tracking
- **Background Location** – Requested when enabling Locate to track when the app is closed
- **Notifications** – Optional; shows accepted or refused command attempts on the target phone

### ⚠️ Android 15+ Note 
> Android 15+ blocks SMS permissions by default.

If the SMS toggle is greyed out:
1. Go to **Settings > Apps > Duper**.
2. Tap the **three-dot menu (⋮)** in the top-right corner.
   *(Note: On some devices, you may need to tap "Permissions" first to see this menu).*
3. Select **Allow restricted settings**. 
4. Return to **Permissions** and grant **SMS** access.

## Auto-Enable GPS (Optional)

If your phone's location services are turned off, the locate command will fail by default.
Duper can automatically enable location services if you grant it a special permission via the Android Debug Bridge (ADB).

```sh
adb shell pm grant fr.sudotiz.duper android.permission.WRITE_SECURE_SETTINGS
```

Requires USB debugging enabled. One-time setup.

`WRITE_SECURE_SETTINGS` is an optional privileged permission. It cannot be granted through the normal Android permission screen and is not required to use Duper when location services are already enabled.

## Roadmap

### 1. Secure Remote Commands

- Disable Ring Mode and Locate Mode by default.
- Keep `<prefix>` and `<prefix> ring` as equivalent ring commands.
- Require `<prefix> locate <locate-secret>` and a non-empty secret to enable Locate Mode.
- Silently ignore invalid, disabled, and blocked commands.
- Safely disable insecure existing configurations during migration.

### 2. Sender Blocking

- Add a local blacklist with no default entries.
- Check it before replying, recording commands, ringing, or locating.
- Support manual entry, paste, search, editing, and removal.
- Normalize phone numbers and keep the list local.

### 3. Privacy and Backup Protection

- Correct Android backup and data-extraction rules to exclude secrets, lists, history, settings, and location data.
- Audit and remove unnecessary permissions.

### 4. CI/CD Reliability

- Keep Android 7.0 (API 24) compatibility.
- Standardize local and CI builds on JDK 17.
- Run Android lint and validate minified release builds before publication.
- Make versions source-controlled, Git-tag-aligned, and collision-safe.
- Pin actions and Semantic Release tooling; use a lockfile and `npm ci`.
- Prevent concurrent releases and explicitly provide Android SDK 37 tooling.
- Use consistent Gradle caching in validation and release workflows.

### 5. F-Droid Readiness

- Add version-code-specific changelogs and current Fastlane screenshots.
- Update store metadata after the security changes are implemented.

### 6. Import and Export

- Add plain-text blacklist import and export through Android's system document picker.
- Use UTF-8, one phone number per line.
- Preview accepted, duplicate, invalid, and normalized entries before importing.
- Export only on user action, without cloud sync, automatic downloads, or built-in external lists.

## License

GPL-3.0 – See [LICENSE](LICENSE)
