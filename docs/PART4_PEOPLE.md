# Part 4 — People (on-device face grouping)

Replaces the photo tagging attempt from 1.0.6. The user's verdict on tags was that the search box
was not the feature they wanted; finding *a specific person's photos* was. So tagging is gone and
People is here.

## What the user gets

* **Gallery → People strip** — a row of round avatars above the grid. Tap a face and the grid shows
  only that person's photos; tap the same avatar again to clear it.
* **Settings → Backup → People** — on/off switch (default on), "Scan photos now", and the live
  counts (`12 people · 340 faces · 1,208 photos scanned`).
* **Settings → Backup → Forget faces** — deletes every signature and group on the device (with a
  confirm dialog). Photos and the Telegram backup are not touched.

## How it works

1. `FaceScanWorker` (WorkManager, unique work `face-scan`, battery-not-low) picks local photos that
   no scan has looked at yet: `category='IMAGE'`, not trashed, `localPath` present, no row in
   `face_scans`.
2. `FaceAnalyzer` decodes the photo (downsampled to 1600 px), runs **ML Kit face detection**
   (bundled model, accurate mode, min face size 6 %), keeps the 8 largest faces.
3. Each face is cropped — a square centred on the eyes (that is what the model was trained on),
   falling back to an expanded box when landmarks are missing — scaled to 112×112, normalised as
   `(pixel − 127.5) / 128`, and turned into a 192-float signature by the bundled
   **MobileFaceNet** model (`assets/models/MobileFaceNet.tflite`, MIT, 5.2 MB, two faces per run).
4. `PeopleRepositoryImpl` compares the signature to stored signatures (up to 600 most recent). If
   the best cosine similarity is **≥ 0.62** the face joins that person, otherwise a new group
   `Person N` is created. Unknown faces inside one photo are also compared against each other, so
   twins in one picture do not create two groups.
5. Faces in one photo that all fail the threshold create one group per face — expected: name them
   later, or forget and rescan.
6. A photo with **no** faces still gets a `face_scans` row, so it is never decoded twice. A file that
   fails to decode also gets one, and is skipped from then on.

## Storage

| Table | Holds |
| --- | --- |
| `people` | id, name, cover crop path, created at |
| `faces` | one row per face: 192 floats (BLOB), crop path, box, person id; indexed by `fileId` and `personId` |
| `face_scans` | one row per photo looked at, with how many faces it had |

Database version **18 → 19**. Migrations `MIGRATION_18_19` (drops the abandoned `photo_labels`
table, creates the three new tables) and `MIGRATION_17_19` (straight upgrade for anyone still on
1.0.5 or older). No existing table is altered.

Face crops are JPEGs in app storage (`filesDir/faces`), one per face, pruned at the start of every
run when their face row is gone, and deleted on logout together with the rest of the local data.

## Privacy

* No network code in this path. The detector and the embedding model are both inside the APK.
* Face signatures never leave the phone and are **not** written to the Telegram manifest.
* `Forget faces` removes all of it; uninstalling does the same.

## Honest limits

| Item | State |
| --- | --- |
| Detection, signatures, grouping, People strip, filter, scan-once marker | ✅ implemented |
| Renaming a person | ❌ not yet — groups are called `Person 1`, `Person 2`. Next slice. |
| Merging two groups that are really one person | ❌ not yet |
| Photos without a device copy (cloud-only) | ❌ skipped — they must be downloaded first |
| Preview swipe while filtered | shows all media, not only that person (known limitation) |
| Accuracy | real but not magic: side profiles, sunglasses, small faces and children at different ages separate into extra groups; **0.62** is tuned to prefer splitting over wrongly merging |
| Speed | roughly 1–2 photos/second on a modern phone; a 5,000-photo library takes a while — it runs in the background, 40 photos per run |
| APK size | +~14 MB (face detector ~7 MB, TFLite ~2 MB, model 5.2 MB) minus the 5.7 MB tag model that was removed; `legacyJniPackaging=true` now compresses the native libraries, so the download should *drop* compared with 1.0.6 |

## Testing it

1. Settings → Backup → People is on; tap **Scan photos now** (or just open the app and wait).
2. Give it 10–20 minutes on a few hundred photos, then open Gallery.
3. A **People** row appears above the grid with one avatar per group. Tap one → only that person's
   photos. Tap again → back to everything.
4. Counts in Settings should climb: `N people · M faces · K photos scanned`.
5. If two groups are the same person, that is the known splitting behaviour — merging lands in the
   next slice, together with renaming.
