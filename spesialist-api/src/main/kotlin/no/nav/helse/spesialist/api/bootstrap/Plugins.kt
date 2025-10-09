package no.nav.helse.spesialist.api.bootstrap

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.AuthScheme
import io.github.smiley4.ktoropenapi.config.AuthType
import io.github.smiley4.ktoropenapi.config.OpenApiPluginConfig
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.github.smiley4.ktoropenapi.config.SchemaOverwriteModule
import io.github.smiley4.schemakenerator.swagger.data.RefType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.websocket.WebSockets
import io.swagger.v3.oas.models.media.Schema
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import no.nav.helse.spesialist.api.objectMapper
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.math.BigDecimal
import java.util.UUID

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
private val logg = LoggerFactory.getLogger("SpesialistApp")

internal fun Application.installPlugins(eksponerOpenApi: Boolean) {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate {
            UUID.randomUUID().toString()
        }
    }
    install(WebSockets)
    install(StatusPages) { configureStatusPages() }
    install(CallLogging) {
        disableDefaultColors()
        logger = sikkerlogg
        level = Level.INFO
        callIdMdc("callId")
        filter { call ->
            call.request.path().let { it.startsWith("/graphql") || it.startsWith("/ws/") || it.startsWith("/api/") }
        }
    }
    install(DoubleReceive)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    requestResponseTracing(sikkerlogg)
    if (eksponerOpenApi) {
        install(OpenApi) { configureOpenApi() }
    }
}

private fun OpenApiPluginConfig.configureOpenApi() {
    pathFilter = { _, url -> url.firstOrNull() == "api" }

    schemas {
        generator =
            SchemaGenerator.reflection {
                referencePath = RefType.OPENAPI_SIMPLE
                overwrite(SchemaGenerator.TypeOverwrites.JavaUuid())
                overwrite(SchemaGenerator.TypeOverwrites.Instant())
                overwrite(SchemaGenerator.TypeOverwrites.LocalDateTime())
                overwrite(SchemaGenerator.TypeOverwrites.LocalDate())
                overwrite(
                    object : SchemaOverwriteModule(
                        identifier = BigDecimal::class.qualifiedName!!,
                        schema = {
                            Schema<Any>().also {
                                it.types = setOf("string")
                                it.format = "bigdecimal"
                            }
                        },
                    ) {},
                )
            }
    }

    security {
        securityScheme("JWT") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
            bearerFormat = "JWT"
        }
        defaultSecuritySchemeNames("JWT")
    }
}

fun StatusPagesConfig.configureStatusPages() {
    exception<Modellfeil> { call: ApplicationCall, modellfeil: Modellfeil ->
        modellfeil.logger()
        call.respond(status = modellfeil.httpkode, message = modellfeil.tilFeilDto())
    }
    exception<Throwable> { call, cause ->
        val uri = call.request.uri
        val verb = call.request.httpMethod.value
        logg.error("Unhandled: $verb", cause)
        sikkerlogg.error("Unhandled: $verb - $uri", cause)
        call.respondText(
            text = "Det skjedde en uventet feil",
            status = HttpStatusCode.InternalServerError,
        )
        call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
    }
}
