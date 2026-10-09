# ClipForge by 8BitTrade

**[Open ClipForge](https://babycrttv.github.io/clipforge/)** · **[Download the desktop engine](https://babycrttv.github.io/clipforge/clipforge-v0.1.0.zip)**

The complete project is in **BabyCRTTV/clipforge** on GitHub: source code, desktop engine, website, launchers, tests, and packaging. No ChatGPT login or ChatGPT-hosted service is required. Use **Code → Download ZIP** to download the complete project, or download the prepared application from `dist/clipforge-v0.1.0.zip`.

A local-first short-form video studio. Paste a YouTube video link or upload an original video, choose a time range, frame it in 9:16, generate and edit captions, then download a high-quality MP4 and SRT. No watermark or paid transcription API.

## Quick start — Windows

1. Install **Python 3.11 or newer** from https://www.python.org/downloads/. During installation, select **Add Python to PATH**. Python 3.11/3.12 are recommended.
2. Open Terminal and install FFmpeg: `winget install Gyan.FFmpeg`.
3. For reliable YouTube extraction, also install Deno: `winget install DenoLand.Deno`. yt-dlp uses this JavaScript runtime; it is not needed for uploaded videos.
4. Close Terminal and reopen it so the new commands are available.
5. Extract the download ZIP. Double-click **Setup-Windows.bat**. Setup downloads the Python libraries once.
6. Double-click **Start-Windows.bat**. The local editor opens at **http://127.0.0.1:8765**. Keep the engine window open while working.

This is a Python desktop companion with a browser interface, not a packaged EXE. Setup needs internet access. A standalone installer can be added later.

## macOS / Linux

Install Python 3.11+, FFmpeg, and optionally Deno using your system package manager. On macOS with Homebrew: `brew install python@3.12 ffmpeg deno`. On Ubuntu, install `python3`, `python3-venv`, and `ffmpeg`; install Deno from its official instructions. Then, from the extracted folder:

```sh
bash setup.sh
bash start.sh
```

## Creating a clip

1. Paste a single YouTube video/Shorts/live replay URL and press the arrow, or select a video file.
2. Wait for the source to load. YouTube sources are downloaded in full at the best available quality and cached. The first clip can take longer; subsequent clips skip the download. Sources up to four hours are accepted; uploaded files are limited to 2 GB.
3. Enter the start and end times **in seconds**, or use the 15/30/60-second buttons. Maximum clip length: 180 seconds. This release uses manual clip selection; it does not automatically pick highlights.
4. Choose **Fill frame** to crop into 9:16 and adjust the horizontal position, or **Fit whole video** to preserve the entire shot with dark padding. Subject tracking is manual.
5. Choose a speech model and click **Generate captions**. Base is faster; small is generally more accurate. The first transcription downloads a speech model. Automatic language detection is enabled. The words and timing are editable, measured from the beginning of the selected clip. Changing the clip range clears the old captions.
6. Review names and spelling. Choose white or yellow captions. You can also add phrases manually or export without captions.
7. Select maximum resolution and export quality, then **Export clip**. Download the MP4 and optional SRT subtitle file.

## Quality and speed

Exports use H.264 at CRF 18 (veryfast) or CRF 16 (fast), with source frame rate and 192 kbps AAC audio. Crop/pad and burned-in captions require re-encoding, so exports cannot be identical to the source. The renderer never enlarges video pixels. A 1920×1080 landscape source cropped to portrait produces at most 594×1056; the narrow crop simply does not contain 1080 pixels across. Choose Fit whole video to keep all original content. Output dimensions are exact 9:16 and rounded down to multiples suitable for encoding.

The speed depends on source size, computer performance, and the speech model. No fixed export-time promise is made. The first YouTube download and first model download usually dominate startup time. Jobs run one at a time to avoid overwhelming your computer. An NVIDIA CUDA environment can use `CLIPFORGE_DEVICE=cuda`; the appropriate CUDA/cuDNN libraries must already be installed. CPU mode works by default.

HDR sources are rejected to avoid incorrect colors. Video previews depend on browser codec support; MP4/H.264 is the safest format. Most YouTube sources are downloaded as MP4, but preview support can vary with codec. Export can still work when a browser cannot decode the source preview.

## Hosted site versus local editor

The hosted site is a download/setup page and interactive framing preview. It does not run FFmpeg, download YouTube videos, or transcribe speech. Those actions run in the editor opened by Start on your computer. The site does not attempt to access your local engine across origins. Hosting the full service would require a persistent video-processing server, storage limits, and user authentication; none are provisioned by this release.

## Troubleshooting

- **FFmpeg not found:** close and reopen the terminal after installation. Confirm `ffmpeg -version` and `ffprobe -version` work.
- **YouTube link fails:** confirm Deno is installed and try updating yt-dlp: `.venv\Scripts\python.exe -m pip install --upgrade "yt-dlp[default]"` on Windows. Some videos require sign-in, are restricted, or trigger YouTube anti-bot checks. This release does not bypass access restrictions or collect browser cookies. Use your original video file instead.
- **Speech model download fails:** check your internet connection and access to Hugging Face. No API token is required for the default models.
- **Captions are inaccurate:** try the small model and edit the transcript. Music, overlapping voices, and accents can affect recognition.
- **Engine restarted:** import the source again; cached YouTube sources are reused. Session projects/caption edits are not saved automatically.
- **Another app uses port 8765:** set `CLIPFORGE_PORT` before launch.

## Local storage and security

Imported videos, downloaded sources, audio extracts, speech models, and exports remain on your computer. Media files are under `.clipforge` in your home directory; speech model downloads use the Hugging Face cache. To reclaim space, close the engine and remove the `.clipforge` contents when no longer needed. There is no automatic cache deletion or retention guarantee. Store the MP4/SRT downloads you want to keep before removing cached files.

The engine binds to `127.0.0.1`, checks Host and Origin, and requires a per-session token for changes. It is intended for a trusted personal computer. **Do not expose the port through a tunnel or on a public server.** Caption text is escaped before inclusion in ASS subtitles. Media commands use argument arrays, not shell interpolation. There are no account credentials in the source. Python dependencies, YouTube downloads, external UI fonts, and model downloads contact their respective services. UI fonts fall back to system fonts offline.

Use videos you own or have permission to edit and repost.

## Development

- `dist/` — shared hosted/local interface, vanilla HTML/CSS/JavaScript.
- `engine/core.py` — validation, transcription, framing, ASS/SRT generation, FFmpeg exports.
- `engine/server.py` — loopback HTTP server and serialized job queue.
- `tests/` — validations and actual FFmpeg/HTTP integration tests.
- `requirements.txt` — tested direct dependencies, including a compatible PyAV version.
- `scripts/package.py` — regenerates the complete portable application ZIP.
- `.github/workflows/pages.yml` — packages and publishes the website on GitHub Pages.

Regenerate the application ZIP after code changes with `python scripts/package.py`. This requires only Python, and does not install media-processing dependencies.

Run `python -m unittest discover -s tests -v` and `node --check dist/app.js`. Integration tests skip when the generated video fixture is missing; `python tests/make_fixture.py` creates one. The GitHub Pages workflow builds the application ZIP and deploys `dist/`. Enable Pages with **GitHub Actions** as its source in repository settings. The Pages deployment runs only for a public repository; free accounts need public visibility to enable Pages. Keep Python engine processing local. The site and source have no dependency on ChatGPT hosting.

## v0.1.0 validation

Verified: URL/range validation, caption escaping and timing validation, exact 9:16 crop and fit exports, source frame-rate preservation, audio export, HTTP upload, queued processing, MP4/SRT downloads, byte-range serving, and rejected cross-origin mutations. Real generated speech was used to exercise transcription. YouTube end-to-end download could not be verified in the development environment because its outbound proxy rejected TLS certificates; TLS verification remains enabled. The public GitHub Pages website and application download were checked in Chrome; full local editor browser automation was unavailable. Windows/macOS launcher behavior was not tested on those systems.

Built with FFmpeg, yt-dlp, and faster-whisper. See their respective projects and licenses:
- https://ffmpeg.org/
- https://github.com/yt-dlp/yt-dlp
- https://github.com/SYSTRAN/faster-whisper
