package com.stackspot.labs.acpserver

import com.agentclientprotocol.agent.AgentInfo
import com.agentclientprotocol.agent.AgentSession
import com.agentclientprotocol.agent.AgentSupport
import com.agentclientprotocol.client.ClientInfo
import com.agentclientprotocol.common.Event
import com.agentclientprotocol.common.SessionParameters
import com.agentclientprotocol.model.AgentCapabilities
import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.LATEST_PROTOCOL_VERSION
import com.agentclientprotocol.model.McpCapabilities
import com.agentclientprotocol.model.PromptCapabilities
import com.agentclientprotocol.model.PromptResponse
import com.agentclientprotocol.model.SessionId
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.StopReason
import com.stackspot.labs.acpserver.stk.StackspotClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private class TerminalAgentSession(
        override val sessionId: SessionId,
        val workingDirectory: String,
        val stackspotClient: StackspotClient
) : AgentSession {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun prompt(content: List<ContentBlock>, _meta: JsonElement?): Flow<Event> =
            flow {
                val userText =
                        content.filterIsInstance<ContentBlock.Text>().joinToString(" ") { it.text }
                val reply = stackspotClient.prompt(userText)
                if (reply == null) {
                    emit(
                            Event.SessionUpdateEvent(
                                    SessionUpdate.AgentMessageChunk(
                                            ContentBlock.Text(
                                                    "Agent returned a weird response. Sorry :("
                                            )
                                    )
                            )
                    )
                    emit(Event.PromptResponseEvent(PromptResponse(StopReason.END_TURN)))
                } else {

                    // Finish the turn once updates are sent.
                    emit(Event.PromptResponseEvent(PromptResponse(StopReason.END_TURN)))
                }
            }

    override suspend fun cancel() {
        // No long-running work in this demo, so nothing to clean up yet.
    }
}

// 2. Implement AgentSupport: negotiate capabilities and build per-session handlers.
class TerminalAgentSupport : AgentSupport {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun initialize(clientInfo: ClientInfo): AgentInfo {
        logger.info("Initializing agent")
        return AgentInfo(
                protocolVersion = LATEST_PROTOCOL_VERSION,
                capabilities =
                        AgentCapabilities(
                                loadSession = true,
                                promptCapabilities =
                                        PromptCapabilities(
                                                audio = false,
                                                image = false,
                                                embeddedContext = true
                                        ),
                                mcpCapabilities = McpCapabilities(http = true)
                        )
        )
    }

    override suspend fun createSession(sessionParameters: SessionParameters): AgentSession {
        logger.info("Creating session")
        val sessionId = SessionId("session-${System.currentTimeMillis()}")
        val stkClient = stackspotClientFromEnv()
        return TerminalAgentSession(sessionId, sessionParameters.cwd, stkClient)
    }

    override suspend fun loadSession(
            sessionId: SessionId,
            sessionParameters: SessionParameters
    ): AgentSession {
        logger.info("Loading session")
        // Rehydrate existing sessions with the provided identifier.
        val stkClient = stackspotClientFromEnv()
        return TerminalAgentSession(sessionId, sessionParameters.cwd, stkClient)
    }
}

fun main(args: Array<String>) = runBlocking {
    val opts = try {
        optionsFromArgs(args.asList())
    } catch (e: com.stackspot.labs.acpserver.cli.HelpRequestedException) {
        return@runBlocking
    }
    LoggerFactory.getLogger("Main")
            .info("Running on {} mode", opts.executionMode.toString().lowercase())

    when (opts.executionMode) {
        ExecutionMode.STDIO -> stdio(this)
        ExecutionMode.WEBSOCKET -> server()
        ExecutionMode.PROXY -> proxy()
    }
}
