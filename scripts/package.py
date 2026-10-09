"""Build the complete portable ClipForge download with no credentials or runtime data."""
from pathlib import Path
import zipfile
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'dist'/'clipforge-v0.1.0.zip'
EXCLUDED={'.git','.openai','.venv','__pycache__','fixtures','android'}
with zipfile.ZipFile(DEST,'w',zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(ROOT.rglob('*')):
        relative=path.relative_to(ROOT)
        if not path.is_file() or any(part in EXCLUDED for part in relative.parts) or path.suffix in ('.zip','.pyc'):
            continue
        archive.write(path,Path('ClipForge')/relative)
print(f'Packaged {DEST.name}')
