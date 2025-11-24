package com.stackspot.labs.acpserver

import com.agentclientprotocol.agent.Agent
import com.agentclientprotocol.agent.AgentSupport
import com.agentclientprotocol.protocol.Protocol
import com.agentclientprotocol.transport.StdioTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun stdio(parentScope: CoroutineScope, agentSupport: AgentSupport = TerminalAgentSupport()) {
    val transport =
            StdioTransport(
                    parentScope = parentScope,
                    ioDispatcher = Dispatchers.IO,
                    input = System.`in`.asSource().buffered(),
                    output = System.out.asSink().buffered()
            )
    val protocol = Protocol(parentScope, transport)
    Agent(protocol = protocol, agentSupport = agentSupport)
    protocol.start()
}
