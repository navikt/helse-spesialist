package no.nav.helse.spesialist.api.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BigDecimalStringSerializer : KSerializer<java.math.BigDecimal> {
    override val descriptor =
        PrimitiveSerialDescriptor(java.math.BigDecimal::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: java.math.BigDecimal,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.math.BigDecimal = java.math.BigDecimal(decoder.decodeString())
}

object BooleanStrictSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN)

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean =
        when (val raw = decoder.decodeString()) {
            "true" -> true
            "false" -> false
            else -> throw SerializationException("Expected 'true' or 'false', got '$raw'")
        }
}

object InstantIsoSerializer : KSerializer<java.time.Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor(java.time.Instant::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: java.time.Instant,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.time.Instant = java.time.Instant.parse(decoder.decodeString())
}

object LocalDateIsoSerializer : KSerializer<java.time.LocalDate> {
    override val descriptor =
        PrimitiveSerialDescriptor(java.time.LocalDate::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: java.time.LocalDate,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.time.LocalDate = java.time.LocalDate.parse(decoder.decodeString())
}

object LocalDateTimeIsoSerializer : KSerializer<java.time.LocalDateTime> {
    override val descriptor =
        PrimitiveSerialDescriptor(java.time.LocalDateTime::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: java.time.LocalDateTime,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.time.LocalDateTime = java.time.LocalDateTime.parse(decoder.decodeString())
}

object UUIDStringSerializer : KSerializer<java.util.UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(java.util.UUID::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: java.util.UUID,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.util.UUID = java.util.UUID.fromString(decoder.decodeString())
}
