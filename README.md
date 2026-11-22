# Image Generator — Android Native AI Studio

A native Kotlin/Jetpack Compose Android app foundation for privacy-first, on-device image generation and local personalization.

## Target device
Initial target: POCO X6 Pro (Dimensity 8350-Ultimate, Mali-G615 MC6, 8 GB RAM). Hardware acceleration and model compatibility must be benchmarked on-device.

## Current milestone
The app includes the native UI, local prompt/settings persistence architecture, gallery shell, adaptive-preference data layer, model-management foundations, tokenizer, ONNX text-encoder and UNet runners, and a deterministic DDIM scheduler foundation.

**Real end-to-end diffusion inference is not yet integrated or verified.** The engine deliberately reports unavailability rather than simulating image generation. The catalog does not distribute model weights or claim a pack is ready to run.

## Build
Open the project in Android Studio (current stable), allow Gradle sync, then run the app on an Android device.

## Local inference roadmap
- Define requests, validation, cancellation, and progress contracts
- Model catalog and app-private model storage foundation
- Validate an ONNX Runtime Android pipeline and model pack, including tokenizer, text encoder, UNet, scheduler and VAE
- Benchmark NNAPI/GPU paths and memory use on the POCO X6 Pro
- Implement generation queue and persist outputs locally

Model binaries are not committed. Model acquisition must be user initiated and comply with each model's licence.

## Privacy
Prompt history and preference signals are intended to stay on device. Learning is opt-in. Model downloads, when implemented, must be user initiated and licence compliant.

## Roadmap
- [x] Native Compose starter and studio UI
- [x] Prompt, negative prompt and settings state
- [x] Local gallery/history foundation
- [x] Adaptive preference data model
- [x] Initial local inference interface and request validation
- [x] Model catalog and private storage groundwork
- [x] ONNX Runtime Android dependency and graph-session loader
- [x] Versioned model-pack manifest validation
- [x] Optional SHA-256 graph integrity verification and model-folder containment checks
- [x] Defined diffusion pack contract (model family, tokenizer assets, scheduler config and graph tensor schemas)
- [x] Tensor schema validation (name, declared shape and supported dtype)
- [x] Offline CLIP-style byte-level BPE tokenizer (basic implementation)
- [x] Load tokenizer assets through diffusion pack contract
- [x] ONNX text-encoder tensor runner foundation
- [x] ONNX UNet runner and float tensor validation foundation
- [x] Deterministic DDIM scheduler and descending inference timestep generation
- [x] Core DDIM denoising-loop orchestration with classifier-free guidance, progress callbacks and cancellation checks\n- [x] Seeded latent-noise initialization and float32 VAE decoder foundation (pipeline wiring pending)\n- [ ] Complete diffusion pipeline orchestration (graph adapters, end-to-end wiring) and validated model pack
- [x] Safe ZIP model-pack installer with manifest and optional SHA-256 verification
- [ ] Model download/import UI with integrity verification
- [ ] Generation queue and real progress
- [ ] Image-to-image
- [ ] LoRA inference
- [ ] Investigate on-device fine-tuning (experimental)
