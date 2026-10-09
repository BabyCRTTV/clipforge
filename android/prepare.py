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
