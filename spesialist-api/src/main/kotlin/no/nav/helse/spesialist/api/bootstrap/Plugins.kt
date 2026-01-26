package no.nav.helse.spesialist.api.bootstrap

import com.fasterxml.jackson.databind.ObjectMapper
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
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
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
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.websocket.WebSockets
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import io.swagger.v3.oas.models.media.Schema
import kotlinx.serialization.modules.SerializersModule
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.serialization.BigDecimalStringSerializer
import no.nav.helse.spesialist.api.serialization.BooleanStrictSerializer
import no.nav.helse.spesialist.api.serialization.InstantIsoSerializer
import no.nav.helse.spesialist.api.serialization.LocalDateIsoSerializer
import no.nav.helse.spesialist.api.serialization.LocalDateTimeIsoSerializer
import no.nav.helse.spesialist.api.serialization.UUIDStringSerializer
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.teamLogs
import org.slf4j.event.Level
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
        logger = teamLogs
        level = Level.INFO
        callIdMdc("callId")
        filter { call ->
            call.request.path().let { it.startsWith("/graphql") || it.startsWith("/ws/") || it.startsWith("/api/") }
        }
    }
    install(DoubleReceive)
    install(ContentNegotiation) {
        register(ContentType.Application.Json, UnitFriendlyJacksonConverter(objectMapper))
    }
    requestResponseTracing(teamLogs)
    if (eksponerOpenApi) {
        install(OpenApi) { configureOpenApi() }
    }
    install(Resources) { serializersModule = customSerializersModule }
}

private val customSerializersModule =
    SerializersModule {
        contextual(BigDecimal::class, BigDecimalStringSerializer)
        contextual(Boolean::class, BooleanStrictSerializer)
        contextual(Instant::class, InstantIsoSerializer)
        contextual(LocalDate::class, LocalDateIsoSerializer)
        contextual(LocalDateTime::class, LocalDateTimeIsoSerializer)
        contextual(UUID::class, UUIDStringSerializer)
    }

private fun OpenApiPluginConfig.configureOpenApi() {
    pathFilter = { _, url -> url.firstOrNull() == "api" }
    autoDocumentResourcesRoutes = true
    schemas {
        generator =
            SchemaGenerator.kotlinx {
                serializersModule = customSerializersModule
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
        loggThrowable("Unhandled: $verb", uri, cause)
        call.respondText(
            text = "Det skjedde en uventet feil",
            status = HttpStatusCode.InternalServerError,
        )
        call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
    }
}

private class UnitFriendlyJacksonConverter(
    private val objectMapper: ObjectMapper,
) : ContentConverter {
    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel,
    ): Any? {
        // Håndter Unit som body eksplisitt
        if (typeInfo.type == Unit::class) {
            if (content.isClosedForRead) return Unit

            val text = content.readRemaining().readText()
            if (text.isBlank()) return Unit
            return Unit // til og med om noen sender "{}"
        }

        return JacksonConverter(objectMapper).deserialize(charset, typeInfo, content)
    }

    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?,
    ): OutgoingContent = JacksonConverter(objectMapper).serialize(contentType, charset, typeInfo, value)
}
