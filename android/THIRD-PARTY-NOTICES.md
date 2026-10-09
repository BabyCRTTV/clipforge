# Third-party source and licenses

- [whisper.cpp v1.7.6](https://github.com/ggml-org/whisper.cpp/tree/a8d002cfd879315632a579e73f0148d06959de36): MIT. Includes ggml. Source fetched by `prepare.py`; upstream LICENSE preserved.
- [Whisper models](https://huggingface.co/ggerganov/whisper.cpp/tree/5359861c739e955e79d9a303bcbc70fb988958b1): OpenAI Whisper MIT license. Downloaded with user consent; not bundled into the APK.
- [AndroidX Media3](https://github.com/androidx/media): Apache-2.0, Maven version 1.6.1.
- [yt-dlp-android](https://github.com/ffmpegkit-maintained/yt-dlp-android): MIT, Maven version 2.0.2. Its AAR includes upstream THIRD-PARTY-NOTICES.txt covering Python/Chaquopy/yt-dlp/certifi.
- [yt-dlp](https://github.com/yt-dlp/yt-dlp): Unlicense for normal source; bundled dependencies keep their licenses.
- [CPython](https://github.com/python/cpython): PSF license.
- [Chaquopy](https://github.com/chaquo/chaquopy): BSD license.
- [certifi](https://github.com/certifi/python-certifi): MPL-2.0.

These notices supplement, not replace, the licenses shipped with upstream artifacts. ClipForge ships no FFmpeg binary in its Android APK; video decoding, muxing and encoding use Android APIs/Media3.
