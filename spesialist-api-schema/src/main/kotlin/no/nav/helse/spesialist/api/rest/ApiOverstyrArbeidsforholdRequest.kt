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
data class ApiOverstyrArbeidsforholdRequest(
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<Arbeidsforhold>,
) {
    @Serializable
    data class Arbeidsforhold(
        val organisasjonsnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String,
        val lovhjemmel: ApiLovhjemmel?,
    )
}
