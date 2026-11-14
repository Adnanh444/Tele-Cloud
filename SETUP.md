# Memora Cloud — setup on GitHub (start here)

Everything here can be done from Chrome on your phone. No PC, no Android Studio, no local Android SDK.

Repository to create: **`Tele-Cloud`** under **`Adnanh444`** — that exact name is compiled into the
app (update checks read `github.com/Adnanh444/Tele-Cloud`).

---

## 1. Create the repository

1. Open <https://github.com/new>
2. **Repository name:** `Tele-Cloud`
3. **Visibility:** Private (recommended until signing is configured)
4. Do **not** add a README, .gitignore or license — an empty repo imports cleanly
5. **Create repository**

## 2. Upload this project

1. On the empty repo page tap **uploading an existing file** (or *Add file → Upload files*)
2. Pick `MemoraCloud-source.zip` (≈2 MB) from your phone's Downloads
3. **Commit changes**

## 3. Let Actions write (once)

*Settings → Actions → General → Workflow permissions →* **Read and write permissions** → **Save**

## 4. Unpack it

**Actions** tab → **Import source archive** → **Run workflow** → branch `main` → **Run workflow**

After ~30 s the Code tab shows `android/`, `shared/`, `ui/`, `gradle/`, `.github/`, `docs/`.
The ZIP and that helper workflow delete themselves.

## 5. Prove the build works

**Actions → Build → Run workflow** → wait for the green check → open the run → **Artifacts** →
`MemoraCloud-debug-apk`.

That APK is signed with a throwaway key — install it only to look around, then do step 6 before you
start backing up real files (a differently-signed build cannot be installed over it).

## 6. Create the permanent signing key (once)

**Actions → Generate signing keystore (one time) → Run workflow** → open the run → **Summary**:
it lists the four values. Then *Settings → Secrets and variables → Actions → New repository secret*,
four times:

| Secret | Value |
|---|---|
| `KEYSTORE` | the long base64 line from the summary |
| `KEYSTORE_PASSWORD` | shown in the summary |
| `KEY_ALIAS` | `memoracloud` |
| `KEY_PASSWORD` | shown in the summary |

Download the artifact once and keep `memoracloud.jks` somewhere safe, then delete the artifact.
**Lose this key and you can never update the app in place again.** Details: `docs/SIGNING_AND_RELEASES.md`.

## 7. Ship the first real release

**Actions → Release → Run workflow** → version `1.0.0` → **Run workflow**

It verifies the secrets, builds, verifies the signature with `apksigner`, and publishes a **Release**
containing:

* `MemoraCloud-1.0.0.apk` ← install this one
* `SHA256SUMS.txt`
* `update.json` ← what the in-app updater (Part 2) reads

Later versions: run the same workflow with `1.0.1`, `1.0.2`, … — `versionCode` is derived from the
run number so Android always accepts the update and keeps your data.

---

## Built-in workflows

| Workflow | When | Result |
|---|---|---|
| **Build** | push / PR / manual | debug APK artifact |
| **Release** | tag `v*` or manual | signed APK + checksums + `update.json` on a Release |
| **Import source archive** | manual, once | unpacks the uploaded ZIP and commits it |
| **Generate signing keystore** | manual, once | creates the keystore, prints the secrets |

## Requirements checklist

- [ ] Repo named exactly `Tele-Cloud` under `Adnanh444`
- [ ] Actions has read/write permission
- [ ] `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secrets added
- [ ] `memoracloud.jks` backed up outside GitHub
- [ ] First **Build** run is green before you trust the app with files

## If something goes wrong

| Symptom | Cause / fix |
|---|---|
| `Archive not found` | ZIP not at repo root — upload it at top level, not inside a folder |
| `Missing secret KEYSTORE` | Step 6 not done |
| `Permission denied` on `gradlew` | Workflows already run `chmod +x gradlew`; re-run the import step |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A build with a different signing key is installed — uninstall once, then sign-in restores your library from Telegram |
| Update check finds nothing | Release is draft/pre-release, or the tag lacks the `v` prefix |

Full walkthrough with troubleshooting: [`docs/GITHUB_MOBILE_SETUP.md`](docs/GITHUB_MOBILE_SETUP.md).
Feature-by-feature truth: [`docs/FEATURE_STATUS.md`](docs/FEATURE_STATUS.md).
