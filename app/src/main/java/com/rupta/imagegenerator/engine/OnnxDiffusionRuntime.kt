package com.rupta.imagegenerator.engine

import android.graphics.Bitmap
import android.content.Context
import java.io.File

/**
 * Owns the loaded ONNX model sessions and exposes a concrete pipeline for supported pack contracts.
 * Packs must provide the graph keys required by the contract and graph filenames in their manifest.
 */
class OnnxDiffusionRuntime private constructor(
    private val pack: OnnxModelPack,
    private val tokenizerPack: LoadedTokenizerPack,
    private val textEncoder: OnnxTextEncoder,
    private val unet: OnnxUnetRunner,
    private val unetAdapter: StableDiffusionUnetAdapter,
    private val vae: OnnxVaeDecoder,
    private val contract: DiffusionPackContract,
    private val scheduler: DdimScheduler
) : AutoCloseable {
    fun generate(
        request: GenerationRequest,
        onProgress: (GenerationProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): Bitmap {
        val pipeline = DiffusionPipeline(
            tokenizer = tokenizerPack.tokenizer,
            encodeText = { ids ->
                val encoded = textEncoder.encode(ids)
                FloatTensorData(encoded.shape, encoded.values)
            },
            predictNoise = { sample, timestep, conditioning ->
                unetAdapter.predict(sample, timestep, conditioning)
            },
            decodeLatents = { latents -> vae.decode(latents) },
            latentChannels = contract.latentChannels,
            vaeScale = contract.vaeScale,
            scheduler = scheduler
        )
        return pipeline.generate(request, onProgress, isCancelled)
    }

    override fun close() = pack.close()

    companion object {
        fun load(context: Context, directory: File): OnnxDiffusionRuntime {
            val modelPack = OnnxModelPack.load(context, directory)
            try {
                val tokenizerPack = LoadedTokenizerPack.load(directory)
                val contract = tokenizerPack.contract
                val schedulerFile = File(directory, contract.schedulerConfig)
                require(schedulerFile.isFile && schedulerFile.canonicalFile.parentFile == directory.canonicalFile) {
                    "Scheduler configuration file missing or outside model pack."
                }
                val scheduler = DdimScheduler.fromConfig(schedulerFile)
                val graphKeys = contract.graphInterfaces.keys
                require(modelPack.sessions.keys.containsAll(graphKeys)) {
                    "Manifest must load every graph declared by the pipeline contract."
                }
                GraphCompatibilityValidator.validatePack(modelPack.sessions, contract)
                val textSession = modelPack.sessions.getValue("text_encoder")
                val unetSession = modelPack.sessions.getValue("unet")
                val vaeSession = modelPack.sessions.getValue("vae_decoder")
                val textEncoder = OnnxTextEncoder(
                    OrtEnvironmentHolder.environment(), textSession,
                    contract.graphInterfaces.getValue("text_encoder")
                )
                val unetRunner = OnnxUnetRunner(
                    OrtEnvironmentHolder.environment(), unetSession,
                    contract.graphInterfaces.getValue("unet")
                )
                val adapter = StableDiffusionUnetAdapter(unetRunner, contract.graphInterfaces.getValue("unet"))
                val decoder = OnnxVaeDecoder(
                    OrtEnvironmentHolder.environment(), vaeSession,
                    contract.graphInterfaces.getValue("vae_decoder")
                )
                return OnnxDiffusionRuntime(modelPack, tokenizerPack, textEncoder, unetRunner, adapter, decoder, contract, scheduler)
            } catch (t: Throwable) {
                modelPack.close()
                throw t
            }
        }
    }
}

/** Shared ONNX Runtime environment accessor to avoid duplicating initialization. */
private object OrtEnvironmentHolder {
    fun environment() = ai.onnxruntime.OrtEnvironment.getEnvironment()
}
