package com.stackspot.labs.acpserver.stk

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

@Serializable
data class ChatRequest(
    val streaming: Boolean = true,
    @SerialName("user_prompt")
    val userPrompt: String,
    @SerialName("stackspot_knowledge")
    val stackspotKnowledge: Boolean = false,
    @SerialName("return_ks_in_response")
    val returnKsInResponse: Boolean = false
)

@Serializable
data class ChatResponse(
    val message: String? = null,
    @SerialName("stopReason")
    val stopReason: String? = null,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("token_type")
    val tokenType: String,
)

@Serializable
enum class AgentActionType {
    @SerialName("list-dir")
    ListDir,
    @SerialName("read-file")
    ReadFile,
    @SerialName("write-file")
    WriteFile,
    @SerialName("move-file")
    MoveFile,
    @SerialName("apply-patch")
    ApplyPatch,
    @SerialName("run-tests")
    RunTests,
    @SerialName("comment")
    Comment
}

data class ExpectedAgentAction(
    val type: AgentActionType,
    val target: String?,
    val payload: String?,
    val description: String?
)

data class ExpectedAgentOutput(
    val steps: List<ExpectedAgentAction>
)

const val MAX_AI_CHUNK_LOOP = 9999

class StackspotClient(
    private val clientId: String,
    private val clientSecret: String,
) {


    private var token: String? = null
    private var tokenExpiryEpochSec: Long = 0

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private suspend fun authenticate() {
        val now = Instant.now().epochSecond
        if (token != null && now < tokenExpiryEpochSec - 30) {
            // Token valid for at least 30 more seconds, reuse it
            return
        }

        val response: HttpResponse = client.submitForm(
            url = "https://idm.stackspot.com/stackspot/oidc/oauth/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("client_id", clientId)
                append("client_secret", clientSecret)
            })

        if (!response.status.isSuccess()) {
            throw RuntimeException("Failed to authenticate with StackSpot API: ${response.status}")
        }

        val tokenResponseText = response.bodyAsChannel().toInputStream().bufferedReader().readText()
        val tokenResponse = json.decodeFromString<TokenResponse>(tokenResponseText)
        token = tokenResponse.accessToken
        tokenExpiryEpochSec = Instant.now().epochSecond + tokenResponse.expiresIn
    }

    /**
     * Streams NDJSON responses from the prompt API as a Flow of ChatResponseChunk.
     */
    suspend fun prompt(p: String): ExpectedAgentOutput? {
        authenticate()
        val response =
            client.post("https://genai-inference-app.stackspot.com/v1/agent/01KAPJZS0FC8BFK7QRB2KZCVQY/chat") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(
                    ChatRequest(
                        userPrompt = p,
                        streaming = false,
                        stackspotKnowledge = false,
                        returnKsInResponse = false,
                    )
                )
            }

        val resp = response.body<ChatResponse>()
        if (resp.message == null) return null

        return json.decodeFromString<ExpectedAgentOutput>(resp.message)
    }
}
