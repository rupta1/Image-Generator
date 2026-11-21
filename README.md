# Image Generator — Android Native AI Studio

A native Kotlin/Jetpack Compose Android app foundation for privacy-first, on-device image generation and local personalization.

## Target device
Initial target: POCO X6 Pro (Dimensity 8350-Ultimate, Mali-G615 MC6, 8 GB RAM).

## Current milestone
This repository starts with the native UI, local prompt/settings persistence architecture, gallery shell, and adaptive-preference data layer. **Real diffusion inference and LoRA fine-tuning are not yet wired in.** The app will not pretend to generate images until a supported model/runtime is installed and integrated.

## Build
Open the project in Android Studio (current stable), allow Gradle sync, then run the app on an Android device.

## Planned inference
Benchmark compatible mobile inference paths (ONNX Runtime Android / NNAPI and GPU execution where supported) against a mobile-optimized Stable Diffusion pipeline. Device-specific GPU support is not guaranteed. Model files are not committed.

## Privacy
Prompt history and preference signals are intended to stay on device. Learning is opt-in and controls are included in the app foundation. Model downloads, when implemented, must be user-initiated and must respect model licences.

## Roadmap
- [x] Native Compose starter and studio UI
- [x] Prompt, negative prompt and settings state
- [x] Local gallery/history foundation
- [x] Adaptive preference data model
- [ ] Diffusion model/runtime integration
- [ ] Model manager and download progress
- [ ] Generation queue and real progress
- [ ] Image-to-image
- [ ] LoRA inference
- [ ] Investigate on-device fine-tuning (experimental)
