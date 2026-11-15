# Memora Cloud architecture audit

This is the inspection report produced **before** any modification, plus the decisions taken for
Memora Cloud. It describes the real code in this repository, not an idealised plan.

Starting point: the open-source baseline recorded in `NOTICE` (Apache License 2.0), at 691 Kotlin
files / ~55 700 lines. Attribution for that baseline lives in `NOTICE` only.

---

## 1. What the project actually is

TeleDrive is **not** a single-module app. It is a Kotlin Multiplatform project with four Gradle
modules:

| Module | Purpose | Files | Notes |
| --- | --- | --- | --- |
| `:android` | Android app shell, `Application`, `MainActivity`, TDLib client, WorkManager workers, MediaStore, notifications, permissions, media preview (Media3) | 105 | `applicationId com.drdisagree.teledrive`, `minSdk 26`, `targetSdk 37` |
| `:shared` | Domain models, use cases, Room database + DAOs + migrations, repositories, transfer engine, Telegram abstraction, crypto, update checker, logging | 324 | Multiplatform: `commonMain`, `jvmCommonMain`, `androidMain`, `desktopMain` |
| `:ui` | Compose Multiplatform UI: screens, navigation, view models, theme, compose-resources strings | 216 | Same screen code shared by Android and desktop |
| `:desktop` | Compose Desktop launcher (Windows/Linux/macOS), TDLight backend, VLC playback | 46 | Not built for Memora Cloud |

Key libraries: TDLib (Android) / TDLight (desktop), Room 2.8, WorkManager 2.11, Koin 4.2 DI,
Compose Multiplatform 1.12, Paging 3, Coil 3, Media3 1.11, kotlinx.serialization, Zip4j, Biometric.

Layer flow: `UI (ui module) → ViewModel (Koin) → Repository (shared) → TelegramClient (TDLib)`,
with Room as the local cache and the Telegram message captions as the durable cloud index.

---

## 2. Feature inventory found in the source (inherited, working)

### Telegram storage and account
* Phone-number + OTP + 2FA login through TDLib; session and `api_id`/`api_hash` stored encrypted
  with an Android Keystore-wrapped key (`core/crypto`, `KeystoreManager`).
* Multiple "drives" = private channels. `StorageChannelDao` + `ChannelRepositoryImpl` create,
  select, rename, switch and remove them; `createStorageChannel(label)` creates a private channel
  titled `<brand> <label>` and writes a marker (`#memoracloud-storage`) into its description.
* Rediscovery after a reinstall works: `TdLibTelegramClient.findOrCreateStorageChannel()` searches
  chats by title/servery (`SearchChatsOnServer`), verifies ownership + marker, and adopts the
  existing channel instead of creating a second one.
* Proxy support (`ProxyRepositoryImpl`, SOCKS5/MTProxy) and Telegram rate-limit handling
  (`TelegramPacer`, `AppError.RateLimited`).

### Durable cloud index (this is the important part for recovery)
* Every uploaded file gets a **manifest in its caption**: `td1:{json}` (plain) or
  `tde1:{base64 AES-GCM}` (when file encryption is on). It carries file id, name, folder path,
  mime type, size, **SHA-256 content hash**, width/height/duration, created/modified dates,
  hidden/archived/favourite/trashed flags, encryption flag and forward-compatible `extra` fields.
  See `ManifestCodec`, `RemoteFileManifest`, `FileManifestPublisher`.
* A gzipped **index snapshot** (`memoracloud.index.json.gz`, marker `#memoracloud-index`) and a
  **folder tree file** (`memoracloud.folders.json`, marker `#memoracloud-folders`) are published to
  the channel, so a fresh install can rebuild the whole library without the local database
  (`IndexSnapshotFile`, `IndexSnapshot`, `FolderStateSynchronizer`, `SyncRepositoryImpl`).
* Encryption-key backup file (`memoracloud.keybackup`) with passphrase, so encrypted libraries are
  recoverable (`KeyBackupRepositoryImpl`, `KeyBackupCodec`).

### Backup engine
* WorkManager chain: `MediaSweepWorker` (scan), `MediaWatchWorker` / `MediaStoreWatcher`
  (instant backup on new media), `ScheduledBackupWorker` (interval), `BackupResumeWorker`,
  `TransferQueueWorker`, `PublishOutboxWorker`, `CacheCleanupWorker`, `TrashCleanupWorker`,
  `UpdateCheckWorker`, plus `MediaTriggerService` for long transfers.
* Chunked uploads with `FilePartEntity` + `PartUploader` + `RangeInputStream`: interrupted uploads
  resume from the remaining parts, failed parts retry (`transferRetryCount`), progress is reported
  through `BackupSessionTracker` and notifications.
* Settings already exist for: auto backup on/off, instant backup, **selected folders**
  (`backupFolders` + `ExclusionDao`), **Wi-Fi only**, **charging only**, metered-data allowance,
  backup interval hours, max file size, concurrency, delete-after-upload (opt-in).
* `DecideBackupActionUseCase` + `BackupRecordEntity`: size and mtime first, content hash only when
  the mtime changed. `FileRepositoryImpl.liveMatchesBySize()` + hash comparison detects a
  duplicate **even when the file was renamed**, because the lookup is by size in the same channel
  and then by hash, never by name.

### Cloud browser
* Gallery (albums, grid), Files (folders, list/grid, sort, search), Search screen, Trash (30-day
  auto purge, restore), Notes, Transfers (pending/active/failed), per-file detail with original
  date, upload date, size, hash.
* Preview without downloading the whole file: `TelegramDataSource` + `PartedTelegramDataSource`
  feed Media3 ExoPlayer for video streaming, Coil for thumbnails, and in-app PDF/text/archive
  viewers. Download, multi-select, bulk download, "make available offline".
* Deleting goes through Trash first; permanent deletion removes the Telegram message
  (`PendingDeleteEntity`).

### Update checking (partial — see gaps)
* `UpdateChecker` reads `api.github.com/repos/<owner>/<repo>/releases/latest`, compares dotted
  versions, `UpdateCheckWorker` posts a notification once a day, and the About screen shows the
  release and opens the release page in a browser. **There is no APK download, no checksum check
  and no installer flow in the upstream code.**

### Quality infrastructure
* Room with 17 schema versions in `shared/schemas` and real migration tests
  (`ReleaseUpgradeMigrationTest`, `FolderClockMigrationTest`, …).
* JVM unit tests for the transfer engine, dedupe decision, manifest codec, folder merge, crypto.
* R8 minify + resource shrink with keep rules for TDLib/Room/serialization.

---

## 3. Gaps versus the Memora Cloud brief

| Requirement | State in upstream | Action |
| --- | --- | --- |
| Telegram as the only provider | Already true — no Drive/Firebase/AWS dependency anywhere | none |
| Branding | "TeleDrive" in app name, icons, notification glyph, channel title, download folder, About page, links | **done in Part 1** |
| Package id | `com.drdisagree.teledrive` | **done: `com.adnan.memoracloud`** |
| Android-only build system | Workflows also build Windows/Linux/macOS + a website deploy | **done: Android-only workflows** |
| In-app update download + checksum + signature verify + installer | ❌ missing (opens release page) | **Part 2** |
| Release metadata for the updater (`update.json` + SHA256SUMS) | ❌ | **done in Part 1 CI** |
| AI photo intelligence (objects, scenes, faces, semantic search) | ❌ nothing: no ML Kit / TFLite / ONNX dependency | **Part 3** |
| Face grouping + privacy controls | ❌ | **Part 3** |
| Dedupe hardening (cross-restart, cloud-side verification) | Mostly present; hash comparison is local-index first | **Part 2 review** |
| Restore wizard (choose destination, resume, skip duplicates) | Present but spread across Files/Trash/Transfers; no single "Restore" entry point | **Part 2** |
| Translations for new strings | Only the 6 upstream locales exist; new Memora strings fall back to English | documented |

---

## 4. Decisions taken for this fork

1. **Reuse, do not rewrite.** Every screen, worker, DAO and migration is kept. Only identity,
   build system and (in later parts) the two missing features are touched.
2. **Package rename is mechanical but complete**: `com.drdisagree.teledrive` → `com.adnan.memoracloud`
   across 691 Kotlin files, resources, manifest, proguard rules, Room schema directory, compose-resource
   package, notification action string and test helpers.
3. **Cloud protocol markers renamed** (`#teledrive-*` → `#memoracloud-*`, `memoracloud.index.json.gz`,
   …). The channel-title check still accepts the legacy `TeleDrive` title, so an inherited channel is
   adopted instead of duplicated.
4. **Database renamed** to `MemoraCloudDatabase` / `memoracloud.db` with the schema directory moved
   accordingly; all 17 exported schemas and the migration tests keep working because the version
   numbers and entity definitions are untouched.
5. **Desktop module is left in the tree** (shared code compiles it) but no CI job builds it, per the
   "remove unnecessary desktop build jobs" instruction. Deleting the module would force edits to the
   KMP source-set wiring of `:shared` and `:ui`, which is unnecessary risk for an Android product.
6. **Version reset** to `1.0.0` / `versionCode 1`, with CI able to override both from the tag so
   `versionCode` always increases.
