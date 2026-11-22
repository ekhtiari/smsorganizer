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

### Sideloaded APK / “denied default SMS app”

Android 13+ (especially 15) blocks SMS role for apps not installed from Play Store until you allow restricted settings:

1. Open **Settings → Apps → SMS Organizer**
2. Tap the **⋮** menu → **Allow restricted settings**
3. Return to the app and tap **Set as default SMS app**, or choose it under **Settings → Apps → Default apps → SMS app**

## Build locally

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/SMS-Organizer-<version>-debug.apk`

## Signing

Android installs an update only when the new APK is signed with the same certificate. CI signs release and debug APKs with one keystore supplied through Actions secrets. The keystore stays out of git (`.gitignore` ignores `.p12`, `.pfx`, `.jks`, and `.keystore`).

Builds without those secrets still succeed. Debug uses the machine debug keystore, and the release APK is unsigned.

### Create a keystore

`keytool` prompts for the store password, key password, and certificate name. For PKCS12, use the same value for both passwords. `smsorganizer` is the alias of the current CI key.

```bash
keytool -genkeypair -v \
  -keystore sms-organizer-release.p12 \
  -storetype PKCS12 \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias smsorganizer
```

An OpenSSL PKCS12 file works the same way (`openssl pkcs12 -export -name smsorganizer -out sms-organizer-release.p12 ...`). Keep the file. Losing it means existing installs cannot be updated.

### GitHub Actions secrets

Repository **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Base64 of the `.p12` file: `base64 -w0 sms-organizer-release.p12` (macOS: `base64 -i sms-organizer-release.p12 \| tr -d '\n'`) |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias (`smsorganizer`) |
| `SIGNING_KEY_PASSWORD` | Key password (same as the store password for PKCS12) |

CI decodes the secret to a temporary `.p12` file and passes `storeType` `pkcs12` to Gradle. Tag builds (`v*`) fail when any secret is missing, so an unsigned APK is never published. Debug and release use different application ids (`com.ekhtiari.smsorganizer.debug` and `com.ekhtiari.smsorganizer`), so they do not replace each other. Debug APKs signed with this key do update older debug installs signed with the same key. A debug app left over from an earlier CI run used a different certificate and needs one uninstall before this key can replace it.

### Sign a local build

```bash
export SIGNING_KEYSTORE_PATH="$PWD/sms-organizer-release.p12"
export SIGNING_STORE_PASSWORD='your-store-password'
export SIGNING_KEY_ALIAS=smsorganizer
export SIGNING_KEY_PASSWORD='your-key-password'
./gradlew assembleRelease
```

`SIGNING_STORE_TYPE` is optional. Gradle uses `pkcs12` unless the file name ends in `.jks`. Set `SIGNING_STORE_TYPE=pkcs12` when a PKCS12 keystore is named `.jks`. The same names work as `-P` properties or in `~/.gradle/gradle.properties`.

## CI

Workflow: [`.github/workflows/android.yml`](.github/workflows/android.yml)

- Runs on push/PR to `main` / `master`, and on tags `v*`
- Signs both APKs when the [signing secrets](#signing) are set
- Uploads APK artifacts (`SMS-Organizer-release` when signed)
- On tags `v*`, requires the signing secrets and attaches only the signed release APK (`SMS-Organizer-<version>.apk`) to a GitHub Release. Debug APKs are built and verified in CI but are not release assets.

## Privacy

Classification runs fully on-device. No cloud sync or analytics.
