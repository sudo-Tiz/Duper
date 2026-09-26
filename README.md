# Duper

**Lost your phone? Send it "duper".**

Find your Android phone remotely via SMS. No internet needed on the target device.

## Badges

[![F-Droid](https://img.shields.io/f-droid/v/fr.sudotiz.duper?label=F-Droid)](https://f-droid.org/packages/fr.sudotiz.duper)
[![GitHub Release](https://img.shields.io/github/v/release/sudo-Tiz/Duper)](https://github.com/sudo-Tiz/Duper/releases)

> **Why "Duper"?** French *verlan* for *perdu* ("lost"): **du-per**. The app that finds what's lost.

|  |  |
| :---: | :---: |
| ![screenshot1](assets/screenshot1.png) | ![screenshot2](assets/screenshot2.png) |
| | |

## Features

- **Ring** – Alarm + flash at max volume, works in silent mode
- **Locate** – GPS coordinates by SMS, with OpenStreetMap link
- **Offline** – Pure SMS, no internet, no account, no cloud
- **Configurable** – Prefix, optional Ring password, required Locate secret

## Quick Start

1. Download latest APK
    - [Releases](https://github.com/sudo-tiz/Duper/releases)
    - [F-Droid](https://f-droid.org/packages/fr.sudotiz.duper)
2. Enable Ring or Locate in the app and grant only the requested permissions
3. From another phone, send a configured command

## Commands

| Command | Action |
|---------|--------|
| `<prefix>` | Ring (if no password set) |
| `<prefix> ring` | Same |
| `<prefix> <password>` | Ring (if password set) |
| `<prefix> locate <secret>` | Send GPS coordinates |

- Prefix: one word. Password/secret: case-sensitive.
- Ring and Locate are **off by default**.
- Locate secret is **required**. Ring password is optional; SMS confirmation optional.
- Commands only run while the phone is **locked**. Unlocked → refused, no reply.

## Permissions

- **SMS** – Requested when enabling Ring or Locate to receive commands and send responses
- **Camera** – Requested when enabling Ring to control the device torch; Duper does not capture photos or video
- **Location** – Requested when enabling Locate for GPS tracking
- **Background Location** – Requested when enabling Locate to track when the app is closed
- **Notifications** – Optional; shows accepted or refused command attempts on the target phone

## Privacy

No account, no server, no tracking, no internet permission. Secrets, settings, history, and last location stay on the device and are excluded from Android backups and transfers.

Ring uses the alarm audio stream. Do Not Disturb may limit alerts (DND support planned).

## Troubleshooting

### Android 15+: SMS greyed out

Android 15+ blocks SMS by default. One-time fix:

1. **Settings → Apps → Duper → ⋮ → Allow restricted settings**
2. Back to the app, grant **SMS**

## Auto-Enable GPS (Optional)

If location services are off, locate fails. One-time ADB grant lets Duper re-enable them:

```sh
adb shell pm grant fr.sudotiz.duper android.permission.WRITE_SECURE_SETTINGS
```

Requires USB debugging. Optional — not needed if location is already on.

## Roadmap

- [ ] Sender blocking: local blacklist with add, edit, search, paste
- [ ] Optional Do Not Disturb support
- [ ] Import/export blacklist as UTF-8 via document picker

## License

GPL-3.0 – See [LICENSE](LICENSE)
