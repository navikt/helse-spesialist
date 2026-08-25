@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiOverstyrInntektOgRefusjonRequest(
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<Arbeidsgiver>,
) {
    @Serializable
    data class Arbeidsgiver(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<Refusjonselement>?,
        val fraRefusjonsopplysninger: List<Refusjonselement>?,
        val begrunnelse: String,
        val forklaring: String,
        val lovhjemmel: ApiLovhjemmel?,
        val fom: LocalDate?,
        val tom: LocalDate?,
    )

    @Serializable
    data class Refusjonselement(
        val fom: LocalDate,
        val tom: LocalDate?,
        val beløp: Double,
    )
}
