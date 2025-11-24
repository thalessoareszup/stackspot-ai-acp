package com.stackspot.labs.acpserver

import com.stackspot.labs.acpserver.cli.Command
import com.stackspot.labs.acpserver.stk.StackspotClient
import java.lang.RuntimeException

class CliArgumentMissingException(
        val argName: String,
) : RuntimeException()

const val STK_ACPSERVER_CLIENT_ID_ENV_KEY = "STK_ACPSERVER_CLIENT_ID"
const val STK_ACPSERVER_CLIENT_SECRET_ENV_KEY = "STK_ACPSERVER_CLIENT_SECRET"

fun stackspotClientFromEnv(): StackspotClient {
    val clientId =
            System.getenv(STK_ACPSERVER_CLIENT_ID_ENV_KEY)
                    ?: throw CliArgumentMissingException(STK_ACPSERVER_CLIENT_ID_ENV_KEY)
    val clientSecret =
            System.getenv(STK_ACPSERVER_CLIENT_SECRET_ENV_KEY)
                    ?: throw CliArgumentMissingException(STK_ACPSERVER_CLIENT_SECRET_ENV_KEY)
    return StackspotClient(clientId, clientSecret)
}

data class ProgramOptions(
        val executionMode: ExecutionMode,
)

fun optionsFromArgs(args: List<String>): ProgramOptions {
    val cmd = Command("acp-server", "StackSpot ACP Server")
    val mode by cmd.option("-m", "--mode", default = "stdio", description = "Execution mode")

    try {
        cmd.parse(args)
    } catch (e: com.stackspot.labs.acpserver.cli.HelpRequestedException) {
        cmd.printHelp()
        throw e
    }

    return ProgramOptions(executionMode = ExecutionMode.fromString(mode))
}

enum class ExecutionMode {
    STDIO,
    WEBSOCKET,
    PROXY;

    companion object {
        fun fromString(s: String): ExecutionMode =
                when (s.lowercase()) {
                    "stdio" -> STDIO
                    "websocket" -> WEBSOCKET
                    "proxy" -> PROXY
                    else -> throw IllegalArgumentException("Unknown transportType $s")
                }
    }
}
