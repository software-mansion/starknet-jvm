package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.ComputationResources
import com.swmansion.starknet.extensions.toNumAsHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

// TODO: (#344) Remove this workaround serializer once ExecutionResources conform to the RPC spec on Pathfinder side.
internal object ComputationResourcesSerializer : KSerializer<ComputationResources> {
    override fun deserialize(decoder: Decoder): ComputationResources {
        // This accepts both integer and NUM_AS_HEX values, as a temporary workaround until this is fixed on Pathfinder side.
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonInput for ${decoder::class}")

        val jsonObject = input.decodeJsonElement().jsonObject

        // TODO (#344): This shoulde be a mandatory field, but it's not on Pathfinder side.
        // val steps = getAsInt(jsonObject, "steps") ?: throw SerializationException("Input element does not contain mandatory field 'steps'")
        val steps = getAsInt(jsonObject, "steps") ?: 0
        val memoryHoles = getAsInt(jsonObject, "memory_holes")
        val rangeCheckApplications = getAsInt(jsonObject, "range_check_builtin_applications")
        val pedersenApplications = getAsInt(jsonObject, "pedersen_builtin_applications")
        val poseidonApplications = getAsInt(jsonObject, "poseidon_builtin_applications")
        val ecOpApplications = getAsInt(jsonObject, "ec_op_builtin_applications")
        val ecdsaApplications = getAsInt(jsonObject, "ecdsa_builtin_applications")
        val bitwiseApplications = getAsInt(jsonObject, "bitwise_builtin_applications")
        val keccakApplications = getAsInt(jsonObject, "keccak_builtin_applications")
        val segmentArenaApplications = getAsInt(jsonObject, "segment_arena_builtin")

        return ComputationResources(
            steps = steps,
            memoryHoles = memoryHoles,
            rangeCheckApplications = rangeCheckApplications,
            pedersenApplications = pedersenApplications,
            poseidonApplications = poseidonApplications,
            ecOpApplications = ecOpApplications,
            ecdsaApplications = ecdsaApplications,
            bitwiseApplications = bitwiseApplications,
            keccakApplications = keccakApplications,
            segmentArenaApplications = segmentArenaApplications,
        )
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ExecutionResources", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ComputationResources) {
        throw SerializationException("Class used for deserialization only.")
    }

    private fun getAsInt(jsonObject: JsonObject, key: String): Int? {
        return jsonObject.getOrDefault(key, null)?.jsonPrimitive?.contentOrNull?.let { fromHexOrInt(it) }
    }

    private fun fromHexOrInt(value: String): Int {
        return value.toBigIntegerOrNull()?.toInt() ?: value.toNumAsHex.value.toInt()
    }
}
