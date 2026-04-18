# TubeTone

Personal-use Android app: YouTube share → audio extraction → waveform trim → system ringtone.

## Build

```
./gradlew :app:assembleDebug
```

## Install

```
./gradlew :app:installDebug
```

## Usage

1. Open YouTube, tap Share → TubeTone.
2. Wait for waveform analysis.
3. Drag handles to pick a segment.
4. Tap "벨소리로 설정" → grant `WRITE_SETTINGS` once.
5. Manage saved ringtones in the Library tab.

## Notes

- NewPipeExtractor is the weak link — if YouTube changes internals, bump `libs.versions.toml` → `newpipe` and rebuild.
- Trimming uses native `MediaExtractor` + `MediaMuxer` (copy-mode, lossless, no re-encode). Fade in/out is planned for v1.1.
- Personal/sideload use only. Not for Play Store distribution.
