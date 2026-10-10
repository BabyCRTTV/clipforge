"""Pinned public-video importer. Uses the embedded JS engine, not CLI flag mapping."""
import json
import threading
from yt_dlp import YoutubeDL
from yt_dlp.extractor.youtube.jsc._builtin.ejs import EJSBaseJCP
from yt_dlp.extractor.youtube.jsc.provider import register_provider, register_preference
from java import jclass

_state = threading.local()

@register_provider
class AndroidQuickJS(EJSBaseJCP):
    PROVIDER_NAME = 'clipforge_quickjs'
    JS_RUNTIME_NAME = 'quickjs'

    def is_available(self):
        return self._available

    def _run_js_runtime(self, stdin):
        callback = getattr(_state, 'callback', None)
        cancel = callback.cancellation if callback else None
        return jclass('com.eightbittrade.clipforge.QuickJs').evaluate(stdin, cancel)

@register_preference(AndroidQuickJS)
def preference(provider, request):
    return 1000

class Logger:
    def __init__(self, callback): self.callback = callback
    def debug(self, message): pass
    def warning(self, message): self.callback.message('YouTube: ' + message)
    def error(self, message): pass

def options(callback):
    def progress(data):
        if callback.cancelled(): raise InterruptedError('Cancelled')
        if data.get('downloaded_bytes', 0) > 4 * 1024**3: raise ValueError('This preview supports sources up to 4 GB.')
        total = data.get('total_bytes') or data.get('total_bytes_estimate') or 0
        percent = int(100 * data.get('downloaded_bytes', 0) / total) if total else 0
        callback.message(f'Downloading source · {min(percent, 100)}%')
    _state.callback = callback
    return dict(noplaylist=True, quiet=True, no_warnings=False, logger=Logger(callback),
                socket_timeout=20, retries=2, fragment_retries=2, extractor_retries=2,
                progress_hooks=[progress], nopart=True, continuedl=False,
                max_filesize=4 * 1024**3, cachedir=False, js_runtimes={})

def metadata(link, callback):
    try:
        with YoutubeDL(options(callback)) as dl:
            data = dl.extract_info(link, download=False)
            if data.get('is_live'): raise ValueError('Please choose a completed video. Live streams are not supported yet.')
            return json.dumps({'title': data.get('title'), 'heatmap': data.get('heatmap')})
    finally: _state.callback = None

def download(link, format, destination, callback):
    try:
        opts = options(callback)
        opts.update(format=format, outtmpl=destination, fixup='never')
        with YoutubeDL(opts) as dl:
            dl.extract_info(link, download=True)
    finally: _state.callback = None

def engine_version():
    from yt_dlp.version import __version__
    return __version__
