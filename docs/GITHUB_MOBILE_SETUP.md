# Upload Memora Cloud to GitHub from Android Chrome

Everything below is doable in the Chrome app on the phone. No PC required.
You need: your GitHub account, the `MemoraCloud-source.zip` file (keep it in `Downloads`), and a
few minutes.

---

## Step 1 — Create the repository

1. Open <https://github.com/new> in Chrome (turn on *Desktop site* in the ⋮ menu if the page looks
   cramped).
2. **Repository name:** `Tele-Cloud` (this must match the links baked into the app:
   `github.com/Adnanh444/Tele-Cloud`; a different name means the update check finds nothing).
3. **Visibility:** **Private** is recommended while you set up signing.
4. Do **not** tick "Add a README" — an empty repo unpacks more cleanly.
5. Tap **Create repository**.

## Step 2 — Upload the archive

1. On the empty repository page tap **uploading an existing file** (or *Add file → Upload files*).
2. Tap **choose your files** → pick `MemoraCloud-source.zip` from Downloads.
3. Wait for the upload to finish (≈2 MB, a few seconds), then tap **Commit changes**.

## Step 3 — Allow Actions to write

*Settings → Actions → General → Workflow permissions →* select **Read and write permissions** →
**Save**. (Needed once, by the unpack step below.)

## Step 4 — Unpack in the cloud

1. Open the **Actions** tab → **Import source archive** (left sidebar) → **Run workflow** →
   branch `main` → **Run workflow**.
2. Wait ~30 s. When it is green, go to the **Code** tab: you should see `android/`, `shared/`,
   `ui/`, `gradle/`, `.github/`, `README.md` and so on. The archive and the helper workflow delete
   themselves.

If it fails with *Archive not found*, the file is not at the repository root — upload it again at
the top level, not inside a folder.

## Step 5 — Make the first (unsigned) APK, to prove the build works

**Actions → Build → Run workflow → Run workflow.** A green run gives
**MemoraCloud-debug-apk** in the run's *Artifacts* section (tap the run, scroll down, tap the
artifact; Chrome downloads a `.zip`, unzip with the Files app and install the APK — you must allow
"install unknown apps" for Chrome).

This APK is signed with a throwaway debug key, so do not build a habit of installing it: the next
signed release will refuse to install over it and you will have to uninstall once. Do Step 6 before
you start using the app for real.

## Step 6 — Permanent signing key + first real release

Follow `docs/SIGNING_AND_RELEASES.md` (5 minutes, also phone-only). Then:

1. **Actions → Release → Run workflow**, version `1.0.0` → **Run workflow**.
2. The run publishes a **Release** with `MemoraCloud-1.0.0.apk`, `SHA256SUMS.txt` and `update.json`.
3. Open *Releases* → download the APK → install. This is the build you keep using; every later
   release installs over it and keeps your data.

## Step 7 — Later releases

For each new version: **Actions → Release → Run workflow** and type the next version
(`1.0.1`, `1.0.2`, …). The `versionCode` is derived from the run number, so it always increases.
Alternatively, from a PC: `git tag v1.0.1 && git push origin v1.0.1`.

The in-app updater (Part 2) reads the same release, so once it ships you will be able to update from
inside Memora Cloud.

---

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Build fails at `chmod +x gradlew` / `Permission denied` | You uploaded files one by one. Re-run Step 4, or run `git update-index --chmod=+x gradlew` from a terminal. The bundled workflows already run `chmod +x gradlew`, so this should not happen with the ZIP. |
| `Missing secret KEYSTORE` in the Release run | Step 6 not finished — see `docs/SIGNING_AND_RELEASES.md`. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | An APK with a different signing key is installed. Uninstall the old one once (data is re-downloadable from Telegram: sign in, pick the channel, the index rebuilds). |
| Update check says "up to date" but a release exists | The release is a draft, a pre-release, or the tag has no `v` prefix. Publish it as a normal release. |
| Release APK won't install ("App not installed") | Play Protect can block sideloaded APKs; choose *Install anyway*, or temporarily disable scanning for that install. |
