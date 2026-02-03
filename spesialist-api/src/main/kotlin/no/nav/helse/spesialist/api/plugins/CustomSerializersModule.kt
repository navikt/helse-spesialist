package no.nav.helse.spesialist.api.plugins

import kotlinx.serialization.modules.SerializersModule
import no.nav.helse.spesialist.api.serialization.BigDecimalStringSerializer
import no.nav.helse.spesialist.api.serialization.BooleanStrictSerializer
import no.nav.helse.spesialist.api.serialization.InstantIsoSerializer
import no.nav.helse.spesialist.api.serialization.LocalDateIsoSerializer
import no.nav.helse.spesialist.api.serialization.LocalDateTimeIsoSerializer
import no.nav.helse.spesialist.api.serialization.UUIDStringSerializer
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

val customSerializersModule =
    SerializersModule {
        contextual(BigDecimal::class, BigDecimalStringSerializer)
        contextual(Boolean::class, BooleanStrictSerializer)
        contextual(Instant::class, InstantIsoSerializer)
        contextual(LocalDate::class, LocalDateIsoSerializer)
        contextual(LocalDateTime::class, LocalDateTimeIsoSerializer)
        contextual(UUID::class, UUIDStringSerializer)
    }
