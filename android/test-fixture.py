"""Create the integration fixture; neither model nor sample is shipped in the app."""
from pathlib import Path
import urllib.request,subprocess,hashlib
root=Path(__file__).resolve().parent
assets=root/'app/src/androidTest/assets';assets.mkdir(parents=True,exist_ok=True)
audio=root/'vendor/whisper/samples/jfk.wav'
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-f','lavfi','-i','testsrc2=size=640x360:rate=15','-i',str(audio),'-t','11','-c:v','libx264','-preset','ultrafast','-crf','18','-pix_fmt','yuv420p','-c:a','aac','-movflags','+faststart',str(assets/'fixture.mp4')],check=True)
model=assets/'ggml-base.en.bin'
if not model.exists():
 urllib.request.urlretrieve('https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-base.en.bin',model)
if hashlib.sha256(model.read_bytes()).hexdigest()!='a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002':
 raise SystemExit('Unexpected model checksum')
