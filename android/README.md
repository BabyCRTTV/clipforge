# ClipForge for Android - 0.2.0 preview

A native app, not a website shortcut. Android 10+ on 64-bit ARM phones and x86_64 emulators. Four guided steps: source → recommended moments → captions → export/save. No account or paid API needed.

## On the phone

1. Download the APK from the repository's Android release. Open it and allow installation from the app you used to download it, when Android asks.
2. Paste a YouTube link (or share the link to ClipForge), or select an original using Android's system file picker.
3. ClipForge recommends three distinct moments when the video is at least 45 seconds long. The first is selected by default. Public YouTube heatmap peaks are prioritized when extraction succeeds. Every suggestion names its evidence: **YouTube replay peak**, **Content suggestion**, or **Review sample**. Heatmap values are relative replay intensity, not exact view counts. Private YouTube Studio analytics are not requested. There is no OAuth or Google login.
4. When no heatmap is available, allow the speech-model download to analyze the video's content. Or select times manually and analyze only your clip. Full-video analysis can be slow; a 60-minute video takes much longer than a 30-second clip. Preview suggestions before choosing. Adjust start/end times and framing. Clips can be 1–180 seconds.
5. Review captions and timestamps. The default small.en model (~488 MB once) uses beam search and token timestamps. base.en (~148 MB) is quicker. English is supported in this preview. Proper names, music, noisy speech, accents and overlapping voices can need corrections. Uncertain phrases are marked using model token probability, which is not a calibrated accuracy guarantee.
6. Pick white text with active-word lime highlighting, yellow, or clean white. Text uses short phrases, bold type, black outlines, and space above platform controls. Edited phrases receive evenly spaced word timings; original recognition preserves token-derived timing. These are popular short-form presentation patterns, not a live trends feed or a promise of virality.
7. Export while keeping the app open. Save to gallery writes a real MP4 to **Movies/ClipForge**, visible in Gallery/Photos. Sharing uses Android's share sheet. Drafts are saved locally, including captions and the last export. Return to Moments to export another suggestion.

## Quality and limits

Source video/audio downloads are combined without re-encoding. Rendering a vertical crop and burned-in captions necessarily re-encodes video; bit-for-bit original quality is impossible. The Android app uses Media3 and hardware H.264/AAC, retains source frame rate when the device supports it, never intentionally upscales, and caps output at 1080×1920. Dimensions are exact 9:16 with even sizes. A landscape source loses horizontal pixels when cropped. Fit keeps the whole image on a black portrait canvas. HDR input is rejected with a clear message in this preview. Codec and encoder support varies by phone.

YouTube can block extraction or downloads; newer videos may require JS/PO-token support that the bundled downloader cannot supply. The app does not defeat login, age, region or bot protections. Use an original video if a link fails. Public replay data is not guaranteed even when YouTube's player shows it. This is a preview release: test it on your phone before depending on it for a posting schedule.

## Permissions and privacy

Only `INTERNET` is declared. No microphone, camera, contacts, location, notifications, all-files access, or broad photo/video-library permission. Android's picker grants access to the selected file; MediaStore saves the app's own output without storage permission on Android 10+. The app keeps the screen on while processing, rather than requesting background-service permissions. Keep it open until processing finishes. No advertising, tracking, analytics, hosted processing, or ChatGPT dependency. Original speech and video are not sent to a transcription service. YouTube receives link requests and Hugging Face supplies the first model download. Model downloads verify pinned SHA-256 hashes before use. App backup is disabled. Clear Workspace removes private drafts/video caches and keeps gallery videos and models; uninstall removes private app data.

## Build from source

Use JDK 17, Android SDK platform/build-tools 35, NDK 27.2.12479018 and CMake 3.22.1. Run:

```sh
cd android
python prepare.py
./gradlew assembleDebug testDebugUnitTest
```

The Gradle wrapper pins Gradle 8.13. `prepare.py` pins whisper.cpp v1.7.6 by commit. Media3 1.6.1 and yt-dlp-android 2.0.2 are pinned Maven dependencies. Both arm64-v8a and x86_64 are included. Native code is built with 16 KB page-size alignment. The CI workflow publishes build artifacts. The downloadable APK is a non-debuggable release build signed with a privately retained development certificate. Its certificate SHA-256 is `c79c84a6b1c094d29f849ce1f67e2c1b014c85b1f229e3b9db0d7327f58fd4a3`. CI debug APKs are test builds and can use a different signing certificate than the downloadable manually signed preview; replacing one with the other can require uninstalling first. Production/store distribution should use a private, durable release key.

## Component licenses

App source: MIT (repository LICENSE). whisper.cpp/ggml: MIT; downloaded models derive from OpenAI Whisper (MIT). Media3/AndroidX: Apache-2.0. yt-dlp-android: MIT; embedded yt-dlp: Unlicense, CPython: PSF, Chaquopy: its BSD license, certifi: MPL-2.0. Upstream AAR assets contain THIRD-PARTY-NOTICES.txt. All dependencies retain their own licenses. See THIRD-PARTY-NOTICES.md for source links.
