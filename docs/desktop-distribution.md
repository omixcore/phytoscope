# Desktop distribution

Build with JDK 21 on each target OS:

```bash
sh mvnw -Pui-tests verify
python tools/package_desktop.py --jar target/phytoscope-0.2.0.jar
```

The `jpackage` application image contains a Java runtime; recipients do not need
Maven or a separate JDK. Launch `PhytoScope` from the app image (`PhytoScope.exe`
on Windows; the `.app` on macOS; `bin/PhytoScope` on Linux). Keep the complete
image directory together. The image is OS/architecture specific and unsigned.
Linux still requires its normal desktop graphics/font libraries. This is an
application image, not a signed installer or an app-store distribution.

The `Validate and package PhytoScope` workflow builds images on Ubuntu, Windows
and macOS and uploads them as separate artifacts with test evidence. Artifacts
become available only after the corresponding job succeeds. JavaFX tests use
Monocle, so they do not certify OS-native dialogs, display scaling, graphics
drivers, signing/notarization or security prompts. A release should record actual
native launch/import/export checks on its target machines separately.

Portable `.phytoscope` projects embed annotation sources and a canonical matrix
snapshot. The original source label and the import report remain in the manifest;
invalid cells are represented as missing in the snapshot. File checksums protect
restoration, and annotation changes after import prevent an inconsistent save.
