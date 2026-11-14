# Desktop distribution

> **Data-archiving notice (post-`8fd01ca`).** The current application JAR/package
> contains only six KB-scale example annotations. Complete reference data lives in the
> independent `phytoscope-reference-data` repository (local directory ready, remote
> URLs pending user provision) and is not packaged into the app image; there is no
> in-app download. Only the current checkout and build artifacts got smaller - git
> history pack (~209 MiB) and fresh-clone size are unchanged.

Build with JDK 21 on each target OS:

```bash
sh mvnw -Pui-tests verify
python tools/package_desktop.py --jar target/phytoscope-0.3.0.jar --out target/desktop-v0.3.0
```

The script creates a `jpackage` application image plus an OS/architecture-specific
archive: `.tar.gz` on Linux/macOS (preserving executable permissions) or `.zip`
on Windows. Unpack this archive before launching.

> **B05 (0.3.0) authoritative archive name.** The single authoritative Windows
> release asset is **`../dist/PhytoScope-Windows-0.3.0.zip`** (46,185,790 bytes,
> SHA-256 `8f7953eba6c04922be9b2faf21b717ff3fe36cc15a5bd1f3393b38da7716bfa6`).
> `tools/package_desktop.py` still emits `PhytoScope-Windows-AMD64.zip` (raw
> jpackage output, no version in the name) and keeps a legacy bundle
> `--app-version 0.2.0` constant; **that is NOT the application version**. This
> batch does not modify the script — it builds into an isolated `--out` directory
> and renames the copy. Application version is `pom.xml` 0.3.0 / tag / ZIP name.
> Two older archives in `../dist/` — `PhytoScope-Windows-AMD64.zip` (113,094,544 B)
> and `PhytoScope-Windows-0.2.0.zip` (110,405,267 B) — are labelled **`historical`**,
> retained **byte-for-byte unchanged**, and are NOT the 0.3.0 release asset.
> See `docs/fcs-v0.3.0-release-notes.md` and `desktop_program/SHA256SUMS.txt`.

## Packaged application layout

| Item | Purpose |
| --- | --- |
| `PhytoScope.exe` (Windows) / `.app` (macOS) / `bin/PhytoScope` (Linux) | Launcher — double-click or run directly |
| `app/` | Application JAR and bundled libraries |
| `runtime/` | Bundled Java runtime — recipients need no JDK, Maven or separate Java install |

Keep all items together inside the extracted folder. The image is
OS/architecture specific and unsigned: it is an application image, not a signed
installer or app-store distribution. Linux additionally requires its normal
desktop graphics/font libraries.

## Release checklist

- The current JAR ships only six KB-scale examples; complete reference data is archived separately and is not packaged into the app image.
- Compute and publish a SHA-256 checksum for every archive (see
  `desktop_program/SHA256SUMS.txt` for the Windows package).
- Tag the release commit; attach archives to a GitHub Release instead of
  linking to branch files, so downloads stay stable across updates.
- The `Validate and package PhytoScope` workflow builds images on Ubuntu,
  Windows and macOS and uploads them as separate artifacts with test evidence.
  Artifacts become available only after the corresponding job succeeds.
  JavaFX tests use Monocle, so they do not certify OS-native dialogs, display
  scaling, graphics drivers, signing/notarization or security prompts. A release
  should record actual native launch/import/export checks on its target machines
  separately (see `docs/desktop-verification.md`).

## Portable projects

Portable `.phytoscope` projects embed annotation sources and a canonical matrix
snapshot. The original source label and the import report remain in the manifest;
invalid cells are represented as missing in the snapshot. File checksums protect
restoration, and annotation changes after import prevent an inconsistent save.
