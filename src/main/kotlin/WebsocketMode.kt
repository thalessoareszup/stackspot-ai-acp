package com.stackspot.labs.acpserver

import com.agentclientprotocol.agent.Agent
import com.agentclientprotocol.protocol.ProtocolOptions
import com.agentclientprotocol.transport.acpProtocolOnServerWebSocket
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.awaitCancellation

fun server() {
    embeddedServer(Netty, port = 9898) {
                install(WebSockets)

                routing {
                    acpProtocolOnServerWebSocket("/acp", ProtocolOptions()) {
                        try {
                            Agent(protocol = it, agentSupport = TerminalAgentSupport())
                            it.start()
                            awaitCancellation()
                        } catch (e: Exception) {
                            println(e)
                        }
                    }
                }
            }
            .start(wait = true)
}
