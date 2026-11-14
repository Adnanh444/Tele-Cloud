# Progress — what is done, what is next

Last updated after Part 3c. "Written" means the code is in `MemoraCloud-source.zip` and reviewed
statically (imports, resource keys, XML, brace balance) — it is **not** called "tested" until it has
run on the phone, because there is no Android SDK in this workspace.

| Part | Scope | State |
| --- | --- | --- |
| 1 | Rebrand to Memora Cloud, `com.adnan.memoracloud`, icons, Gradle, CI workflows, keystore + signing, first signed arm64 release, docs | ✅ **verified** — you installed the signed APK and used the app |
| 2 | In-app updater: check → download → SHA-256 + signing-certificate verify → system installer, progress/cancel/retry, "update ready" notification | ✅ written · ⏳ your device test is pending (you deferred it) |
| 2.1 | Updater diagnostics: every check failure now has a reason, restored install state after process death, stale-APK cleanup, install logging | ✅ written · ⏳ ships with the next release |
| 3a | Automatic backup-folder detection from the device (MediaStore + fallback walk), **Search again**, new folders appear on their own | ✅ written · ⏳ device test pending |
| 3c | "What to back up" switches (photos, videos, audio, documents, archives, other) + consent dialog before *Delete local copy after backup* | ✅ written · ⏳ device test pending |
| 3b | On-device AI: image labels, face detection/grouping, semantic search, background worker, opt-out, manifest storage | ⏳ **not started** — next big piece |
| 4 | "Restore everything" wizard with destination-folder picker; duplicate-protection hardening + tests; restore/backup status polish | ⏳ not started |
| 5 | Final QA pass, honest feature list, mobile upload guide refresh | ⏳ not started |

## Queued on request — graceful upload pause

Requested behaviour: *pausing must not cut a file mid-upload; the running file finishes, then the
queue pauses.*

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
