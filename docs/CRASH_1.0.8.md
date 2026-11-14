# The 1.0.8 launch crash — confirmed cause and fix

Reported: after updating from 1.0.6 to 1.0.8 the app closed immediately, and 1.0.9 did the same.

## The crash, from `Downloads/MemoraCloud/memora-crash.txt`

```
java.lang.IllegalStateException: Room cannot verify the data integrity. Looks like you've changed
schema but forgot to update the version number. You can simply fix this by increasing the version
number. Expected identity hash: f1470a9f712bf49f5f13ca835976dab3,
found: 6a36037306ccfd53f425aa7e340b179c
```

Room says exactly what was wrong, and it was right: **1.0.8 removed database tables without bumping
the database version number.**

* 1.0.6 shipped database **version 19**, whose schema contained the AI tables `photo_labels`
  (photo tagger) and `faces`, `people`, `face_scans` (face groups).
* 1.0.8 deleted those entities but left `version = 19`.
* On an upgraded install the file on disk is still version 19, so Room runs **no migration** — it
  only validates the stored schema against the current one, and the identity hash cannot match.
* The exception is thrown while opening the database, on a background dispatcher, every launch.

A fresh install was never affected, which is why the build looked fine.

## Fix

Version **20**, with a real migration step:

```kotlin
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `photo_labels`")
        connection.execSQL("DROP TABLE IF EXISTS `faces`")
        connection.execSQL("DROP TABLE IF EXISTS `people`")
        connection.execSQL("DROP TABLE IF EXISTS `face_scans`")
    }
}
```

`MIGRATION_17_19` and `MIGRATION_18_19` stay so older installs still have a path
(17 → 19 → 20, 18 → 19 → 20); Room validates only the final schema, so the intermediate no-op is
harmless. **The file index, settings and login survive** — no rebuild needed.

## Why 1.0.8/1.0.9 diagnostics took this long

The original suspicion was WorkManager re-creating a job for the deleted `PhotoLabelWorker`. That was
wrong, and the empty stand-ins (now in their original packages, with `-keepnames`) are only belt and
braces. What settled it was writing the stack trace to `Downloads/MemoraCloud/memora-crash.txt` from
a content provider declared first in the manifest, so even a crash during start-up is recorded.

## Last-resort net

`CrashHandler` now recognises this exact failure. If *any* future build ships a schema change without
a version bump, it deletes `memoracloud.db` and notes it in the crash file instead of bricking the
app forever; the next launch opens with an empty index, rebuilt from Telegram by "Rebuild index".

## Standing rule

Any change to `@Entity` classes requires `version = N + 1` **and** a migration in the same commit.
`Migrations.kt` carries a comment saying so; a schema change that only drops tables is still a
version step.
