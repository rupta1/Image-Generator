# Image Generator — Android Native AI Studio

A native Kotlin/Jetpack Compose Android app foundation for privacy-first, on-device image generation and local personalization.

## Target device
Initial target: POCO X6 Pro (Dimensity 8350-Ultimate, Mali-G615 MC6, 8 GB RAM). Hardware acceleration and model compatibility must be benchmarked on-device.

## Current milestone
The Create screen invokes the ONNX runtime wrapper for detected model packs, shows inference progress, supports cancellation, and displays a successful bitmap result in-session. Model-pack static preflight and graph metadata checks now run before pipeline use. Real model compatibility and actual output remain unverified on-device.

**Real end-to-end generation remains dependent on compatible user-supplied ONNX model packs.** Model binaries are not committed and generation is not simulated.

## Build
Open the project in Android Studio (current stable), allow Gradle sync, then run the app on an Android device.

## Local inference roadmap
- Validate real model-pack compatibility and output correctness on a physical device
- Benchmark NNAPI/GPU paths and memory use on the POCO X6 Pro
- Add generation queue and gallery persistence

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
- [x] Core DDIM denoising-loop orchestration with classifier-free guidance, progress callbacks and cancellation checks
- [x] Seeded latent-noise initialization and float32 VAE decoder foundation
- [x] Stable Diffusion UNet adapter mapping common sample, timestep and conditioning inputs
- [x] Pipeline coordinator for prompt encoding, seeded latents, denoising, decode callback, progress and cancellation
- [x] ONNX model-pack session runtime integration wrapper
- [x] Create-screen runtime invocation, progress/cancel controls and result preview
- [x] Safe ZIP model-pack installer with manifest and optional SHA-256 verification
- [x] Load supported linear DDIM scheduler configuration from pack metadata
- [x] Reject unsupported scheduler types and beta schedules with explicit errors
- [x] Validate ONNX graph names, tensor shapes and dtypes against pack contracts at runtime
- [x] Static model-pack preflight checks for required graph, contract, tokenizer and scheduler assets
- [ ] Validate real pack compatibility and device inference
- [ ] Generation queue and gallery persistence
- [ ] Image-to-image
- [ ] LoRA inference
- [ ] Investigate on-device fine-tuning (experimental)
