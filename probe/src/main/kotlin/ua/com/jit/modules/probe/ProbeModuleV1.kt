package ua.com.jit.modules.probe

import ua.com.jit.module.runtime.ModuleEntrypointV1
import ua.com.jit.module.runtime.ModuleInstanceV1
import ua.com.jit.module.runtime.ModuleStartContextV1
import ua.com.jit.module.runtime.ModuleStartResultV1

object ProbeModuleV1 {
    const val STATUS: String = "PROBE_OK"
}

/**
 * First external JIT Module implementation.
 *
 * It has no UI, storage, network or JIT-Node source dependency. A successful start proves only the
 * minimal public runtime contract.
 */
class ProbeEntrypointV1 : ModuleEntrypointV1 {
    override fun start(context: ModuleStartContextV1): ModuleStartResultV1 =
        ModuleStartResultV1.Started(
            ProbeInstanceV1(context),
        )
}

class ProbeInstanceV1 internal constructor(
    val context: ModuleStartContextV1,
) : ModuleInstanceV1 {
    val status: String = ProbeModuleV1.STATUS

    var closed: Boolean = false
        private set

    override fun close() {
        closed = true
    }
}
