# Part 2 — in-app updates

The app now updates itself from its own GitHub Releases page: it finds the newest release, reads the
`update.json` the release workflow publishes, downloads the arm64 APK in the background, checks it,
and hands it to Android's package installer.

Nothing here downloads from anywhere except `github.com/Adnanh444/Tele-Cloud`. There is no update
server, no analytics ping, and no secret in the repository.

## Files added

| File | Why |
| --- | --- |
| `shared/.../core/update/AppRelease.kt` | Release model the UI sees: version, notes, page, APK url/name, `sha256`, `versionCode`, size. `canInstallInApp` tells the UI whether to offer in-app install or just the release page. |
| `shared/.../core/update/ReleaseManifest.kt` | `update.json` (`version`, `versionCode`, `apk`, `sha256`, `minSdk`) — the contract between CI and the updater. |
| `ui/.../presentation/platform/UpdateInstaller.kt` | Platform-free `UpdateInstaller` interface, `LocalUpdateInstaller`, `UpdateInstallState` (`Idle`, `Downloading`, `Verifying`, `ReadyToInstall`, `Failed`) and `UpdateFailure`. |
| `android/.../core/update/ApkUpdateInstaller.kt` | The real work: resumable download to `cache/updates`, SHA-256 check, signer-certificate check, `FileProvider` + installer intent, notification. |
| `android/.../core/update/UpdateDownloadWorker.kt` | Runs the download as WorkManager work, so it continues when the user leaves the dialog. |

## Files changed

| File | Change |
| --- | --- |
| `shared/.../core/update/GitHubRelease.kt` | Added `assets` (`name`, `browser_download_url`, `size`) so the updater can find the APK and `update.json`. |
| `shared/.../core/update/UpdateChecker.kt` | Reads `update.json` from the release assets, picks the APK it names (or the first `.apk`), returns a full `AppRelease`. Still tolerant: a release without `update.json` still offers the download, without a published hash. |
| `ui/.../components/UpdateDialog.kt` | New signature with `installState` + `inAppInstallSupported`; progress bar with percentage, "Verifying", "Downloaded and verified", failure text per `UpdateFailure`, buttons Download / Cancel / Install / Try again / Later. Notes still render as Markdown. |
| `ui/.../settings/AboutSection.kt` | Collects `LocalUpdateInstaller.state`, starts the download, installs, cancels; falls back to opening the release page when there is no installer (desktop). |
| `ui/.../presentation/MemoraCloudApp.kt` | Same wiring for the launch-time "update available" dialog. |
| `ui/.../composeResources/values/strings.xml` | 12 new strings for the states above. |
| `android/.../core/update/UpdateModule.kt` | Registers `ApkUpdateInstaller` and `UpdateDownloadWorker`. |
| `android/.../presentation/platform/AndroidPlatformProviders.kt` | Provides `LocalUpdateInstaller`. |
| `android/.../core/common/AppNotifications.kt` | `notifyUpdateReady()` on the existing *Updates* channel (`NOTIFICATION_ID_UPDATE_READY = 1006`, request code 6). |
| `android/src/main/AndroidManifest.xml` | `REQUEST_INSTALL_PACKAGES` (Android still asks the user to confirm; this permission only lets the app open the installer for its own APK). |
| `android/src/main/res/values/strings.xml` | Strings for the "update ready to install" notification. |

`res/xml/file_paths.xml` needed no change — `cache-path` was already exposed, and the APK lives in
`cache/updates/`.

## The flow the user sees

1. *Settings → About → Check for updates* (or the automatic daily check, or launch).
2. Dialog: version, notes, **Download**.
3. **Downloading… 42%** with a progress bar and a working **Cancel download**.
4. **Verifying the download…** while the hash and the signature are compared.
5. **Install** — Android shows *Do you want to update this app?*; the user confirms.
6. If the user has never allowed installs from this app, step 5 first opens *Install unknown apps*
   for Memora Cloud, and the dialog then offers **Install** again (the verified file is kept, so
   nothing is downloaded twice).

## Safety rules enforced in code

* The hash comes from `update.json` in the same release; a mismatch deletes the file and shows
  "did not match the published SHA-256 checksum". No partial or unchecked file is ever installed.
* The APK's signing certificate digest must equal the installed app's, and the package name must
  match. A mismatch deletes the file (`UpdateFailure.SIGNATURE`). This is what stops a repacked APK
  from a look-alike release from being trusted.
* `install()` only ever hands over a file that passed both checks (`verifiedApk`); it is not
  reachable with an arbitrary path.
* No silent installation is attempted or claimed.

## Known limits (deliberate, not bugs)

* **Signature change means manual reinstall.** An app built with a different keystore (e.g. a CI
  release installed over an old debug build) cannot be updated in place; Android reports
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstall once, then the library restores from Telegram.
  The updater detects this case *before* downloading and shows the signature error instead.
* **Resume is best-effort.** If the server does not honour `Range`, the download restarts. Partial
  bytes are reused when GitHub does honour it.
* **AOSP/privileged silent update** is out of scope; it needs a system signature.
* The downloaded APK stays in the cache; *Settings → Storage* clears it, and Android may clear it
  itself under pressure — the release can simply be downloaded again.
