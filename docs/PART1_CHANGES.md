# Part 1 — Identity, build system, release plumbing

Status: **implemented in the source tree**. Part 1 does not add features; it turns upstream
TeleDrive into *your* app, with a build and release pipeline you can drive from a phone.

Nothing in Part 1 was compiled here — see "What was and was not verified" at the end.

---

## 1. What changed

### App identity
| Item | Before | Now |
| --- | --- | --- |
| Launcher label | `TeleDrive` | `Memora Cloud` (all 6 locales) |
| applicationId / namespace | `com.drdisagree.teledrive` | `com.adnan.memoracloud` |
| Kotlin package | `com.drdisagree.teledrive.**` | `com.adnan.memoracloud.**` (691 files, directories moved) |
| Compose resources package | `com.drdisagree.teledrive.resources` | `com.adnan.memoracloud.resources` |
| `Application` class | `TeleDriveApplication` | `MemoraCloudApplication` |
| Root Compose entry | `TeleDriveApp`, `TeleDriveTheme`, `TeleDriveTypography`, `TeleDriveShapes` | `MemoraCloudApp`, `MemoraCloudTheme`, `MemoraCloudTypography`, `MemoraCloudShapes` |
| Root project / APK name | `TeleDrive-1.5.0-universal-release.apk` | `MemoraCloud-1.0.0-universal-release.apk` |
| Room database | `TeleDriveDatabase` / `teledrive.db` / schema dir `…TeleDriveDatabase` | `MemoraCloudDatabase` / `memoracloud.db` / schema dir `…MemoraCloudDatabase` |
| Android theme | `Theme.TeleDrive` | `Theme.MemoraCloud` |
| Version | 1.5.0 (code 10) | **1.0.0 (code 1)**, overridable from CI |
| Notification “open” action | `com.drdisagree.teledrive.OPEN` | `com.adnan.memoracloud.OPEN` |
| Default download folder | `Downloads/TeleDrive` | `Downloads/MemoraCloud` |
| MediaStore folder | `TeleDrive` | `MemoraCloud` |

### Icons and visual identity
* New launcher icon (indigo→cyan cloud with an upload arrow), generated from a single master and
  exported at every density in the right shapes:
  `ic_launcher`, `ic_launcher_round`, `ic_launcher_foreground` (adaptive, art inside the 66 dp
  safe zone), `ic_launcher_monochrome` (themed icons), and a white `ic_notification` glyph for the
  status bar. The old TeleDrive `.webp` bitmaps were removed so resource names stay unique.

### Cloud-side markers (protocol strings stored in Telegram)
`#teledrive-storage`, `#teledrive-index`, `#teledrive-folders`, `#teledrive-keybackup`,
`#teledrive-icon`, `teledrive.index.json.gz`, `teledrive.folders.json`, `teledrive.keybackup`,
`teledrive.log` → the same names with `memoracloud`. Channels are now created with the title
**Memora Cloud** (plus an optional label). Discovery still accepts the legacy `TeleDrive` title so
an existing channel is adopted rather than duplicated.

### About screen and links
* Removed the upstream donation and Telegram-channel rows (they pointed at someone else's
  accounts and a donation page that is not yours).
* Added: “Developed by Adnan”, “Apache License 2.0 · based on TeleDrive, with attribution”, and an
  **Upstream project** row linking to the original repository (the attribution Apache 2.0 asks for).
* `AppLinks` now points at `github.com/Adnanh444/Tele-Cloud` for source, releases and issues.
* `UpdateChecker` now reads
  `https://api.github.com/repos/Adnanh444/Tele-Cloud/releases/latest` with the user agent
  `MemoraCloud`, so the update notification is about *your* releases.

### Build and release
* `gradle/libs.versions.toml`: `appVersion 1.0.0`, `appVersionCode 1`.
* `android/build.gradle.kts`: version can be overridden with `-PappVersionName=` and
  `-PappVersionCode=`; CI uses this so `versionCode` always increases without editing files.
* `.github/workflows/build.yml` — **Debug APK** on every push / PR / manual run. Android only.
  If the signing secrets exist the debug APK is signed with your permanent key (so it installs over
  a release build); if not, it uses a throwaway debug key and says so.
* `.github/workflows/release.yml` — **Signed release** on a `v*` tag or manual run:
  verifies the four secrets exist, decodes the keystore, builds
  `:android:assembleRelease -PsplitAbis=false`, **verifies the signature with `apksigner`**,
  produces `MemoraCloud-<version>.apk`, `SHA256SUMS.txt` and `update.json`
  (`version`, `versionCode`, `apk`, `sha256`, `minSdk`, `page`), and publishes a GitHub Release.
  `update.json` is the contract Part 2's in-app updater will consume.
* `.github/workflows/unpack-source.yml` — one-time mobile helper: upload the ZIP, run the workflow,
  it unpacks and commits the project for you.
* `.github/workflows/generate-keystore.yml` — one-time mobile helper that creates a 4096-bit RSA
  keystore, prints the four values for the secrets, and uploads the `.jks` as a 1-day artifact.
* Deleted: the Windows/Linux/macOS/AppImage jobs, the website deploy workflow (`pages.yml`), the
  upstream website folder, upstream screenshots, upstream issue templates and the Spanish copy of
  the upstream README.

### Documentation
`README.md` (rewritten), `NOTICE` (Apache 2.0 attribution), `CHANGELOG.md`,
`docs/ARCHITECTURE_AUDIT.md`, `docs/PART1_CHANGES.md` (this file),
`docs/PART2_CHANGES.md` (in-app updater),
`docs/FEATURE_STATUS.md`, `docs/GITHUB_MOBILE_SETUP.md`, `docs/SIGNING_AND_RELEASES.md`.

---

## 2. Things you must know

* **This is a fresh application id.** Android treats `com.adnan.memoracloud` as a different app
  from TeleDrive: it installs side by side, and it will not see TeleDrive's local database.
  Anything already uploaded by TeleDrive stays in the Telegram channel; if that channel is titled
  `TeleDrive` the app will adopt it and rebuild its index from the cloud markers.
* **Keep the same keystore forever.** A release signed with a different key cannot be installed
  over the previous one. The keystore is never in this archive — only in your GitHub secrets and
  your own backup.
* **Translations:** the new/changed English strings have no translations in `values-es`, `values-id`,
  `values-pt-rBR`, `values-ru`, `values-zh-rCN`. Compose resources fall back to English, so nothing
  breaks; the brand token was translated everywhere it existed.

## 3. What was and was not verified

Verified here, in this container:
* every `.kt`, `.kts`, `.xml`, `.toml`, `.yml`, `.pro` file scanned for leftover `com.drdisagree`,
  `TeleDrive`, `teledrive` identifiers (none left except the deliberate legacy-title list and the
  upstream attribution URL);
* all 691 package declarations and **all in-project imports resolve** to a declaration in the tree
  (custom checker, 0 unresolved);
* every `res/**` and `composeResources/**` XML file parses; no duplicate resource names remain;
* both workflows and the helper action parse as YAML;
* Room schema directory name matches the renamed database class; database `NAME` updated;
* icon set regenerated at 5 densities × 5 variants with no leftover `.webp` duplicates.

**Not verified here:** an actual Gradle/AGP compile. This sandbox has no Android SDK, JDK 11 only,
2 GB RAM and no network access to Google's Maven for a multi-gigabyte dependency graph. The first
real compile happens in GitHub Actions (see `docs/GITHUB_MOBILE_SETUP.md`); if anything fails there
the log points at a file, and it gets fixed in the next part. I will not claim the APK builds until
a green run exists.
