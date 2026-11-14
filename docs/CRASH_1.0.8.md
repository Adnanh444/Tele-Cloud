# What crashed 1.0.8

Reported: after installing 1.0.8 over 1.0.6, the app would not open.

## Most likely cause

1.0.6 shipped an experimental photo tagger and enqueued it at every start:

```kotlin
if (prefs.aiLabelsEnabled) photoTagScheduler.enqueue()   // unique work "photo-labels"
```

WorkManager keeps its own database of unfinished jobs. 1.0.8 deleted `PhotoLabelWorker` entirely,
but any job still unfinished at update time stayed in WorkManager's database. On start-up:

1. `androidx.startup` initialises WorkManager — **before** `Application.onCreate`.
2. WorkManager re-creates each unfinished job by **class name**.
3. `com.adnan.memoracloud.core.ai.PhotoLabelWorker` no longer exists → `ClassNotFoundException`.
4. The exception happens inside provider initialisation, so nothing in the app can catch it and the
   process dies immediately — exactly "app crashed, not opening".

## Fix in 1.0.9

`core/compat/LegacyWorkers.kt` reintroduces both old class names as do-nothing `Worker`s returning
`Result.success()`, registered in Koin as `legacyWorkersModule`. The leftover job now runs once, does
nothing, and is pruned. They can be deleted several versions later.

## Diagnostics added, because a phone has no logcat

`CrashHandler` (installed from `attachBaseContext`, which runs before providers) writes the next
uncaught exception to:

* `files/crash/last.txt` — shown in a dialog on the next successful start (selectable, so it can be
  copied).
* `Downloads/MemoraCloud/memora-crash.txt` — openable from any file manager **even if the app never
  starts again**.

## If 1.0.9 still refuses to open

1. Look in `Downloads/MemoraCloud/memora-crash.txt` and read the first `Caused by:` line.
2. The other candidate is the Room migration (v18 → v19 drops the AI tables) failing schema
   validation. The message would name a table.
3. Last resort that keeps the backup safe: Android Settings → Apps → Memora Cloud → Storage →
   Clear storage. The index and login are rebuilt from Telegram afterwards; the photos are not
   touched.
