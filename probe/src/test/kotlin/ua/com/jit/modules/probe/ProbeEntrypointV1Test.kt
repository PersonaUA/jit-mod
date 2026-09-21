package ua.com.jit.modules.probe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import ua.com.jit.module.runtime.ModuleDataAccessV1
import ua.com.jit.module.runtime.ModuleDataAppendRequestV1
import ua.com.jit.module.runtime.ModuleDataAppendResultV1
import ua.com.jit.module.runtime.ModuleDataEventHandleV1
import ua.com.jit.module.runtime.ModuleDataPayloadV1
import ua.com.jit.module.runtime.ModuleDataReadRequestV1
import ua.com.jit.module.runtime.ModuleDataReadResultV1
import ua.com.jit.module.runtime.ModuleDataReaderV1
import ua.com.jit.module.runtime.ModuleDataRecordV1
import ua.com.jit.module.runtime.ModuleDataWriterV1
import ua.com.jit.module.runtime.ModuleInstallationHandleV1
import ua.com.jit.module.runtime.ModuleSpaceHandleV1
import ua.com.jit.module.runtime.ModuleStartContextV1
import ua.com.jit.module.runtime.ModuleStartResultV1

class ProbeEntrypointV1Test {
    @Test
    fun probeStartsThroughPublicRuntimeApiOnly() {
        val context = ModuleStartContextV1(
            space = ModuleSpaceHandleV1("space-probe-test"),
            installation = ModuleInstallationHandleV1("installation-probe-test"),
        )

        val result = ProbeEntrypointV1().start(context)

        val started = assertIs<ModuleStartResultV1.Started>(result)
        val probe = assertIs<ProbeInstanceV1>(started.instance)

        assertEquals(ProbeModuleV1.STATUS, probe.status)
        assertEquals(context, probe.context)
        assertFalse(probe.closed)

        probe.close()
        assertTrue(probe.closed)
    }

    @Test
    fun probeUsesOnlyPublicDataServicesForRoundtrip() {
        val persisted = ModuleDataEventHandleV1("probe-event-1")
        var appendedRequest: ModuleDataAppendRequestV1? = null
        var readRequest: ModuleDataReadRequestV1? = null

        val writer = ModuleDataWriterV1 { request ->
            appendedRequest = request
            ModuleDataAppendResultV1.Persisted(persisted)
        }
        val reader = ModuleDataReaderV1 { request ->
            readRequest = request
            ModuleDataReadResultV1.Loaded(
                listOf(
                    ModuleDataRecordV1(
                        event = persisted,
                        operationType = ProbeModuleV1.DATA_OPERATION,
                        payload = ModuleDataPayloadV1(ProbeModuleV1.DATA_PAYLOAD),
                    ),
                ),
            )
        }
        val context = ModuleStartContextV1(
            space = ModuleSpaceHandleV1("space-probe-test"),
            installation = ModuleInstallationHandleV1("installation-probe-test"),
            data = ModuleDataAccessV1(writer, reader),
        )

        val result = ProbeEntrypointV1().start(context)

        val started = assertIs<ModuleStartResultV1.Started>(result)
        val probe = assertIs<ProbeInstanceV1>(started.instance)
        assertEquals(ProbeModuleV1.DATA_STATUS, probe.status)
        assertEquals(ProbeModuleV1.DATA_STREAM, appendedRequest?.streamId)
        assertEquals(ProbeModuleV1.DATA_OPERATION, appendedRequest?.operationType)
        assertTrue(
            appendedRequest?.payload?.bytes?.contentEquals(ProbeModuleV1.DATA_PAYLOAD) == true,
        )
        assertEquals(ProbeModuleV1.DATA_STREAM, readRequest?.streamId)
    }
}
