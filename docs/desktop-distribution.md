# Desktop packaging

Use JDK 21 with `jpackage` and Python 3 on the target operating system.

```sh
sh mvnw -Pui-tests verify
python tools/package_desktop.py --jar target/phytoscope-0.3.1.jar
```

On Windows, replace `sh mvnw` with `mvnw.cmd`.

The script builds an application image and writes an archive under `target/desktop`. Windows builds produce a ZIP. Linux and macOS builds produce a `.tar.gz` archive. Extract the whole archive before launching the application. Keep the launcher and bundled runtime together.

The app images are unsigned. The packaging script checks that the bundled launcher can parse a built-in example from the command line.

Official downloads and their SHA-256 checksums are available from [GitHub Releases](https://github.com/omixcore/phytoscope/releases).
