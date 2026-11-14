# Part 3a — automatic backup-folder detection

Before this, *Settings → Backup → Backup folders* showed five fixed guesses (`DCIM`, `Pictures`,
`Movies`, `Downloads`, `Documents`) plus whatever you added by hand with the system folder picker.
Folders your phone created later — a new camera app's folder, a chat app's image folder, an SD-card
directory — never appeared at all.

Now the app asks the device what is actually there and lists it.

## What the user sees

*Settings → Backup → Backup folders*:

```
Camera                      DCIM                       [ on ]
Pictures                    Pictures                   [ off ]
...
Detected folders (7)        Look for folders created after install
WhatsApp Images             412 photos · 0 videos · /storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images   [ off ]
Screenshots                 88 photos · 9 videos  · /storage/emulated/0/Pictures/Screenshots   [ on ]
Download                    31 photos · 4 videos  · /storage/emulated/0/Download            [ off ]
...
Add another folder          Pick a folder manually
Search again                Look for folders created after install
```

* Each row is a real switch that writes into the same `backupFolders` selection the backup scan
  already walks, so ticking one needs no conversion step.
* Anything already ticked stays visible and ticked, so it can be unticked again.
* The list is rescanned every time the screen is opened and whenever **Search again** is tapped, so a
  folder created after install shows up the next time you look — no reinstall, no reboot, no
  restart of the app.
* Folders that only hold documents or audio do **not** appear here (they contain no photos or
  videos). `Downloads` and `Documents` are still offered in the fixed list above, and anything else
  can be added with **Add another folder**.

## How it works

`MediaStoreFolderScanner` (`android/src/main/java/com/adnan/memoracloud/core/files/`):

1. Queries `MediaStore.Images` and `MediaStore.Video` for `_data` + `_size` of every row with a
   non-zero size, and groups rows by their parent folder.
2. Falls back to a bounded file-system walk (depth ≤ 6, skipping `Android/` and dot-folders, matching
   by image/video extension) when MediaStore answers with nothing — that happens when the media
   permission is not granted yet, or on ROMs whose media index is empty.
3. Returns the folders sorted by item count, so the folder with all your photos is first.

The UI side is a platform interface, `DeviceFolderScanner` + `LocalDeviceFolderScanner`
(`ui/src/jvmCommonMain/.../platform/DeviceFolderScanner.kt`), so the shared settings screen stays
platform-free: on desktop the local is null and the list behaves exactly as before.

## Honest limits

* **Media-only.** Folders holding just PDFs/ZIPs/music are not detected; use **Add another folder**.
* **Permission-dependent.** With Android 14's *partial* photo access, MediaStore only reports the
  items the user selected for the app. If the row is empty, the hint says how to fix it.
* **Counts, not bytes.** Sizes are summed from MediaStore but not displayed yet; the backup size
  limit is unaffected.
* **SD cards.** MediaStore reports the SD card's own volume path, and the backup scan can read it
  only when *All files access* is granted (the app already asks for it for other reasons).
* A folder ticked inside an already-ticked parent is harmless but the scan will see those files
  twice; content hashing means the second copy is recognised and not uploaded twice.

## Files touched

| File | Change |
| --- | --- |
| `android/…/core/files/MediaStoreFolderScanner.kt` | new — MediaStore query + file-system fallback |
| `ui/…/presentation/platform/DeviceFolderScanner.kt` | new — `DeviceFolder`, `DeviceFolderScanner`, `LocalDeviceFolderScanner` |
| `android/…/core/files/FilesModule.kt` | registers the scanner for Koin |
| `android/…/presentation/platform/AndroidPlatformProviders.kt` | provides `LocalDeviceFolderScanner` |
| `ui/…/settings/SettingsSectionScreen.kt` | folder list rebuilt around detected folders + **Search again** |
| `ui/…/composeResources/values/strings.xml` | 6 strings |

Nothing in the backup engine, database schema or Telegram layer was touched — the selection format
is unchanged.
