# Progress — what is done, what is next

Last updated after Part 3b slice 1 (AI tagging). "Written" means the code is in `MemoraCloud-source.zip` and reviewed
statically (imports, resource keys, XML, brace balance) — it is **not** called "tested" until it has
run on the phone, because there is no Android SDK in this workspace.

| Part | Scope | State |
| --- | --- | --- |
| 1 | Rebrand to Memora Cloud, `com.adnan.memoracloud`, icons, Gradle, CI workflows, keystore + signing, first signed arm64 release, docs | ✅ **verified** — you installed the signed APK and used the app |
| 2 | In-app updater: check → download → SHA-256 + signing-certificate verify → system installer, progress/cancel/retry, "update ready" notification | ✅ written · ⏳ your device test is pending (you deferred it) |
| 2.1 | Updater diagnostics: every check failure now has a reason, restored install state after process death, stale-APK cleanup, install logging | ✅ written · ⏳ ships with the next release |
| 3a | Automatic backup-folder detection from the device (MediaStore + fallback walk), **Search again**, new folders appear on their own | ✅ written · ⏳ device test pending |
| 3c | "What to back up" switches (photos, videos, audio, documents, archives, other) + consent dialog before *Delete local copy after backup* | ✅ written · ⏳ device test pending |
| 3b | Photo tags + tag search (1.0.6) | ❌ **removed on request** — user wants People, not search-by-tag. `photo_labels` is dropped in the v18→v19 migration |
| 4a | People: on-device face detection + 192-float signatures + grouping, People strip in Gallery, person filter, Settings switch/counts/forget | ✅ written · ⏳ device test (`docs/PART4_PEOPLE.md`) |
| 4b | Rename a person, merge two groups, semantic search | ⏳ next |
| 3d | Graceful upload pause (finish current file, then stop) | ✅ written · optional, you skipped building it |
| 4 | "Restore everything" wizard with destination-folder picker; duplicate-protection hardening + tests; restore/backup status polish | ⏳ not started |
| 5 | Final QA pass, honest feature list, mobile upload guide refresh | ⏳ not started |

## Graceful upload pause — DONE (Part 3d)

Requested behaviour: *pausing must not cut a file mid-upload; the running file finishes, then the
queue pauses.* Implemented as described in the plan below:

* `TransferDao.pauseQueued()` pauses only `QUEUED` rows; the `RUNNING` file is untouched and runs to
  completion.
* `UserPreferences.queuePaused` (DataStore `queue_paused`) is the queue gate, checked in
  `TransferQueueDrainer` **between** items, never mid-transfer.
* While paused, anything enqueued (instant backup, manual upload, restore) lands as `PAUSED` so it
  is visible in the Paused section instead of silently starting.
* Resuming one item, retrying, Resuming all, or Cancel-all lifts the pause; the Play button in the
  Transfers toolbar does a full resume.
* UI: the toolbar **Pause** button = *pause after this file*; overflow menu → **Pause now (stops the
  current file)** = the old immediate behaviour, for metered-data emergencies.

### Original plan (kept for reference)

Today (verified in code):

* `pauseAll()` flips both `QUEUED` and `RUNNING` to `PAUSED`; the executor notices at the next
  progress tick (`checkControl`) and aborts that transfer.
* **No corrupt file can reach Telegram either way**: the message is only sent when the upload
  completes (`TelegramUploadEvent.Completed` / `PartUploadEvent.Completed`). An aborted attempt is
  retried from scratch for normal files, and for split large files the already-sealed parts are
  kept (`discardParts` is only called on *cancel*, not on pause).
* So the requested change is about **not wasting work and not being abrupt**, not about corruption.

Planned implementation (agreed, not yet written):

1. `TransferDao`: new query pausing only `WHERE state = 'QUEUED'` — the running file is untouched.
2. `TransferRepositoryImpl.pauseAll(graceful = true)`: graceful = the query above, plus a
   "pause when idle" flag so the worker does not start the next queued item; immediate =
   today's behaviour.
3. When the in-flight transfer completes, the executor sees the flag and stops before the next item.
4. UI: **Pause** = graceful (finish current file, then pause) with a one-line hint; **Pause now**
   (immediate) moved into the same button's menu for emergencies.
5. Resume is unchanged: `resumeAll()` requeues everything and the worker kicks.

## Honest gaps that are still open

* No "Restore everything" wizard — restore works per file/folder from Files/Transfers/Trash.
* Duplicate protection is implemented (SHA-256 + records) but has no unit tests of its own.
* AI is the one large brief item with zero code so far.
* The updater has never executed on a real device.
