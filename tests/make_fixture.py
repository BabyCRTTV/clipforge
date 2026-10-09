"""Generate a tiny synthetic source for actual renderer and HTTP tests."""
import subprocess
from pathlib import Path
folder=Path(__file__).resolve().parent/'fixtures'
folder.mkdir(exist_ok=True)
subprocess.run(['ffmpeg','-v','error','-y','-f','lavfi','-i','testsrc2=size=1280x720:rate=30','-f','lavfi','-i','sine=frequency=440:sample_rate=48000','-t','4','-c:v','libx264','-preset','ultrafast','-c:a','aac',str(folder/'source.mp4')],check=True)
print('Test video created.')
