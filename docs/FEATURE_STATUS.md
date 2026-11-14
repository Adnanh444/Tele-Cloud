# Memora Cloud — feature status

Honest state of every requirement in the brief, mapped to the code that implements it.
Legend: **✅ inherited & working** (upstream code, rebranded, kept) · **🆕 added in this fork** ·
**🚧 planned, not yet written** · **❌ not possible / not provided**

Last updated: Part 4 (restore wizard; AI removed).

---

## 1. App identity
| Requirement | State | Where |
| --- | --- | --- |
| Name, developer, package id, launcher label | ✅🆕 | `strings.xml`, `android/build.gradle.kts`, `com.adnan.memoracloud` |
| Memora Cloud branding across UI, notifications, About, settings | ✅🆕 | all resource files + `AboutSection.kt` |
| New icons / notification glyph | 🆕 | `res/mipmap-*`, `res/drawable-*/ic_notification.png` |
| Apache 2.0 notices preserved | 🆕 | `LICENSE` untouched, new `NOTICE`, About attribution row |
| Telegram only | ✅ | no Drive/Firebase/AWS dependency exists in the dependency graph |

## 2. Telegram connection and channel management
| Requirement | State | Where |
| --- | --- | --- |
| Sign in with the user's own Telegram account | ✅ | `TdLibTelegramClient`, onboarding screens |
| Select or create a private backup channel | ✅ | `ChannelRepositoryImpl`, `channels/` screens |
| Add / change / remove channels | ✅ | `StorageChannelDao`, `SettingsSectionScreen` |
| Credentials stored securely | ✅ | `KeystoreManager`, encrypted `api_hash`, TDLib session in app-private storage |
| Re-detect the channel after a reinstall | ✅ | title search + `#memoracloud-storage` marker adoption |
| Recovery with same account + channel | ✅ | cloud manifest + index snapshot rebuild |
| Never delete backup files during normal sync | ✅ | deletion is explicit and goes through Trash → `PendingDeleteEntity` |
| No separate Memora login | ✅ | Telegram credentials only |

## 3. Automatic backup
| Requirement | State | Where |
| --- | --- | --- |
| Detect new files, instant backup | ✅ | `MediaStoreWatcher`, `MediaWatchWorker`, `MediaSweepWorker` |
| Photos, videos, documents, audio, other | ✅ | `FileCategory`, mime mapping |
| Per-type on/off (photos, videos, audio, documents, archives, other) | 🆕 *Settings → Backup → What to back up* | `backupCategories`, filtered pre-hash |
| Per-folder backup on/off | ✅ | `backupFolders` + `ExclusionDao` (`ExclusionsScreen`) |
| Folders detected automatically from the device (incl. new ones) | 🆕 `MediaStoreFolderScanner` → *Settings → Backup → Detected folders* | `docs/PART3_CHANGES.md` |
| Scheduled / background backup | ✅ | `ScheduledBackupWorker` + `backupIntervalHours` |
| WorkManager based | ✅ | all workers registered through Koin-WorkManager |
| Resume interrupted uploads | ✅ | `FilePartEntity`, `PartUploader`, `BackupResumeWorker` |
| Retry failed uploads | ✅ | `transferRetryCount`, `TransferQueueWorker` |
| Upload progress + statuses (pending/uploading/uploaded/failed) | ✅ | `TransfersScreen`, `BackupSessionTracker`, notifications |
| No repeated uploads of the same file | ✅ | `BackupRecordEntity` + dedupe decision |
| Mobile data on/off, Wi-Fi only, charging only | ✅ | `allowMeteredTransfers`, `backupWifiOnly`, `backupChargingOnly` |
| Never delete phone originals automatically | ✅ | `deleteAfterUpload` is off by default and opt-in |

## 4. Duplicate protection (SHA-256)
| Requirement | State | Where |
| --- | --- | --- |
| Hash before upload | ✅ | `HashAccumulator` in `PartUploader` / `TransferExecutor` |
| Compare against the index before uploading | ✅ | `DecideBackupActionUseCase`, `BackupRepositoryImpl` |
| Skip identical files | ✅ | `BackupDecision.SKIP_UNCHANGED` |
| Detect duplicates after a rename | ✅ | `FileDao.liveMatchesBySize()` then hash compare — never name-based |
| Not "same name + size = duplicate" | ✅ | hash is authoritative; size only shortlists candidates |
| Survive app restart | ✅ | hash + record persisted in Room and in the caption manifest |
| Cloud-side manifest | ✅ | `RemoteFileManifest` in every caption, plus index snapshot |
| Verify completion before marking backed up | ✅ | state only flips after `PartUploader.Completed` + `FileManifestPublisher` |
| Hardening pass (new spec + tests) | 🚧 | Part 2 review |

## 5. Cloud browser
| Requirement | State | Where |
| --- | --- | --- |
| Gallery of backed-up photos/videos | ✅ | `gallery/` |
| Documents and other files | ✅ | `files/`, `collection/`, `search/` |
| Organise by type and date | ✅ | `FileCategory`, albums, sort fields |
| Search and sort | ✅ | `FileQuerySpec`, `search/` |
| Name, size, original date, upload date | ✅ | file detail sheet |
| Thumbnails | ✅ | `ThumbnailDao`, Coil |
| Preview without full download | ✅ | `TelegramDataSource`, Coil, PDF/text/archive viewers |
| Stream video | ✅ | Media3 + `PartedTelegramDataSource` |
| Download one / many | ✅ | `TransferRepositoryImpl`, bulk selection |
| Restore folders or everything | ✅ (spread across Files/Transfers/Trash) | see gap below |
| Progress + resume for downloads | ✅ | `PartDownloader`, `CacheEntryEntity` |
| Distinguish cloud vs local | ✅ | availability badges |
| Confirmation before deleting, deletes the Telegram message | ✅ | trash confirm → `PendingDeleteEntity` |

Gap: there is **no single "Restore everything" wizard** with a destination-folder picker; the
pieces exist (bulk download, `MakeAvailableOfflineUseCase`, MediaStore writer). Planned for Part 2.

## 6. Restore and recovery
| Requirement | State | Where |
| --- | --- | --- |
| Recover with Telegram account + channel | ✅ | storage-channel adoption + index rebuild |
| Rebuild index if local data is lost | ✅ | `SyncRepositoryImpl`, `IndexSnapshotFile`, `FolderStateSynchronizer` |
| Recover names, metadata, folder structure, hashes | ✅ | caption manifests + snapshots |
| Restore individual / selected / all | ✅ / ✅ / 🚧 (wizard) | `transfers/`, `TrashScreen` |
| Choose destination folder | ✅ | `downloadDirectory` setting + SAF picker |
| Preserve names and metadata | ✅ | `MediaStoreDownloadWriter` |
| Resume interrupted restores | ✅ | part cache + resume worker |
| Avoid duplicate downloads | ✅ | `CacheEntryEntity` + `isAvailableOffline` |

## 7. AI photo intelligence
| Requirement | State |
| --- | --- |
| Object / scene recognition, categorisation, semantic search | 🚧 **Part 3** — no ML code exists upstream and none is added yet |
| Face detection and face groups | 🚧 Part 3 (opt-in, on-device only) |
| Background processing | 🚧 Part 3 (WorkManager worker) |
| Recoverable AI metadata in the cloud manifest | 🚧 Part 3 (`extra` field of the manifest already exists for this) |
| Disable AI | 🚧 Part 3 setting |
| No third-party AI upload | by design: on-device ML Kit/TFLite only |

There is **no fake AI UI** in this build: nothing is shown that does not work.

## 8. Settings
All requested settings already exist and are wired: Telegram account/channel, auto backup, folder
selection, mobile data, Wi-Fi only, charging only, interval schedule, notifications, storage & cache
(`CacheCleanupWorker`), restore/recovery (index rebuild, key restore), update checking.
Duplicate protection toggle and AI toggle are 🚧 Part 2 / Part 3 additions.

## 9. In-app updates
| Requirement | State |
| --- | --- |
| Check GitHub Releases for `Adnanh444/Tele-Cloud` | ✅ (`UpdateChecker`, reads the `update.json` asset released by CI) |
| Show version + notes | ✅ (`UpdateDialog`, notes rendered as Markdown) |
| Download the APK inside the app | ✅ (`ApkUpdateInstaller` + `UpdateDownloadWorker`, resumable, retried 3x, survives leaving the app) |
| Verify SHA-256 + signing certificate | ✅ hash from `update.json`; signer digest compared with the installed package before the installer is offered |
| Install through Android's package installer, no silent install | ✅ `FileProvider` + `ACTION_VIEW`, with the "install unknown apps" permission flow. Silent install is impossible without a privileged installer and is not claimed |
| Progress + cancel + retry + honest failure messages | ✅ (`UpdateInstallState`) |
| Notification when the APK is ready | ✅ `notifyUpdateReady` on the existing *Updates* channel |
| Data preserved by keeping id + signing key, increasing versionCode | ✅ CI (`versionCode = 1000 + run number`) |
| No secrets in the repo | ✅ |

## 10. UI/UX
| Requirement | State |
| --- | --- |
| Light/dark, dynamic colour | ✅ (theme settings) |
| Responsive/adaptive layouts | ✅ (Material3 adaptive navigation) |
| Empty/loading/error states | ✅ (skeleton components, error strings) |
| Confirmation before destructive actions | ✅ |
| Backup status at a glance | ✅ (home + transfers) |

## 11. Build system
✅ Android-only workflows, signed releases, GitHub Release with checksums, documented secrets,
mobile upload helper, version override, no desktop jobs.

---

## Not implemented, and why
* **Part 2 (done):** in-app APK download/verify/install, see `docs/PART2_CHANGES.md`.
* **Remaining polish:** "Restore everything" wizard, duplicate-protection hardening + tests,
  restore/backup status polish.
* **Part 3 — folder auto-detection: done**; **per-type switches + delete consent: done**
  (`docs/PART3_CHANGES.md`).
* **On-device AI: removed.** Photo tags (1.0.6) and face grouping (unpublished 1.0.7) were both
  dropped at the user's request — the app is meant to stay light and simple. ML Kit, TensorFlow Lite
  and the 5 MB model are out of the APK, and the tables are dropped by the v18→v19 migration.
* **Part 4 — Restore from Telegram:** one place to bring the backup back, with scope, live counts
  and "skip files already on this device" (`docs/PART4_RESTORE.md`).
* **Desktop apps:** the module is still in the tree but no longer built or supported here.
* **Localisation of new strings** beyond English.
* **Silent self-update:** not possible without system privileges; Android will always show the
  install dialog. The app will state this instead of pretending otherwise.
