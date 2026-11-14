# Part 4 — Restore from Telegram

The backup half of the app was always the strong side; getting files *back* was scattered across
Files, Transfers and Trash with no single answer to "put my library back on this phone". This part
adds that one place.

## What the user gets

**Settings → Library → Restore from Telegram** opens a small wizard:

| Control | What it does |
| --- | --- |
| Everything / Photos and videos / Documents / Audio | chooses what to bring back |
| Skip files already on this device (on by default) | the no-duplicate-downloads rule |
| live summary | `1,204 files · 3.2 GB · 980 not on this device (2.7 GB)` |
| Restore | queues the downloads and says `980 files queued — follow them in Transfers` |

The counts come from the local index (`SELECT id … WHERE backupState = 'BACKED_UP'`), so opening the
wizard downloads nothing. Only rows that actually have a Telegram message (`remoteFileId != null`)
are considered — a stub row can never be restored, and is silently skipped instead of failing.

## How it fits with the rest

* Downloads are ordinary transfers: they appear in **Transfers**, follow the queue rules (Wi-Fi
  only, pause/resume, retry), and land in `Downloads/MemoraCloud`, mirroring the folder structure
  they were backed up with.
* If the index is empty — fresh install, new phone — **Rebuild index** in the same Settings section
  runs first, then the wizard has something to count.
* Nothing is deleted, moved or overwritten on the device: an existing file with the same name is
  skipped by the "skip files already here" rule, not replaced.

## Honest limits

| Item | State |
| --- | --- |
| Restore everything / by type, skip existing, live counts, queueing | ✅ implemented |
| Progress per file, pause/resume, retry | ✅ existing Transfers machinery |
| Choosing an arbitrary device folder | ❌ not yet — Android scoped storage needs a SAF picker; files go to `Downloads/MemoraCloud` (mirrored folders) |
| Restoring a *single* file or folder | ✅ already existed (Files / Gallery / Album → Download) |
| Resuming a half-finished restore after a reboot | ✅ transfers are recovered on start (`recoverOrphanedTransfers`) |
| Duplicate protection | SHA-256 at upload time; the wizard adds "already on device" skipping at restore time. Still no unit tests of its own. |

## What is next (Part 5)

Final QA pass, the honest feature list, the refreshed mobile upload guide, and the credits cleanup.
