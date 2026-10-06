#!/usr/bin/env python3
"""Publish the build's immutable versioned release, retrying transient gh failures."""
import os
from pathlib import Path
import subprocess
import time


def publish(tag, version, jar, commit, runner=subprocess.run, sleep=time.sleep):
    files = [Path('release')/jar, Path('release')/(jar+'.sha256')]
    if not all(p.is_file() for p in files):
        raise FileNotFoundError('Release jar or checksum is missing')
    for attempt in range(3):
        existing = runner(['gh', 'release', 'view', tag, '--json', 'url'], capture_output=True, text=True)
        if existing.returncode == 0:
            print(f'{tag} already exists; leaving it unchanged.')
            return
        result = runner(['gh', 'release', 'create', tag, *map(str, files), '--target', commit,
                         '--title', f'orven-bw Ornithe {version}', '--generate-notes'], capture_output=True, text=True)
        if result.returncode == 0:
            print(result.stdout.strip())
            return
        print(f'Publication attempt {attempt+1} failed: {result.stderr.strip()}')
        if attempt < 2: sleep(5)
    raise RuntimeError(f'Could not publish {tag} after three attempts; build artifacts remain available.')


if __name__ == '__main__':
    publish(os.environ['TAG'], os.environ['VERSION'], os.environ['JAR'], os.environ['COMMIT'])
