# Signing and releases (phone-only workflow)

Memora Cloud must be signed with **one permanent key** for its whole life. Android refuses to
install an update signed by a different key, and re-signing would force every user (you) to
uninstall and re-download everything.

Rules that follow from that:

* the key never goes into the repository, the ZIP, or a chat message;
* the key is stored once as **GitHub Actions secrets** (and in your own safe backup);
* the `applicationId` never changes (`com.adnan.memoracloud`);
* `versionCode` always increases (the release workflow handles it).

---

## A. Create the keystore (once)

Easiest, no PC: run the bundled helper.

1. **Actions → Generate signing keystore (one time) → Run workflow.**
2. When it finishes, open the run → **Summary**. It shows a table with the four secret values:
   `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Scroll to **Artifacts** → `memoracloud-keystore` → download it. Unzip and keep
   `memoracloud.jks` somewhere private (e.g. a password manager attachment or an encrypted cloud
   folder) **before** deleting the artifact.
4. Copy the whole `KEYSTORE` value (one long base64 line — tap the copy icon or select the text).

> The helper prints a random password on the runner. It is visible to anyone who can read your
> workflow runs, which is why the repository should be **private** while you do this. Afterwards
> delete the artifact (it expires after 1 day) and, if you want, the run.

If you prefer doing it from a PC with the Android SDK, the equivalent command is:

```bash
keytool -genkeypair -v -keystore memoracloud.jks -alias memoracloud \
  -keyalg RSA -keysize 4096 -validity 9125 \
  -dname "CN=Memora Cloud, OU=Adnan, O=Memora Cloud, L=Dhaka, C=BD"
base64 -w0 memoracloud.jks > memoracloud.jks.b64   # macOS: base64 -i memoracloud.jks -o memoracloud.jks.b64
```

## B. Add the four secrets

GitHub mobile web: repository → **Settings** → *Secrets and variables* → **Actions** →
**New repository secret**, four times:

| Name | Value |
| --- | --- |
| `KEYSTORE` | contents of `memoracloud.jks.b64` (base64, one line, no line breaks) |
| `KEYSTORE_PASSWORD` | store password from the summary |
| `KEY_ALIAS` | `memoracloud` |
| `KEY_PASSWORD` | key password from the summary |

They are write-only: neither you nor anyone else can read them back, so if you lose the `.jks` file
you cannot produce compatible updates any more. Back it up.

## C. Cut a release

**Actions → Release → Run workflow**

* `version`: `1.0.0` for the first one, then `1.0.1`, `1.1.0`, …
* `notes`: optional markdown; it becomes the release body.

The workflow then, on GitHub's runners:

1. checks that all four secrets exist (and fails with a clear message naming the missing one);
2. decodes the keystore to a temp file;
3. builds `:android:assembleRelease -PsplitAbis=false` with
   `-PappVersionName=<your version>` and `-PappVersionCode=1000+<run number>`;
4. runs `apksigner verify --print-certs` and fails if the APK is not signed;
5. writes `MemoraCloud-<version>.apk`, `SHA256SUMS.txt` and `update.json`;
6. publishes a **GitHub Release** with all three files.

`update.json` looks like this and is what the in-app updater (Part 2) reads:

```json
{ "version": "1.0.1", "versionCode": 1001,
  "apk": "MemoraCloud-1.0.1.apk",
  "sha256": "…", "minSdk": 26,
  "page": "https://github.com/Adnanh444/Tele-Cloud/releases/tag/v1.0.1" }
```

## D. Install the release

Download the APK from the Release page in Chrome → open it → allow "install unknown apps" for
Chrome → *Install*. Android shows the installer dialog; **there is no silent install** — that
requires a privileged/root installer, and this app will never pretend otherwise.

Because the `applicationId` and the signing key are unchanged, the install is an **update**: app
data, your Telegram session and the channel selection survive.

## E. Version numbering

* `versionName` — what you type in the workflow (`1.0.0`, `1.0.1`, …).
* `versionCode` — `1000 + run number` of the Release workflow. Monotonic, so Android always sees a
  newer build. Local builds fall back to `gradle/libs.versions.toml` (currently `1`).

## F. If you lose the key

You cannot publish an update that installs over the existing one. The only path is to keep the same
applicationId but sign the new build with a new key, which Android treats as a different app:
users must uninstall and reinstall. Memora Cloud's data is recoverable (log in with the same
Telegram account, the backup channel's manifest rebuilds the index), so this is annoying, not
fatal — but treat the keystore as the single most important file of this project.
