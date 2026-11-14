#!/usr/bin/env python3
"""Check production profile validation/storage with in-memory Android adapters, without contacting a TV."""
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parent.parent
dependency = root / 'test-results/json-20240303.jar'
digest = '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed'
if not dependency.exists():
    dependency.parent.mkdir(exist_ok=True)
    subprocess.run(['curl', '--fail', '--location', '--max-time', '30', '--output', str(dependency),
                    'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar'], check=True)
if hashlib.sha256(dependency.read_bytes()).hexdigest() != digest:
    raise SystemExit('JSON-java test dependency checksum mismatch')
java_home = os.environ.get('JAVA_HOME')
if not java_home and sys.platform == 'darwin':
    java_home = subprocess.check_output(['/usr/libexec/java_home', '-v', '21'], text=True).strip()
if not java_home:
    java_home = str(Path(shutil.which('javac')).resolve().parent.parent)
java = Path(java_home) / 'bin'
with tempfile.TemporaryDirectory(prefix='atpd-profiles-') as output:
    sources = [root / p for p in [
        'tests/profiles/android/content/Context.java', 'tests/profiles/android/content/SharedPreferences.java',
        'app/src/de/leisetv/AppSelection.java', 'app/src/de/leisetv/ProfileConfig.java', 'app/src/de/leisetv/DashboardProfiles.java',
        'app/src/de/leisetv/HomeLayout.java', 'app/src/de/leisetv/HomeAppearance.java',
        'app/src/de/leisetv/IntegrationSettings.java', 'tests/java/de/leisetv/ProfileChecks.java']]
    subprocess.run([str(java / 'javac'), '-encoding', 'UTF-8', '-classpath', str(dependency), '-d', output, *map(str, sources)], check=True)
    subprocess.run([str(java / 'java'), '-cp', output + os.pathsep + str(dependency), 'de.leisetv.ProfileChecks'], check=True)
