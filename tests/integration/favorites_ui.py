#!/usr/bin/env python3
"""Native favorite dispatch/storage/layout checks; disposable emulator only, launches intercepted."""
from pathlib import Path
import argparse,subprocess,os,tempfile,zipfile,json,re,sys,shutil
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--output',type=Path,default=Path('test-results/favorites'));args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
root=Path(__file__).resolve().parents[2];sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT',str(Path.home()/('Library/Android/sdk' if sys.platform=='darwin' else 'Android/Sdk')))));adb=[os.environ.get('ADB',str(sdk/'platform-tools/adb')),'-s',args.serial];tools=sdk/'build-tools/36.1.0';jar=sdk/'platforms/android-34/android.jar'
java_home=os.environ.get('JAVA_HOME')
if not java_home and sys.platform=='darwin':java_home=subprocess.check_output(['/usr/libexec/java_home','-v','21'],text=True).strip()
java=Path(java_home) if java_home else Path(shutil.which('javac')).resolve().parent.parent
env=dict(os.environ,NICKOS_SIGN_PASSWORD=os.environ.get('NICKOS_SIGN_PASSWORD','nickos-local-test'))
assert subprocess.check_output(adb+['shell','getprop','ro.kernel.qemu'],text=True).strip()=='1'
def run(*a):return subprocess.run(list(map(str,a)),env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,check=True,timeout=120)
with tempfile.TemporaryDirectory(prefix='atpd-favorites-') as tmp:
 t=Path(tmp);(t/'classes').mkdir();(t/'dex').mkdir();(t/'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="fixture.favorites"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="28"/><application android:label="Disposable favorites checks"/><instrumentation android:name="fixture.FavoritesProbe" android:targetPackage="de.leisetv"/></manifest>')
 try:
  run(java/'bin/javac','--release','8','-classpath',jar,'-d',t/'classes',root/'tests/integration/FavoritesProbe.java');run(tools/'d8','--min-api','26','--lib',jar,'--output',t/'dex',*list((t/'classes').rglob('*.class')));run(tools/'aapt','package','-f','-M',t/'AndroidManifest.xml','-I',jar,'-F',t/'probe.apk')
  with zipfile.ZipFile(t/'probe.apk','a') as z:z.write(t/'dex/classes.dex','classes.dex')
  run(tools/'apksigner','sign','--ks',Path(os.environ.get('NICKOS_KEYSTORE',str(root/'app/local-test-signing.p12'))),'--ks-key-alias','leisetv','--ks-pass','env:NICKOS_SIGN_PASSWORD',t/'probe.apk');run(*adb,'install','-r',t/'probe.apk')
  result=run(*adb,'shell','am','instrument','-w','fixture.favorites/fixture.FavoritesProbe').stdout
  match=re.search(r'INSTRUMENTATION_RESULT: checks=(.*)',result);checks=json.loads(match.group(1)) if match else [];(args.output/'verification.json').write_text(json.dumps({'target':'isolated emulator; app launches intercepted; no real stream/HDMI source','checks':checks},indent=2))
  if 'fixture=ready' not in result:raise RuntimeError(result)
  run(*adb,'pull','/data/user/0/de.leisetv/files/favorites-home.png',args.output/'home.png');print(str(len(checks))+' native favorite validation/dispatch/layout assertions passed.')
 except subprocess.CalledProcessError as e:print(e.stderr);raise
