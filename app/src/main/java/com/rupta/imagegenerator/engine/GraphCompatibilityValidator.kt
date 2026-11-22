package com.rupta.imagegenerator.engine

import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo

/** Validates actual ONNX graph metadata against the model-pack contract before inference. */
object GraphCompatibilityValidator {
    data class GraphMetadata(
        val inputs: Map<String, TensorMetadata>,
        val outputs: Map<String, TensorMetadata>
    )
    data class TensorMetadata(val shape: LongArray, val dataType: String)

    fun inspect(session: OrtSession): GraphMetadata {
        fun collect(nodes: Map<String, ai.onnxruntime.NodeInfo>): Map<String, TensorMetadata> =
            nodes.mapValues { (_, node) ->
                val info = node.info as? TensorInfo
                    ?: error("Graph contains a non-tensor node.")
                TensorMetadata(info.shape, normalizeType(info.type.toString()))
            }
        return GraphMetadata(collect(session.inputInfo), collect(session.outputInfo))
    }

    fun validate(graphName: String, contract: GraphInterfaceContract, metadata: GraphMetadata) {
        validateSide(graphName, "inputs", contract.inputs, metadata.inputs)
        validateSide(graphName, "outputs", contract.outputs, metadata.outputs)
    }

    private fun validateSide(
        graphName: String,
        side: String,
        specs: List<TensorSpec>,
        actual: Map<String, TensorMetadata>
    ) {
        val expectedNames = specs.map { it.name }.toSet()
        require(actual.keys.containsAll(expectedNames)) {
            "$graphName graph is missing declared $side: ${expectedNames - actual.keys}"
        }
        require(actual.keys == expectedNames) {
            "$graphName graph has undeclared $side: ${actual.keys - expectedNames}"
        }
        specs.forEach { spec ->
            val tensor = actual.getValue(spec.name)
            require(spec.validateShape(tensor.shape)) {
                "$graphName $side tensor '${spec.name}' shape mismatch. Expected ${spec.dimensions}, got ${tensor.shape.toList()}."
            }
            require(normalizeType(spec.dataType) == normalizeType(tensor.dataType)) {
                "$graphName $side tensor '${spec.name}' data type mismatch. Expected ${spec.dataType}, got ${tensor.dataType}."
            }
        }
    }

    private fun normalizeType(type: String): String = when (type.uppercase().replace(" ", "")) {
        "FLOAT", "FLOAT32", "ONNXJAVATYPE.FLOAT" -> "float32"
        "FLOAT16", "ONNXJAVATYPE.FLOAT16" -> "float16"
        "INT32", "ONNXJAVATYPE.INT32" -> "int32"
        "INT64", "ONNXJAVATYPE.INT64" -> "int64"
        else -> type.lowercase()
    }

    fun validatePack(sessions: Map<String, OrtSession>, contract: DiffusionPackContract) {
        contract.graphInterfaces.forEach { (graphName, graphContract) ->
            val session = sessions[graphName] ?: error("Model pack is missing graph session: $graphName")
            validate(graphName, graphContract, inspect(session))
        }
    }
}
