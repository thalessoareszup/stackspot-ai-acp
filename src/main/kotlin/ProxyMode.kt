package com.stackspot.labs.acpserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.readLine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Receives input/output like [stdio], but pipes to a websocket server. This is useful for local
 * development, where the ACP client talks to the proxy, and you can debug/redeploy the server with
 * [server] transport.
 */
suspend fun proxy() {
    val logger: Logger = LoggerFactory.getLogger("AcpProxy")
    val wsUrl = "ws://localhost:9898/acp"

    logger.info("Creating HttpClient")
    val client = HttpClient(CIO) { install(WebSockets) { pingInterval = Duration.parse("1s") } }

    logger.info("Starting webSocket session")

    client.webSocket(urlString = wsUrl) {
        logger.info("WebSocket connected")

        coroutineScope {
            // Read from stdin in separate coroutine
            val stdinChannel = Channel<String>(Channel.UNLIMITED)

            launch(Dispatchers.IO) {
                logger.info("Stdin reader started")
                val reader = System.`in`.bufferedReader()
                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        logger.info("Read from stdin: $line")
                        stdinChannel.send(line)
                    }
                } finally {
                    stdinChannel.close()
                    logger.info("Stdin reader closed")
                }
            }

            // Send to WebSocket
            launch {
                logger.info("WebSocket sender started")
                for (line in stdinChannel) {
                    logger.info("Sending to WebSocket: $line")
                    send(Frame.Text(line))
                }
                logger.info("WebSocket sender finished")
                close(CloseReason(CloseReason.Codes.NORMAL, "stdin closed"))
            }

            // Receive from WebSocket
            launch {
                logger.info("WebSocket receiver started")
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            logger.info("Received from WebSocket: $text")
                            println(text)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Receiver error: ${e.message}")
                }
                logger.info("WebSocket receiver finished")
            }
        }
    }

    client.close()
    logger.info("Client closed")
}
