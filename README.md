# SMS Organizer

Android SMS app inspired by the retired Microsoft SMS Organizer.

## Features

- **Default SMS app** — send, receive, and store messages
- **Categories** — Personal, Transactions, Offers, OTP (on-device rules)
- **OTP** — detect codes and one-tap copy (including from notifications)
- **Dual / multi-SIM** — pick which SIM to send with; messages keep subscription id
- **Search** across conversations
- **GitHub Actions** builds debug + release APKs on every push

## Requirements

- Android 8.0+ (API 26)
- Set this app as the **default SMS app** for full send/receive
- Grant SMS, phone, contacts, and notification permissions

## Build locally

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI

Workflow: [`.github/workflows/android.yml`](.github/workflows/android.yml)

- Runs on push/PR to `main` / `master`
- Uploads APK artifacts
- On tags `v*`, attaches APKs to a GitHub Release

## Privacy

Classification runs fully on-device. No cloud sync or analytics.
