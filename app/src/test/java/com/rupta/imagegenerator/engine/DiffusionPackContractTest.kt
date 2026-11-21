package com.rupta.imagegenerator.engine

import org.junit.Assert.*
import org.junit.Test

class DiffusionPackContractTest {
    private val valid = """
        {
          "formatVersion":1,
          "modelFamily":"sd15",
          "latentChannels":4,
          "vaeScale":8,
          "maxTokenLength":77,
          "tokenizerFiles":{"vocab":"vocab.json","mergesOrConfig":"merges.txt"},
          "schedulerConfig":"scheduler.json",
          "graphInterfaces":{
            "text_encoder":{"inputs":[{"name":"input_ids","dimensions":[1,77],"dataType":"int64"}],"outputs":[{"name":"hidden_states","dimensions":[1,77,768],"dataType":"float32"}]},
            "unet":{"inputs":[{"name":"sample","dimensions":[-1,4,-1,-1],"dataType":"float32"},{"name":"timestep","dimensions":[1],"dataType":"float32"},{"name":"encoder_hidden_states","dimensions":[-1,77,768],"dataType":"float32"}],"outputs":[{"name":"noise_pred","dimensions":[-1,4,-1,-1],"dataType":"float32"}]},
            "vae_decoder":{"inputs":[{"name":"latent_sample","dimensions":[-1,4,-1,-1],"dataType":"float32"}],"outputs":[{"name":"image","dimensions":[-1,3,-1,-1],"dataType":"float32"}]}
          }
        }
    """.trimIndent()

    @Test fun parsesContractAndTensorShape() {
        val contract = DiffusionPackContract.parse(valid)
        assertEquals("sd15", contract.modelFamily)
        assertEquals(3, contract.graphInterfaces.size)
        val spec = contract.graphInterfaces.getValue("unet").inputs.first()
        assertTrue(spec.validateShape(longArrayOf(1,4,64,64)))
        assertFalse(spec.validateShape(longArrayOf(1,3,64,64)))
    }

    @Test fun rejectsUnsupportedFamily() {
        assertThrows(IllegalArgumentException::class.java) {
            DiffusionPackContract.parse(valid.replace("sd15", "unknown"))
        }
    }

    @Test fun rejectsUnsafeTokenizerFilename() {
        assertThrows(IllegalArgumentException::class.java) {
            DiffusionPackContract.parse(valid.replace("vocab.json", "../vocab.json"))
        }
    }

    @Test fun rejectsMissingGraphContract() {
        assertThrows(IllegalArgumentException::class.java) {
            DiffusionPackContract.parse(valid.replace("\"vae_decoder\":{", "\"decoder\":{"))
        }
    }
}
