"""Fetch the exact MIT-licensed whisper.cpp source used by the Android JNI build."""
from pathlib import Path
import subprocess
root=Path(__file__).resolve().parent
vendor=root/'vendor'/'whisper'
if not (vendor/'CMakeLists.txt').exists():
    vendor.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run(['git','clone','--depth','1','--branch','v1.7.6','https://github.com/ggml-org/whisper.cpp.git',str(vendor)],check=True)
    sha=subprocess.check_output(['git','-C',str(vendor),'rev-parse','HEAD'],text=True).strip()
    if sha!='a8d002cfd879315632a579e73f0148d06959de36':
        raise SystemExit('Unexpected whisper.cpp tag revision; build stopped.')

# Pure Python wheels are loaded ahead of the legacy bootstrap AAR's package.
# Pin both versions and hashes: yt-dlp checks EJS's own bundled script hashes too.
import hashlib
import urllib.request
assets=root/'app/src/main/assets/downloader'
assets.mkdir(parents=True,exist_ok=True)
wheels=[
 ('yt_dlp-2026.8.19-py3-none-any.whl','https://files.pythonhosted.org/packages/69/b2/8cd1613f56eed7ceb64fbd4df3f1c01246bfb098e6f398228bafda22b80b/yt_dlp-2026.8.19-py3-none-any.whl','1d57897e94c6665a0a6f9bc54b34e584284e32c034ffab3a7df25d8f7b24eedf'),
 ('yt_dlp_ejs-0.8.0-py3-none-any.whl','https://files.pythonhosted.org/packages/e3/bd/520769863744b669440a924271a6159ddd82ad5ae26b4ac4d4b69e9f8d44/yt_dlp_ejs-0.8.0-py3-none-any.whl','79300e5fca7f937a1eeede11f0456862c1b41107ce1d726871e0207424f4bdb4'),
]
for name,url,expected in wheels:
    path=assets/name
    if not path.exists():
        with urllib.request.urlopen(url,timeout=60) as response: data=response.read()
        if hashlib.sha256(data).hexdigest()!=expected: raise SystemExit('Downloader wheel checksum mismatch')
        path.write_bytes(data)
    if hashlib.sha256(path.read_bytes()).hexdigest()!=expected: raise SystemExit('Downloader wheel checksum mismatch')
quickjs=root/'vendor/quickjs'
if not (quickjs/'quickjs.c').exists():
    subprocess.run(['git','clone','--depth','1','--branch','v0.12.0','https://github.com/quickjs-ng/quickjs.git',str(quickjs)],check=True)
if subprocess.check_output(['git','-C',str(quickjs),'rev-parse','HEAD'],text=True).strip()!='db870fd4619bf8c659cf85dd092101ea2a1a8a07':
    raise SystemExit('Unexpected QuickJS-NG tag revision; build stopped.')

(assets/'QuickJS-LICENSE.txt').write_bytes((quickjs/'LICENSE').read_bytes())
