"""ClipForge media engine. No cloud credentials or per-video services."""
import json, math, os, re, subprocess, sys
from pathlib import Path
from urllib.parse import urlparse, parse_qs


def youtube_url(value):
    u = urlparse(str(value).strip())
    if u.scheme != 'https' or u.username or u.password or u.port:
        raise ValueError('Use a normal https YouTube video link.')
    host = (u.hostname or '').lower()
    if host == 'youtu.be':
        video = u.path.strip('/')
    elif host in ('youtube.com', 'www.youtube.com', 'm.youtube.com'):
        video = parse_qs(u.query).get('v', [''])[0] if u.path == '/watch' else u.path.split('/')[-1] if u.path.startswith(('/shorts/', '/live/')) else ''
    else:
        video = ''
    if not re.fullmatch(r'[A-Za-z0-9_-]{11}', video):
        raise ValueError('Please paste a single YouTube video, Shorts, or live replay link.')
    return 'https://www.youtube.com/watch?v=' + video


def run(args, cwd=None):
    p = subprocess.run([str(a) for a in args], cwd=cwd, capture_output=True, text=True, timeout=7200)
    if p.returncode:
        raise RuntimeError(p.stderr[-2000:] or 'Media command failed.')
    return p.stdout


def probe(path):
    data = json.loads(run(['ffprobe','-v','error','-show_streams','-show_format','-of','json',path]))
    v = next((s for s in data['streams'] if s['codec_type'] == 'video'), None)
    if not v:
        raise ValueError('This file does not contain a video stream.')
    if v.get('color_transfer') in ('smpte2084','arib-std-b67'):
        raise ValueError('HDR is not supported yet. Use an SDR source to avoid incorrect colors.')
    if any(int(x.get('rotation', 0)) % 180 for x in v.get('side_data_list', [])):
        v['width'], v['height'] = v['height'], v['width']
    return {'width':v['width'], 'height':v['height'], 'duration':float(data['format'].get('duration',0)), 'fps':v.get('avg_frame_rate','0/1'), 'audio':any(s['codec_type']=='audio' for s in data['streams'])}


def clip_range(data, duration):
    start, end = float(data.get('start',0)), float(data.get('end',60))
    if not all(math.isfinite(x) for x in (start,end)) or start < 0 or end <= start or end > duration + .05 or end-start > 180:
        raise ValueError('Choose a clip between 0.1 and 180 seconds, within the video duration.')
    if end-start < .1:
        raise ValueError('The clip must be at least 0.1 seconds.')
    return start, end


def framing(meta, requested=1920, mode='crop', position=.5):
    # Never enlarge the source. Crop reduces available horizontal pixels.
    available = min(meta['height'], meta['width'] * 16 / 9) if mode == 'crop' else max(meta['height'],meta['width']*16/9)
    height = max(32, int(min(requested, available) // 32) * 32)
    width = height * 9 // 16
    position = max(0, min(1, float(position)))
    if mode == 'fit':
        f = f'scale={width}:{height}:force_original_aspect_ratio=decrease:force_divisible_by=2,pad={width}:{height}:(ow-iw)/2:(oh-ih)/2:color=0x11131b,setsar=1'
    else:
        # Scale the short dimension to cover portrait, then crop horizontally.
        f = f'scale={width}:{height}:force_original_aspect_ratio=increase:force_divisible_by=2,crop={width}:{height}:(iw-ow)*{position}:(ih-oh)/2,setsar=1'
    return width, height, f


def ass_time(t):
    cs = max(0, round(float(t)*100))
    return f'{cs//360000}:{cs//6000%60:02}:{cs//100%60:02}.{cs%100:02}'


def srt_time(t):
    ms = max(0, round(float(t)*1000))
    return f'{ms//3600000:02}:{ms//60000%60:02}:{ms//1000%60:02},{ms%1000:03}'


def safe_text(text):
    # Prevent user caption text from introducing ASS formatting directives.
    return str(text).replace('\\','').replace('{','').replace('}','').replace('\r','').replace('\n',r'\N').strip()


def normalize_cues(cues, duration):
    if not isinstance(cues,list) or len(cues)>500:
        raise ValueError('Too many captions.')
    result=[]
    previous_end=0
    for c in sorted(cues,key=lambda c:float(c['start'])):
        a,b=float(c['start']),float(c['end'])
        text=str(c.get('text','')).strip()
        if not all(math.isfinite(x) for x in (a,b)) or a<previous_end-.01 or b<=a or b>duration+.05:
            raise ValueError('Caption times must be ordered, non-overlapping, and inside the clip.')
        if len(text)>250:
            raise ValueError('Keep each caption under 250 characters.')
        if text:
            result.append({'start':a,'end':b,'text':text}); previous_end=b
    return result


def caption_files(folder, cues, width, height, style='clean'):
    size=round(height*.037)
    margin=round(height*.22)
    color='&H0000E6FF' if style=='yellow' else '&H00FFFFFF'
    outline=max(1,round(height*.0028))
    header=f'''[Script Info]\nScriptType: v4.00+\nPlayResX: {width}\nPlayResY: {height}\nWrapStyle: 0\nScaledBorderAndShadow: yes\n\n[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\nStyle: Default,Arial,{size},{color},&H0000E6FF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,{outline},1,2,{round(width*.09)},{round(width*.14)},{margin},1\n\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n'''
    (folder/'captions.ass').write_text(header+''.join(f"Dialogue: 0,{ass_time(c['start'])},{ass_time(c['end'])},Default,,0,0,0,,{safe_text(c['text'])}\n" for c in cues),encoding='utf-8')
    (folder/'captions.srt').write_text('\n'.join(f"{i+1}\n{srt_time(c['start'])} --> {srt_time(c['end'])}\n{c['text']}\n" for i,c in enumerate(cues)),encoding='utf-8')


MODELS={}
def transcribe(path, start, end, folder, model='base', language=None):
    if model not in ('base','small'):
        raise ValueError('Choose base or small transcription.')
    from faster_whisper import WhisperModel
    run(['ffmpeg','-v','error','-y','-ss',start,'-i',path,'-t',end-start,'-vn','-ac','1','-ar','16000',folder/'speech.wav'])
    if model not in MODELS:
        device=os.environ.get('CLIPFORGE_DEVICE','cpu')
        MODELS[model]=WhisperModel(model,device=device,compute_type='float16' if device=='cuda' else 'int8')
    segments, info=MODELS[model].transcribe(str(folder/'speech.wav'),word_timestamps=True,vad_filter=True,language=language or None,beam_size=3)
    cues=[]
    for segment in segments:
        group=[]
        for w in segment.words or []:
            if group and (len(' '.join(x.word.strip() for x in group))+len(w.word)>34 or w.end-group[0].start>2.8):
                cues.append({'start':group[0].start,'end':group[-1].end,'text':' '.join(x.word.strip() for x in group)})
                group=[]
            group.append(w)
        if group:
            cues.append({'start':group[0].start,'end':min(end-start,group[-1].end),'text':' '.join(x.word.strip() for x in group)})
    return cues


def export(path, meta, data, folder):
    start,end=clip_range(data,meta['duration'])
    mode=data.get('framing','crop')
    if mode not in ('crop','fit') or data.get('resolution',1920) not in (1280,1920,3840):
        raise ValueError('Invalid framing or resolution.')
    width,height,filters=framing(meta,data.get('resolution',1920),mode,data.get('position',.5))
    cues=normalize_cues(data.get('cues',[]),end-start)
    caption_files(folder,cues,width,height,data.get('style','clean'))
    if cues:
        filters+=",ass=captions.ass"
    quality=data.get('quality','balanced')
    preset,crf=('fast','16') if quality=='quality' else ('veryfast','18')
    run(['ffmpeg','-v','error','-y','-ss',start,'-i',path,'-t',end-start,'-map','0:v:0','-map','0:a:0?','-vf',filters,'-c:v','libx264','-preset',preset,'-crf',crf,'-pix_fmt','yuv420p','-fps_mode','passthrough','-c:a','aac','-b:a','192k','-movflags','+faststart',folder/'clip.mp4'],cwd=folder)
    return {'width':width,'height':height,'duration':end-start,'captions':len(cues)}
