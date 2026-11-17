# Publication and ignore rules

[Documentation](README.md) · [Architecture](ARCHITECTURE.md) · [Testing](TESTING.md)

The public repository contains source, documentation and explicitly reviewed synthetic screenshots. Local build output, signing material, provider setup and device evidence stay outside the published tree.

## What is excluded

| Category | Rules |
| --- | --- |
| Build/dependency output | `app/build/`, `app/deps/`, `dist/` |
| Generated Android packages | `.apk`, `.aab`, `.apks` and `.idsig` files anywhere in the checkout |
| Signing/identity files | `.p12`, `.pfx`, `.jks`, `.keystore`, `.pem`, `.key`, `.p8`, `.pk8` |
| Local environment and runtime data | `.env` and `.env.*` except `.env.example`; cookies, encrypted files, database files/sidecars and logs |
| Known raw setup files | `leise-tv-camera-config.json` and `hue-bridge-local.json`, including nested copies |
| Work and test evidence | Root `work/`, `outputs/`, `test-results/`, plus names containing lowercase `private` |
| Images | PNG/JPG/JPEG, including mixed-case extensions; only exact reviewed exceptions are allowed |
| Python/macOS output | Python caches, `.pyc` and `.DS_Store` |

The legacy camera-configuration filename remains intentional because the existing provisioning receiver uses it. JSON/XML/Java files are not broadly ignored: translation catalogs, Android resources, source and ordinary documentation must remain trackable.

## Reviewed screenshots

Each published image has one exact `!/docs/images/...` entry. Do not replace this list with an exception allowing every image in the directory. Review new screenshots for camera content, network details, credentials and other personal data before adding a new exception and staging that image.

The November 13 audit preserved all 26 existing image exceptions. The November 14 publication adds one reviewed synthetic Mediathek settings capture, bringing that publication’s exact allowlist to 27 images. Version 0.12.0 adds two reviewed synthetic profile captures (29 images total). No photograph or screenshot from the second physical TV is published. Uppercase or mixed-case image extensions do not bypass the default exclusion. Native XML drawable resources are unaffected.

## Reproduce the checks

```sh
python3 tests/check_gitignore.py
git ls-files -ci --exclude-standard
git status --short
git diff --cached --check
```

The Python check uses Git's real pattern matching with `--no-index`, case-sensitive matching and no global ignore file, so a machine-wide rule cannot hide a missing repository rule. It uses synthetic paths; it creates no credential or package files. It checks excluded artifacts, important source/template paths, exact image exceptions, tracked files matching ignore rules and private-key blocks in index content. It never reads the ignored local signing key or raw setup files.

At the November 13, 2026 audit, 165 existing ignored files were build/dependency/package output, Python caches or the local signing file. No tracked file matched the sensitive/generated categories or ignore rules examined. These are checkout/index observations, not a new TV test or a certification of the entire Git history.

## November 14 source publication

The 0.11.6 staged tree passed 73 synthetic ignore-rule cases, with 27 reviewed images and 117 index entries checked. Gitleaks 8.30.1 scanned the exact staged file contents with full redaction enabled and found no secrets. The reviewed addition is a synthetic ARD/ZDF settings card; actual TV screenshots, login pages and raw device reports remain outside the repository. Build output and the private installation variant are excluded. This scan is separate from the earlier reachable-history audit and does not rewrite or remove any history.

## Scope and limits

`.gitignore` controls discovery of untracked files; it does not remove already tracked files and cannot inspect arbitrary passwords or camera URLs in an otherwise allowed document. The private-key-block check is a narrow additional guard, not a complete secret scanner. Example environment files must contain placeholders only.

Inspect staged changes before pushing. If a real credential is found in published history, investigate it separately and rotate the exposed credential; adding an ignore rule alone would not undo that disclosure. This audit did not rewrite history, delete local files or change any TV configuration.

## 0.13.0 publication check

The exact 127-file staged snapshot passed 75 ignore/publication cases, with the same 29 reviewed image exceptions. Gitleaks 8.30.1 with full redaction found no secrets; a separate known-private-device-address check found no matches. All 212 local Markdown link targets resolved. The two refreshed profile images were captured from synthetic local fixture data and visually reviewed. Private builds, signing material and physical-device evidence remain excluded. These checks do not rewrite history or guarantee detection of every private value.

## 0.13.3 screenshot refresh

All 29 allowlisted images were replaced with reviewed current captures: 14 native emulator screens and 15 loopback web views. Application DEX matched the public APK, and 27 native navigation/capture checks passed. Local OCR processed all 29 images without errors and found only allowlisted synthetic addresses, with no access-code labels. Image metadata/format checks passed, as did the five Home pixel checks for centered legends and green WLAN glyphs. The final staged snapshot retained 127 files and 29 exact image exceptions; Gitleaks found no secrets and all 215 local Markdown targets resolved. These checks support the synthetic publication boundary and do not establish real-provider or additional hardware acceptance.

## Current 0.14.0 favorites publication

The 134-file staged snapshot passed 77 publication-rule cases with 31 exact reviewed image exceptions. The two additional desktop/phone captures show synthetic local favorite choices and were visually reviewed; the 29 earlier gallery views retain their explicitly labelled 0.13.3 baseline. Image output is metadata-stripped. All 234 local Markdown targets resolved. Gitleaks 8.30.1 with full redaction found no secrets in the exact staged contents; the known-private-device-address check also found no matches. Public/private APKs, signing material and raw native/browser evidence remain excluded. No physical TV was updated for this publication; these staged-tree checks do not replace a full history audit or physical acceptance.
