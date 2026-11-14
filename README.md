# Memora Cloud

**Your phone's files, backed up into your own private Telegram channel.** No server, no Google
Drive, no monthly bill — the storage is the Telegram account you already have.

Developer: **Adnan** · Repository: <https://github.com/Adnanh444/Tele-Cloud> ·
Package: `com.adnan.memoracloud`

Memora Cloud is a fork of [TeleDrive](https://github.com/Mahmud0808/TeleDrive) by Mahmud0808,
used and modified under the Apache License 2.0. See `NOTICE` and `LICENSE`.

---

## Status

* **Part 1 (this drop):** complete rebrand to Memora Cloud — application id, package, icons,
  strings, channel markers, About page, links; Android-only GitHub Actions build + signed release
  pipeline with checksums and release metadata; mobile-friendly setup docs.
* **Part 2 (next):** in-app update download + SHA-256/signature verification + installer, a
  single restore/recovery wizard, duplicate-protection hardening with tests.
* **Part 3:** on-device AI photo intelligence (object/scene labelling, face grouping, semantic
  search), with an off switch and cloud-recoverable metadata.

The current, honest feature-by-feature state lives in [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md).

## What it does today

* Signs in with **your** Telegram account (api_id/api_hash + OTP + 2FA), storing credentials in
  Android Keystore-wrapped storage.
* Uses one private Telegram channel as your "drive": create it in the app, pick an existing one, or
  switch/remove drives later.
* Backs up photos, videos, documents, audio and other files automatically — instantly on new media
  or on a schedule — with folder selection, Wi-Fi-only, charging-only and mobile-data controls.
* Resumes interrupted uploads, retries failures, and shows pending/uploading/uploaded/failed.
* **Skips duplicates by SHA-256 content hash**, not by filename, so a renamed copy is still
  recognised; the hash lives both in the local database and in the cloud manifest.
* Keeps a **cloud-side index**: a manifest in every message caption plus a gzipped index snapshot
  and folder tree in the channel. Reinstall the app on another phone, sign in, and the library
  rebuilds itself from Telegram.
* Browses that library as a cloud drive: gallery, folders, search, sort, thumbnails, streamed video
  preview, PDF/text/archive preview, bulk download, safe delete through a trash bin.
* Optional AES-GCM encryption of uploaded files, biometric app lock, SOCKS5/MTProxy support.

## Install

1. Open the [Releases](https://github.com/Adnanh444/Tele-Cloud/releases) page.
2. Download `MemoraCloud-<version>.apk`.
3. Open it and confirm the install (Android always asks; there is no silent self-update).

Keep the same signing key across releases and app data survives every update.

## Build it yourself

Everything runs on GitHub Actions — no local Android Studio needed.

| Workflow | Trigger | Result |
| --- | --- | --- |
| **Build** | every push / PR / manual | debug APK artifact |
| **Release** | tag `v*` or manual | signed release APK + `SHA256SUMS.txt` + `update.json` on a GitHub Release |

Required secrets: `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
See [`docs/SIGNING_AND_RELEASES.md`](docs/SIGNING_AND_RELEASES.md) — there is a one-time workflow
that generates the keystore for you if you only have a phone.

Phone-only setup: [`docs/GITHUB_MOBILE_SETUP.md`](docs/GITHUB_MOBILE_SETUP.md).

Local build (JDK 21 + Android SDK 37):

```bash
./gradlew :android:assembleDebug        # debug
./gradlew :android:assembleRelease      # release, needs KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD
```

Optional: `-PsplitAbis=false` for one universal APK, `-PwithFfmpegDecoder` to also cross-compile
media3's FFmpeg audio extension (AC-3/E-AC-3/DTS audio in videos — slower CI, cached afterwards).

## Repository layout

```
android/   Android app: TDLib client, WorkManager workers, MediaStore, notifications, preview
shared/    Domain + data: Room (17 migrations), repositories, transfer engine, crypto, Telegram
ui/        Compose Multiplatform UI: screens, view models, theme, strings
desktop/   Upstream desktop targets — kept in the tree, not built or supported here
docs/      Audit, feature status, mobile setup, signing/release guides
```

Architecture notes and the audit this fork started from: [`docs/ARCHITECTURE_AUDIT.md`](docs/ARCHITECTURE_AUDIT.md).

## Requirements

* Android 8.0 (API 26) or newer.
* `api_id` / `api_hash` from <https://my.telegram.org> (Telegram's rule for any third-party
  client — one pair per account, a few minutes to create).
* Access to the Telegram account and the backup channel is the only thing that can restore your
  data. Keep both.

## Security notes

* No backend server is involved; files go from your phone to Telegram's servers and back.
* Credentials and the TDLib session are stored encrypted with a Keystore-backed key.
* Files can be encrypted end-to-end-ish (AES-GCM, key wrapped by the device keystore) with an
  optional passphrase-protected key backup stored in the channel.
* Nothing is sent to any analytics or AI service. (AI features, when they arrive, run on-device.)
* Deleting a file in the app deletes the corresponding Telegram message — after a confirmation and
  via Trash.

## License

Apache License 2.0 — see `LICENSE`. Original work © Mahmud0808 (TeleDrive) and contributors;
modifications for Memora Cloud © Adnan.
