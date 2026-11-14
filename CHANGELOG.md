# Changelog

## 1.0.0 — Part 1

Identity and delivery groundwork, no new end-user features.

* Rebranded to **Memora Cloud** (developer Adnan): launcher label in all shipped locales, About
  screen, notification strings, default download folder, channel titles and cloud markers.
* New application id and Kotlin package: `com.adnan.memoracloud`.
* New launcher icon set (adaptive foreground, monochrome, round, notification glyph) at 5 densities.
* Database and Room schema directory renamed to `MemoraCloudDatabase` / `memoracloud.db`
  (all 17 schema versions and migration tests kept).
* Version reset to 1.0.0 (versionCode 1); CI can override both.
* Update checker now targets `github.com/Adnanh444/Tele-Cloud`.
* Android-only GitHub Actions: `Build` (debug artifact) and `Release` (signed APK, signature
  verification, `SHA256SUMS.txt`, `update.json`, GitHub Release).
* One-time helpers: `Import source archive` (upload the ZIP from a phone) and
  `Generate signing keystore`.
* Removed upstream desktop/website CI jobs, upstream screenshots and issue templates.
* Docs: architecture audit, feature status, mobile setup, signing and release guide, NOTICE.

Inherited from TeleDrive 1.5.0: Telegram-backed storage and channel management, cloud manifest and
index snapshot recovery, SHA-256 duplicate protection, WorkManager backup with resume/retry,
folder selection, Wi-Fi/charging/mobile-data rules, gallery and file browser with streaming preview,
trash, encryption, biometric app lock and proxy support.
