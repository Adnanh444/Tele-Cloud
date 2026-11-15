# Changelog

## 1.0.13 — clean up Google Takeout leftovers

* **New: Settings → Backup → "Clean up Google Takeout sidecars".** A Takeout export writes a small
  `.supplemental-metadata.json` next to every photo; with the "Other" file type enabled those were
  backed up too, one extra message per photo. The new row counts them, then deletes them from the
  index and from Telegram in bulk. Photos and videos are never touched.
* Turning a file type off stops new uploads but cannot cancel what is already queued — the cleanup
  row handles that backlog as well, because it cancels each queued transfer before deleting.

## 1.0.12 — your app, your name

* **About screen no longer credits or links the upstream project.** The "Upstream project" row is
  gone, and the licence line now reads simply "Apache License 2.0". Legal attribution is kept in the
  `NOTICE` file, where the licence requires it.
* README rewritten as a user guide with screenshots, install steps, restore instructions,
  troubleshooting and a table of where data lives.
* Changelog now describes every release a user can actually install.

## 1.0.11 — the app opens again

* **Fixed a launch crash that made 1.0.8 and 1.0.9 unusable on upgraded installs.** 1.0.8 removed
  four database tables without increasing the database version, so Room refused to open the file
  ("cannot verify the data integrity"). The database is now version 20 with a real migration from
  19; the index, settings and login survive the update.
* Crash reporting: an uncaught exception is written to `files/crash/last.txt` and to
  `Downloads/MemoraCloud/memora-crash.txt`, and shown in a dialog on the next start.
* If a future release ever ships a schema change without a migration again, the app resets its
  index once instead of becoming permanently unable to start.

## 1.0.8 — restore, and much less

* **Restore from Telegram** (Settings → Account): choose a scope, see how many files that is and how
  many are missing from this device, then queue the download. Skips files already on the device by
  default. Restored files go to `Downloads/MemoraCloud`.
* Removed the on-device photo tagging and face grouping experiments, their tables, model and
  dependencies.
* Removed text/tag search; search is by file name.
* Update checker no longer fails with HTTP 403: it reads the release metadata from the release page
  instead of the rate-limited API.
* English-only build: unused translations removed.

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
