package ua.com.jit.modules.chat.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import ua.com.jit.module.runtime.ModuleDataAccessV1
import ua.com.jit.module.runtime.ModuleDataAppendResultV1
import ua.com.jit.module.runtime.ModuleDataEventHandleV1
import ua.com.jit.module.runtime.ModuleDataReadResultV1
import ua.com.jit.module.runtime.ModuleDataReaderV1
import ua.com.jit.module.runtime.ModuleDataRecordV1
import ua.com.jit.module.runtime.ModuleDataWriterV1
import ua.com.jit.module.runtime.ModuleInstallationHandleV1
import ua.com.jit.module.runtime.ModuleSpaceHandleV1
import ua.com.jit.module.runtime.ModuleStartContextV1
import ua.com.jit.module.runtime.ModuleStartResultV1
import ua.com.jit.module.runtime.ModuleTextCommandResultV1
import ua.com.jit.module.runtime.ModuleTextCommandV1

class ChatCliModuleV1Test {
    @Test
    fun sendThenListRoundtripsThroughPublicModuleDataApi() {
        val records = mutableListOf<ModuleDataRecordV1>()
        var eventCounter = 0
        val data = ModuleDataAccessV1(
            writer = ModuleDataWriterV1 { request ->
                val handle = ModuleDataEventHandleV1("chat-event-" + (++eventCounter))
                records += ModuleDataRecordV1(
                    event = handle,
                    operationType = request.operationType,
                    payload = request.payload,
                )
                ModuleDataAppendResultV1.Persisted(handle)
            },
            reader = ModuleDataReaderV1 { request ->
                ModuleDataReadResultV1.Loaded(
                    records.take(request.limit),
                )
            },
        )
        val context = ModuleStartContextV1(
            space = ModuleSpaceHandleV1("chat-space"),
            installation = ModuleInstallationHandleV1("chat-installation"),
            data = data,
        )

        val started = assertIs<ModuleStartResultV1.Started>(
            ChatCliEntrypointV1().start(context),
        )
        val chat = assertIs<ChatCliInstanceV1>(started.instance)

        val sent = assertIs<ModuleTextCommandResultV1.Success>(
            chat.execute(ModuleTextCommandV1("send hello from Chat")),
        )
        assertTrue(sent.output.value.startsWith("sent chat-event-1"))

        val listed = assertIs<ModuleTextCommandResultV1.Success>(
            chat.execute(ModuleTextCommandV1("list")),
        )
        assertTrue(listed.output.value.contains("hello from Chat"))
        assertEquals(1, records.size)
        assertEquals(ChatCliModuleV1.MESSAGE_OPERATION, records.single().operationType)
    }

    @Test
    fun startFailsWithoutModuleDataCapability() {
        val context = ModuleStartContextV1(
            space = ModuleSpaceHandleV1("chat-space"),
            installation = ModuleInstallationHandleV1("chat-installation"),
        )

        val failed = assertIs<ModuleStartResultV1.Failed>(
            ChatCliEntrypointV1().start(context),
        )

        assertEquals("chat-data-unavailable", failed.error.code.value)
    }

    @Test
    fun invalidCommandFailsClosed() {
        val data = ModuleDataAccessV1(
            writer = ModuleDataWriterV1 { error("unused") },
            reader = ModuleDataReaderV1 { error("unused") },
        )
        val started = assertIs<ModuleStartResultV1.Started>(
            ChatCliEntrypointV1().start(
                ModuleStartContextV1(
                    space = ModuleSpaceHandleV1("chat-space"),
                    installation = ModuleInstallationHandleV1("chat-installation"),
                    data = data,
                ),
            ),
        )
        val chat = assertIs<ChatCliInstanceV1>(started.instance)

        val failed = assertIs<ModuleTextCommandResultV1.Failed>(
            chat.execute(ModuleTextCommandV1("delete everything")),
        )

        assertEquals("chat-command-invalid", failed.errorCode.value)
    }
}
