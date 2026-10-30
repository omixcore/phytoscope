# Desktop distribution

Build with JDK 21 on each target OS:

```bash
sh mvnw -Pui-tests verify
python tools/package_desktop.py --jar target/phytoscope-0.2.0.jar
```

The script creates a `jpackage` application image plus an OS/architecture-specific
archive: `.tar.gz` on Linux/macOS (preserving executable permissions) or `.zip`
on Windows. Unpack this archive before launching.

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
