# Changelog

## 1.2.0

This is 1.1.9 plus the background backup work, so install this one instead of 1.1.9.

**Background backup**

* **The CPU is held awake while uploading.** WorkManager only keeps the device awake while it lets
  the worker run; a screen-off phone could still have the CPU pulled away mid-upload. The transfer
  worker now holds a partial wake lock for the duration, with a three hour cap so nothing can stick.
* **No more silent ten-minute cut.** Uploads need a foreground slot; if the system refuses one the
  worker used to run blind and get stopped. It now falls back to an untyped foreground service and,
  if that also fails, retries later instead of half-uploading a file.
* **A queue watchdog every fifteen minutes.** A queued row only moves when something kicks the queue.
  If that single kick was ever lost — process killed between enqueue and run, an aggressive OEM
  freeze, expedited quota used up — the queue would sit still until the app was opened. The watchdog
  checks for queued rows and kicks again, so a stuck queue repairs itself.

**Everything from 1.1.9 stays**

* Delete asks Both / Cloud only / Phone only, always through Trash.
* Share works for files that exist only in the cloud.
* Failed rows from deleted files are cleaned up when Home opens; **Clear failed** clears the rest.
* Permanent upload errors are abandoned instead of retried.
* Favourite sync with the phone gallery.

## 1.1.9

* **Delete now asks which copy goes.** Select files, tap Delete, and choose **Both**, **Cloud only**
  or **Phone only**. Cloud deletions always land in Trash (30 days, or until the trash is emptied),
  never a permanent delete. A removed phone copy is never the last copy: the Telegram copy stays put,
  so it can be downloaded again. **Cloud only** also records the file as excluded, because otherwise
  the next scan would see it on the phone and upload it straight back.
* **Share works for files that exist only in the cloud.** Tapping Share on a file that is not on the
  phone downloads it first, then opens the share sheet. The download stays as an ordinary local copy.
* **Failed uploads stop retrying forever.** A queue row with no file, no index entry or no local path
  can never succeed, so it is abandoned instead of being retried. This is what left rows failing
  again and again after an index rebuild.
* **Leftovers from deleted files are cleaned up automatically.** A file deleted from the phone left a
  FAILED record behind, and nothing ever cleared it — Home kept showing "7 failed" and offered to back
  up files that no longer exist. Opening Home now repairs that list: rows whose copy is in the channel
  are marked backed up, rows that were never uploaded are dropped, and the queue rows that belong to
  them go with them. No button to press.
* **Clear failed** in the Transfers menu drops the remaining failed rows in one tap. Files untouched.
* **The main buttons are visible instead of buried in the ⋮ menu.** Files and the gallery both show
  Select all, Share and Delete in the selection bar; move, copy, rename and the rest stay in ⋮.

## 1.0.14 — a stuck queue clears itself

* **Fixed the one-item-stuck-in-the-queue bug.** If the app was killed in the middle of an upload, the
  transfer stayed marked as running. The queue can never claim a running item again, but it still
  counted as active — so one row sat in the list forever and everything behind it looked stuck.
  Stale running rows (progress not moving for 15 minutes) are now put back in the queue when the
  queue starts and when the Transfers screen is opened.
* **A file deleted from the phone no longer blocks anything.** If the source file is gone, the
  transfer is now abandoned immediately (marked cancelled, with the reason kept) instead of being
  retried several times and failing again.
* **The crash dialog is gone** at the user's request. Nothing interrupts startup any more.
* **Favourites now talk to the phone's gallery.** Starring a photo or video in the app sets the
  device's own favourite flag (`IS_FAVORITE`, Android 11+), so it shows as favourite in the system
  gallery too. Settings → Storage → *Import favourites from the phone gallery* pulls the other way:
  photos the gallery already marks as favourite get starred here.

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
