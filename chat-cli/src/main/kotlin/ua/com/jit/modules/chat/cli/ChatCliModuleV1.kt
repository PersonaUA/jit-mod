package ua.com.jit.modules.chat.cli

import ua.com.jit.module.runtime.ModuleDataAccessV1
import ua.com.jit.module.runtime.ModuleDataAppendRequestV1
import ua.com.jit.module.runtime.ModuleDataAppendResultV1
import ua.com.jit.module.runtime.ModuleDataOperationTypeIdV1
import ua.com.jit.module.runtime.ModuleDataPayloadV1
import ua.com.jit.module.runtime.ModuleDataReadRequestV1
import ua.com.jit.module.runtime.ModuleDataReadResultV1
import ua.com.jit.module.runtime.ModuleDataStreamIdV1
import ua.com.jit.module.runtime.ModuleEntrypointV1
import ua.com.jit.module.runtime.ModuleStartContextV1
import ua.com.jit.module.runtime.ModuleStartErrorCodeV1
import ua.com.jit.module.runtime.ModuleStartErrorV1
import ua.com.jit.module.runtime.ModuleStartResultV1
import ua.com.jit.module.runtime.ModuleTextCommandErrorCodeV1
import ua.com.jit.module.runtime.ModuleTextCommandInstanceV1
import ua.com.jit.module.runtime.ModuleTextCommandOutputV1
import ua.com.jit.module.runtime.ModuleTextCommandResultV1
import ua.com.jit.module.runtime.ModuleTextCommandV1

object ChatCliModuleV1 {
    const val MAX_MESSAGE_BYTES: Int = 4 * 1024
    const val LIST_LIMIT: Int = 10

    val MESSAGE_STREAM = ModuleDataStreamIdV1("messages")
    val MESSAGE_OPERATION = ModuleDataOperationTypeIdV1("message")
}

class ChatCliEntrypointV1 : ModuleEntrypointV1 {
    override fun start(context: ModuleStartContextV1): ModuleStartResultV1 {
        val data = context.data
            ?: return ModuleStartResultV1.Failed(
                ModuleStartErrorV1(
                    ModuleStartErrorCodeV1("chat-data-unavailable"),
                ),
            )

        return ModuleStartResultV1.Started(
            ChatCliInstanceV1(data),
        )
    }
}

class ChatCliInstanceV1 internal constructor(
    private val data: ModuleDataAccessV1,
) : ModuleTextCommandInstanceV1 {
    private var closed = false

    override fun execute(
        command: ModuleTextCommandV1,
    ): ModuleTextCommandResultV1 {
        if (closed) return failed("chat-closed")

        val text = command.value.trim()
        return when {
            text == "help" -> success("send <message>\nlist")
            text == "list" -> listMessages()
            text.startsWith("send ") -> sendMessage(text.removePrefix("send ").trim())
            else -> failed("chat-command-invalid")
        }
    }

    override fun close() {
        closed = true
    }

    private fun sendMessage(message: String): ModuleTextCommandResultV1 {
        if (message.isEmpty()) return failed("chat-message-empty")
        val bytes = message.encodeToByteArray()
        if (bytes.size > ChatCliModuleV1.MAX_MESSAGE_BYTES) {
            return failed("chat-message-too-large")
        }

        return when (
            val result = data.writer.append(
                ModuleDataAppendRequestV1(
                    streamId = ChatCliModuleV1.MESSAGE_STREAM,
                    operationType = ChatCliModuleV1.MESSAGE_OPERATION,
                    payload = ModuleDataPayloadV1(bytes),
                ),
            )
        ) {
            is ModuleDataAppendResultV1.Persisted ->
                success("sent " + result.event.value)
            is ModuleDataAppendResultV1.Failed ->
                failed("chat-write-failed")
        }
    }

    private fun listMessages(): ModuleTextCommandResultV1 {
        val loaded = when (
            val result = data.reader.read(
                ModuleDataReadRequestV1(
                    streamId = ChatCliModuleV1.MESSAGE_STREAM,
                    limit = ChatCliModuleV1.LIST_LIMIT,
                ),
            )
        ) {
            is ModuleDataReadResultV1.Loaded -> result
            is ModuleDataReadResultV1.Failed ->
                return failed("chat-read-failed")
        }

        val lines = ArrayList<String>()
        for (record in loaded.records) {
            if (record.operationType != ChatCliModuleV1.MESSAGE_OPERATION) continue
            val message = try {
                record.payload.bytes.decodeToString(throwOnInvalidSequence = true)
            } catch (_: Exception) {
                return failed("chat-message-invalid-utf8")
            }
            lines += record.event.value + "\t" + message
        }

        return success(
            if (lines.isEmpty()) "(empty)" else lines.joinToString("\n"),
        )
    }

    private fun success(output: String): ModuleTextCommandResultV1.Success =
        ModuleTextCommandResultV1.Success(
            ModuleTextCommandOutputV1(output),
        )

    private fun failed(code: String): ModuleTextCommandResultV1.Failed =
        ModuleTextCommandResultV1.Failed(
            ModuleTextCommandErrorCodeV1(code),
        )
}
