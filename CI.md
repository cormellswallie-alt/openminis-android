# Android-only mirror + GitHub Actions build

This repo is the Android subset of [OpenMinis/OpenMinis](https://github.com/OpenMinis/OpenMinis)
(upstream commit `b4c0661`, v1.14). iOS sources, `deps/ish`, FFmpeg and LAME were removed.

## Build

Actions → **Build APK** → Run workflow (choose `release` / `debug`).
Pushing a tag `v*` also builds and attaches the APK to a GitHub Release.

The APK is uploaded as a workflow artifact (arm64-v8a only).

Pipeline: `deps/build_proot.sh` → `scripts/prepare_android_sandbox.sh` →
`deps/build_rclone_android.sh` (gomobile) → `./gradlew :app:assemble<Type>`.
proot, the Alpine rootfs and `rclone.aar` are cached, so only the first run is slow.

## Secrets (Settings → Secrets and variables → Actions)

| Secret | Required | Purpose |
|---|---|---|
| `SIGNING_KEYSTORE_BASE64` | recommended | base64 of a PKCS12 keystore, alias `androiddebugkey`, passwords `android`. Keeps the signature stable so new builds upgrade in place. |
| `ANTHROPIC_OAUTH_IDENTIFIER_PROMPT` | optional | Only for Claude OAuth sign-in (see BUILDING.md). |

## Notes

- Package name defaults to `com.openminis.app`. Use the `application_id` input
  to build a side-by-side copy (e.g. `com.openminis.app.dev`).
- An APK signed with this key cannot upgrade an official build (different signature).
