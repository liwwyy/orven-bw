#!/usr/bin/env python3
"""Extract compile-only API jars from an installed OneConfig beta. Never modifies the pack."""
import argparse
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = Path('/mnt/nvme/PrismLauncher/instances/OneClient Beta final/minecraft/mods/OneConfig-1.8.9-ornithe-1.2.9.jar')
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('jar', nargs='?', type=Path, default=DEFAULT)
args = p.parse_args()
if not args.jar.is_file():
    p.error(f'OneConfig beta jar not found: {args.jar}. Supply its path explicitly.')
with zipfile.ZipFile(args.jar) as z:
    import json
    metadata = json.loads(z.read('fabric.mod.json'))
    if metadata['version'] != '1.2.9':
        p.error('This build is verified against OneConfig 1.2.9. Update dependencies before using another version.')
    out = ROOT / '.reference' / 'oneconfig-beta'
    out.mkdir(parents=True, exist_ok=True)
    members = [n for n in z.namelist() if n.startswith('META-INF/jars/') and n.endswith('.jar')]
    for n in members:
        (out / Path(n).name).write_bytes(z.read(n))
print(f'Extracted {len(members)} compile-only jars from {args.jar} to {out}')
