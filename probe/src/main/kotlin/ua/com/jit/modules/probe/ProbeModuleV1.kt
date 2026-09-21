package ua.com.jit.modules.probe

import ua.com.jit.module.runtime.ModuleDataAppendRequestV1
import ua.com.jit.module.runtime.ModuleDataAppendResultV1
import ua.com.jit.module.runtime.ModuleDataOperationTypeIdV1
import ua.com.jit.module.runtime.ModuleDataPayloadV1
import ua.com.jit.module.runtime.ModuleDataReadRequestV1
import ua.com.jit.module.runtime.ModuleDataReadResultV1
import ua.com.jit.module.runtime.ModuleDataStreamIdV1
import ua.com.jit.module.runtime.ModuleEntrypointV1
import ua.com.jit.module.runtime.ModuleInstanceV1
import ua.com.jit.module.runtime.ModuleStartContextV1
import ua.com.jit.module.runtime.ModuleStartErrorCodeV1
import ua.com.jit.module.runtime.ModuleStartErrorV1
import ua.com.jit.module.runtime.ModuleStartResultV1

object ProbeModuleV1 {
    const val STATUS: String = "PROBE_OK"
    const val DATA_STATUS: String = "PROBE_DATA_OK"

    val DATA_STREAM = ModuleDataStreamIdV1("probe")
    val DATA_OPERATION = ModuleDataOperationTypeIdV1("roundtrip")
    val DATA_PAYLOAD: ByteArray
        get() = "probe-data-v1".encodeToByteArray()
}

/**
 * First external JIT Module implementation.
 *
 * With no data service it proves the minimal runtime contract. When the host exposes Module data,
 * startup additionally proves an append/read roundtrip using only the public runtime API.
 */
class ProbeEntrypointV1 : ModuleEntrypointV1 {
    override fun start(context: ModuleStartContextV1): ModuleStartResultV1 {
        val data = context.data
            ?: return ModuleStartResultV1.Started(
                ProbeInstanceV1(context, ProbeModuleV1.STATUS),
            )

        val appended = when (
            val result = data.writer.append(
                ModuleDataAppendRequestV1(
                    streamId = ProbeModuleV1.DATA_STREAM,
                    operationType = ProbeModuleV1.DATA_OPERATION,
                    payload = ModuleDataPayloadV1(ProbeModuleV1.DATA_PAYLOAD),
                ),
            )
        ) {
            is ModuleDataAppendResultV1.Persisted -> result
            is ModuleDataAppendResultV1.Failed ->
                return failed("probe-data-append-failed")
        }

        val loaded = when (
            val result = data.reader.read(
                ModuleDataReadRequestV1(
                    streamId = ProbeModuleV1.DATA_STREAM,
                ),
            )
        ) {
            is ModuleDataReadResultV1.Loaded -> result
            is ModuleDataReadResultV1.Failed ->
                return failed("probe-data-read-failed")
        }

        val expectedPayload = ProbeModuleV1.DATA_PAYLOAD
        val found = loaded.records.any { record ->
            record.event == appended.event &&
                record.operationType == ProbeModuleV1.DATA_OPERATION &&
                record.payload.bytes.contentEquals(expectedPayload)
        }
        if (!found) {
            return failed("probe-data-roundtrip-failed")
        }

        return ModuleStartResultV1.Started(
            ProbeInstanceV1(context, ProbeModuleV1.DATA_STATUS),
        )
    }

    private fun failed(code: String): ModuleStartResultV1.Failed =
        ModuleStartResultV1.Failed(
            ModuleStartErrorV1(
                code = ModuleStartErrorCodeV1(code),
            ),
        )
}

class ProbeInstanceV1 internal constructor(
    val context: ModuleStartContextV1,
    val status: String,
) : ModuleInstanceV1 {
    var closed: Boolean = false
        private set

    override fun close() {
        closed = true
    }
}
