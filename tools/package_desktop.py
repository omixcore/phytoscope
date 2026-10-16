"""Build a self-contained OS-specific application image using the current JDK's jpackage."""
import argparse, os, shutil, subprocess, sys
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
a.out.mkdir(parents=True,exist_ok=True)
command=[str(jpackage),'--type','app-image','--name','PhytoScope','--app-version','0.2.0','--vendor','omixcore',
         '--input',str(staging),'--main-jar',a.jar.name,'--dest',str(a.out),
         '--add-modules','java.se,jdk.unsupported,jdk.crypto.ec','--java-options','-Xmx3g',
         '--description','Plant gene structure and expression comparison']
subprocess.run(command,check=True)
print('Application image:',a.out.resolve())

if sys.platform=='darwin': launcher=a.out/'PhytoScope.app/Contents/MacOS/PhytoScope'
elif sys.platform=='win32': launcher=a.out/'PhytoScope/PhytoScope.exe'
else: launcher=a.out/'PhytoScope/bin/PhytoScope'
subprocess.run([str(launcher.resolve()),'--benchmark','--example','rice','--out','target/packaged-smoke'],check=True)
print('PASS: packaged runtime launched and parsed the rice example')
