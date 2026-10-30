"""Build a self-contained OS-specific application image using the current JDK's jpackage."""
import argparse, os, platform, shutil, subprocess, sys
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);p.add_argument('--out',type=Path,default=Path('target/desktop'));a=p.parse_args()
if not a.jar.is_file(): raise SystemExit('Build the executable JAR first')
jpackage=Path(os.environ.get('JAVA_HOME',''))/'bin'/('jpackage.exe' if sys.platform=='win32' else 'jpackage')
if not jpackage.is_file():
    found=shutil.which('jpackage')
    if not found: raise SystemExit('A full JDK 21 with jpackage is required')
    jpackage=Path(found)
staging=Path('target/jpackage-input');staging.mkdir(parents=True,exist_ok=True)
for old in staging.iterdir():
    if old.is_file():old.unlink()
shutil.copy2(a.jar,staging/a.jar.name)
# jpackage refuses to run when the destination image already exists; rebuild from scratch.
existing=a.out/('PhytoScope.app' if sys.platform=='darwin' else 'PhytoScope')
if existing.exists(): shutil.rmtree(existing)
a.out.mkdir(parents=True,exist_ok=True)
# macOS rejects bundle versions whose first component is zero, while the
# Maven application version remains 0.2.0. Use a valid bundle-only version there.
app_version = '1.0.0' if sys.platform == 'darwin' else '0.2.0'
command=[str(jpackage),'--type','app-image','--name','PhytoScope','--app-version',app_version,'--vendor','omixcore',
         '--input',str(staging),'--main-jar',a.jar.name,'--dest',str(a.out),
         # Slim runtime: only the JDK modules the application actually uses.
         # java.se would double the runtime size for modules never touched.
         # JavaFX ships inside the fat jar (with its native libraries), so
         # javafx.* must NOT be listed here: they are not JDK modules.
         '--add-modules','java.base,java.logging,java.xml,java.sql,java.naming,java.management,'
         'java.desktop,java.instrument,java.security.jgss,java.security.sasl,'
         'jdk.unsupported,jdk.crypto.ec,jdk.zipfs',
         '--java-options','-Xmx3g',
         '--description','Plant gene structure and expression comparison']
subprocess.run(command,check=True)
print('Application image:',a.out.resolve())

if sys.platform=='darwin': launcher=a.out/'PhytoScope.app/Contents/MacOS/PhytoScope'
elif sys.platform=='win32': launcher=a.out/'PhytoScope/PhytoScope.exe'
else: launcher=a.out/'PhytoScope/bin/PhytoScope'
subprocess.run([str(launcher.resolve()),'--benchmark','--example','rice','--out','target/packaged-smoke'],check=True)
print('PASS: packaged runtime launched and parsed the rice example')

image_name='PhytoScope.app' if sys.platform=='darwin' else 'PhytoScope'
archive=shutil.make_archive(str(a.out/('PhytoScope-'+platform.system()+'-'+platform.machine())),
                           'zip' if sys.platform=='win32' else 'gztar',root_dir=a.out,base_dir=image_name)
print('Distributable archive:',archive)
