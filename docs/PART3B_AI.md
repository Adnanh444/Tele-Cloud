# Part 3b — on-device photo intelligence (slice 1: tags + tag search)

Goal: the library should be searchable by *what is in* a photo, not just by file name — computed on
the phone, with nothing sent anywhere.

This slice delivers the tagging engine, storage, the setting and the search integration. Face
grouping and natural-language (embedding) search are **not** in this slice; see the end of the file.

## What runs

1. `PhotoLabelWorker` (WorkManager, unique work `photo-labels`) asks the repository for a batch of
   local images that have no tags yet: `category = 'IMAGE'`, not trashed, `localPath` present.
2. For each file `MlKitImageLabeler` decodes it and runs the bundled on-device model
   (`com.google.mlkit:image-labeling:17.0.9`), keeping labels above **0.55** confidence.
3. Labels are lowercased, de-duplicated, sorted by confidence, capped at 8, and written to the new
   `photo_labels` table with the top label and its confidence.
4. **A photo the model cannot read still gets a row with no labels**, which is what stops it being
   retried forever.
5. The worker stops after 60 photos per run (12 per batch), and only runs when the user has the
   feature switched on and the battery is not low.

## Where the user sees it

* **Settings → Backup → Photo intelligence** — on/off. Off cancels the work and nothing is analysed.
* **Settings → Backup → Tag photos now** — queues a run; a toast confirms it is running in the
  background.
* **Search** — the same search box now matches tags as well as names:
  `AND (name LIKE ? OR id IN (SELECT fileId FROM photo_labels WHERE labels LIKE ?))`.
  So typing `food`, `dog`, `screenshot`, `document`, `night` finds photos whose file names are
  meaningless (`IMG_2481.jpg`).

## Storage and migrations

* New table `photo_labels` (`fileId` PK, `labels` comma-separated lowercase, `primaryLabel`,
  `confidence`, `updatedAt`).
* Database version **17 → 18**, migration `MIGRATION_17_18` creates the table; registered in
  `DatabaseModule`. No existing table is touched, so an upgrade cannot lose data.
* Tags are **local only** for now. They are not yet written into the cloud manifest, so a reinstall
  re-tags from scratch (the files themselves are restored from Telegram as before). That is a known
  gap, not a silent one — see below.

## Honest limits of this slice

| Item | State |
| --- | --- |
| Tagging engine, storage, migration, worker, setting | ✅ done |
| Search by tag | ✅ done |
| Tags shown in the photo viewer / gallery chips | ❌ not yet — next slice |
| Tags stored in the cloud manifest (survive reinstall) | ❌ not yet — next slice |
| Face detection and grouping ("Person 1") | ❌ not started |
| Natural-language / semantic search | ❌ not started |
| APK size | +model, expected **≈ 30–33 MB** (measured on the first CI run, not before) |
| Accuracy | generic ImageNet-style labels; it will say "photograph" for a lot of things, and it will not know your pet's name |

## Privacy

* No network call exists in the tagging path — `MlKitImageLabeler` only touches the local file.
* The model is bundled in the app, not downloaded at runtime.
* Face data: not collected in this slice at all (no face model is bundled).
* Switch it off and the worker cancels itself and deletes nothing (tags already stored stay until
  **Tag photos now** is never run again, or the app data is cleared).

## Next slices

1. **Tags in the UI**: chips under the preview, a "Tags" filter in Search, tag cloud from
   `labelInventory()`.
2. **Tags in the manifest**: publish them next to the file caption so a reinstall keeps them.
3. **Faces**: face detection + embeddings, grouped locally, names stored only in the encrypted
   local DB (+ optional manifest entry).
4. **Semantic search**: on-device embeddings + a local vector index.
