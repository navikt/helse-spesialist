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
data class ApiOpptegnelse(
    val sekvensnummer: Int,
    val type: Type,
) {
    @Serializable
    enum class Type {
        UTBETALING_ANNULLERING_FEILET,
        UTBETALING_ANNULLERING_OK,
        FERDIGBEHANDLET_GODKJENNINGSBEHOV,
        NY_SAKSBEHANDLEROPPGAVE,
        REVURDERING_AVVIST,
        REVURDERING_FERDIGBEHANDLET,
        PERSONDATA_OPPDATERT,
        PERSON_KLAR_TIL_BEHANDLING,
    }
}
