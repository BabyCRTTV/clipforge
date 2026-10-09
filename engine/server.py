"""Loopback-only HTTP UI and serialized media jobs. Python 3.11+."""
import json, mimetypes, os, queue, secrets, shutil, sys, threading, time, uuid, webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse
from core import run,probe,youtube_url,clip_range,transcribe,export

ROOT=Path(__file__).resolve().parents[1]
DATA=Path(os.environ.get('CLIPFORGE_DATA',Path.home()/'.clipforge')).resolve()
DATA.mkdir(parents=True,exist_ok=True)
TOKEN=secrets.token_urlsafe(32)
JOBS={}; VIDEOS={}; LOCK=threading.Lock(); QUEUE=queue.Queue()
MAX_UPLOAD=2*1024**3
PORT=int(os.environ.get('CLIPFORGE_PORT','8765'))

def update(job,**fields):
    with LOCK: JOBS[job].update(fields)

def submit(fn):
    ident=uuid.uuid4().hex
    with LOCK: JOBS[ident]={'id':ident,'state':'queued','message':'Waiting for the engine','created':time.time()}
    QUEUE.put((ident,fn)); return ident

def worker():
    while True:
        ident,fn=QUEUE.get()
        try:
            update(ident,state='working')
            result=fn(ident)
            update(ident,state='done',message='Ready',result=result)
        except Exception as e:
            update(ident,state='error',message=str(e)[-2400:])
        finally: QUEUE.task_done()

def add_video(path,title):
    info=probe(path)
    ident=uuid.uuid4().hex
    VIDEOS[ident]={'id':ident,'path':str(path),'title':str(title)[:200],**info}
    return {k:v for k,v in VIDEOS[ident].items() if k!='path'}

def import_youtube(url,job):
    update(job,message='Checking the YouTube video')
    options=[sys.executable,'-m','yt_dlp','--ignore-config','--no-playlist','--socket-timeout','30','--retries','3']
    info=json.loads(run(options+['--skip-download','--dump-single-json',url]))
    if info.get('is_live'):
        raise ValueError('Use a completed video or live replay.')
    if float(info.get('duration') or 0)>14400:
        raise ValueError('The first version supports videos up to four hours.')
    folder=DATA/'sources'/info['id']; folder.mkdir(parents=True,exist_ok=True)
    cached=next((p for p in folder.glob('source.*') if p.suffix in ('.mp4','.mkv','.webm','.mov')),None)
    if cached is None:
        update(job,message='Downloading the best available source. This is cached for future clips.')
        run(options+['-f','bv*+ba/b','--merge-output-format','mp4','--max-filesize','4G','-o',str(folder/'source.%(ext)s'),url])
        cached=next((p for p in folder.glob('source.*') if p.suffix in ('.mp4','.mkv','.webm','.mov')),None)
    if cached is None: raise RuntimeError('No source downloaded. Try uploading your original video instead.')
    return add_video(cached,info.get('title','YouTube video'))

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def send_json(self,data,status=200):
        body=json.dumps(data).encode(); self.send_response(status)
        self.send_header('Content-Type','application/json'); self.send_header('Cache-Control','no-store'); self.send_header('Content-Length',str(len(body))); self.end_headers(); self.wfile.write(body)
    def authorized(self):
        host=self.headers.get('Host','')
        origin=self.headers.get('Origin')
        return host in (f'127.0.0.1:{PORT}',f'localhost:{PORT}') and (origin is None or origin in (f'http://127.0.0.1:{PORT}',f'http://localhost:{PORT}'))
    def do_GET(self):
        if not self.authorized(): return self.send_json({'error':'Use the local ClipForge address.'},403)
        path=urlparse(self.path).path
        if path=='/api/session': return self.send_json({'token':TOKEN,'engine':True,'ffmpeg':bool(shutil.which('ffmpeg'))})
        if path.startswith('/api/jobs/'):
            with LOCK: data=dict(JOBS.get(path.rsplit('/',1)[-1],{}))
            return self.send_json(data or {'error':'Job not found'},200 if data else 404)
        if path.startswith('/sources/'):
            video=VIDEOS.get(path.rsplit('/',1)[-1])
            if not video: return self.send_json({'error':'Not found'},404)
            return self.serve_file(Path(video['path']))
        if path.startswith('/outputs/'):
            pieces=path.split('/')
            if len(pieces)!=4 or pieces[3] not in ('clip.mp4','captions.srt') or not all(c in '0123456789abcdef' for c in pieces[2]) or len(pieces[2])!=32:
                return self.send_json({'error':'Not found'},404)
            file=DATA/'jobs'/pieces[2]/pieces[3]
            return self.serve_file(file,download=True)
        rel='index.html' if path=='/' else path.lstrip('/')
        file=(ROOT/'dist'/rel).resolve()
        if not file.is_relative_to((ROOT/'dist').resolve()): return self.send_json({'error':'Not found'},404)
        return self.serve_file(file)
    def serve_file(self,file,download=False):
        if not file.is_file(): return self.send_json({'error':'Not found'},404)
        size=file.stat().st_size
        start,end=0,size-1
        requested=self.headers.get('Range')
        if requested:
            import re
            m=re.fullmatch(r'bytes=(\d+)-(\d*)',requested)
            if not m: return self.send_json({'error':'Unsupported byte range'},416)
            start=int(m[1]); end=min(int(m[2]) if m[2] else end,end)
            if start>end: return self.send_json({'error':'Range exceeds file'},416)
        self.send_response(206 if requested else 200)
        self.send_header('Content-Type',mimetypes.guess_type(file.name)[0] or 'application/octet-stream')
        self.send_header('Content-Length',str(end-start+1)); self.send_header('Accept-Ranges','bytes')
        self.send_header('X-Content-Type-Options','nosniff')
        if requested: self.send_header('Content-Range',f'bytes {start}-{end}/{size}')
        if download: self.send_header('Content-Disposition',f'attachment; filename="{file.name}"')
        self.end_headers()
        with file.open('rb') as f:
            f.seek(start); left=end-start+1
            while left:
                chunk=f.read(min(left,1024*1024))
                if not chunk: break
                try: self.wfile.write(chunk)
                except (BrokenPipeError,ConnectionResetError): break
                left-=len(chunk)
    def do_POST(self):
        if not self.authorized() or not secrets.compare_digest(self.headers.get('X-ClipForge-Token',''),TOKEN):
            return self.send_json({'error':'Reload the local editor to reconnect.'},403)
        try:
            length=int(self.headers.get('Content-Length','0'))
            if length<=0: raise ValueError('Empty request.')
            path=urlparse(self.path).path
            if path=='/api/upload':
                if length>MAX_UPLOAD: return self.send_json({'error':'Upload limit is 2 GB.'},413)
                ident=uuid.uuid4().hex; folder=DATA/'sources'/ident; folder.mkdir(parents=True)
                from urllib.parse import unquote
                name=unquote(self.headers.get('X-Filename','video.mp4'))
                suffix=Path(name).suffix.lower()
                file=folder/('source'+(suffix if suffix in ('.mp4','.webm','.mkv','.mov','.avi','.m4v') else '.bin'))
                try:
                    with file.open('wb') as f:
                        left=length
                        while left:
                            chunk=self.rfile.read(min(left,1024*1024))
                            if not chunk: raise ValueError('Upload interrupted.')
                            f.write(chunk); left-=len(chunk)
                    result=add_video(file,name)
                except Exception:
                    shutil.rmtree(folder); raise
                return self.send_json({'video':result})
            if length>256*1024: return self.send_json({'error':'Request too large'},413)
            data=json.loads(self.rfile.read(length))
            if path=='/api/import':
                url=youtube_url(data.get('url',''))
                return self.send_json({'job':submit(lambda j:import_youtube(url,j))})
            if path in ('/api/caption','/api/export'):
                video=VIDEOS.get(data.get('video_id'))
                if not video: raise ValueError('Import a video first. Re-import after restarting the engine.')
                start,end=clip_range(data,video['duration'])
                def process(job):
                    folder=DATA/'jobs'/job; folder.mkdir(parents=True)
                    if path=='/api/caption':
                        if not video['audio']: raise ValueError('This video has no audio. You can add captions manually.')
                        update(job,message='Transcribing the clip. The first run downloads the speech model.')
                        cues=transcribe(video['path'],start,end,folder,data.get('model','base'),data.get('language'))
                        return {'cues':cues}
                    update(job,message='Rendering your vertical clip with source frame rate and high-quality encoding')
                    result=export(video['path'],video,data,folder)
                    return {**result,'video_url':f'/outputs/{job}/clip.mp4','srt_url':f'/outputs/{job}/captions.srt'}
                return self.send_json({'job':submit(process)})
            return self.send_json({'error':'Not found'},404)
        except Exception as e: return self.send_json({'error':str(e)},400)

if __name__=='__main__':
    if not shutil.which('ffmpeg') or not shutil.which('ffprobe'):
        sys.exit('Install FFmpeg first. See README.md, then run Start again.')
    threading.Thread(target=worker,daemon=True).start()
    server=ThreadingHTTPServer(('127.0.0.1',PORT),Handler)
    print(f'ClipForge is ready: http://127.0.0.1:{PORT}\nKeep this window open. Press Ctrl+C to stop.',flush=True)
    if '--no-browser' not in sys.argv: webbrowser.open(f'http://127.0.0.1:{PORT}')
    try: server.serve_forever()
    except KeyboardInterrupt: server.server_close()
