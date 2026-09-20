package ua.com.jit.modules.probe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
}
