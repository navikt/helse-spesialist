package no.nav.helse.spesialist.api.plugins

import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.ContentNegotiationConfig
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.discard
import no.nav.helse.spesialist.api.objectMapper
import tools.jackson.databind.ObjectMapper

fun ContentNegotiationConfig.configureContentNegotiationPlugin() {
    register(ContentType.Application.Json, UnitFriendlyJacksonConverter(objectMapper))
}

private class UnitFriendlyJacksonConverter(
    objectMapper: ObjectMapper,
) : ContentConverter {
    // Serialiserer hele svaret før sending. Ved strømming pakker Jackson inn «Broken pipe» i DatabindException
    // etter at 200 er sendt, og Ktor logger det som ERROR i stedet for DEBUG.
    private val jacksonConverter = JacksonConverter(objectMapper, streamBody = false)

    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel,
    ): Any? {
        // Unit-body: les og forkast innholdet, uansett hva klienten sender (også "{}")
        if (typeInfo.type == Unit::class) {
            content.discard()
            return Unit
        }

        return jacksonConverter.deserialize(charset, typeInfo, content)
    }

    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?,
    ): OutgoingContent = jacksonConverter.serialize(contentType, charset, typeInfo, value)
}
