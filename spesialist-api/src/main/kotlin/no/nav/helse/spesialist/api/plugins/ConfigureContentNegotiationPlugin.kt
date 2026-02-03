package no.nav.helse.spesialist.api.plugins

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import no.nav.helse.spesialist.api.objectMapper

fun ContentNegotiationConfig.configureContentNegotiationPlugin() {
    register(ContentType.Application.Json, UnitFriendlyJacksonConverter(objectMapper))
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
