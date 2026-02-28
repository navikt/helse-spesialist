@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.sse

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiServerSentEvent(
    val event: Event,
    val data: Unit = Unit,
) {
    @Suppress("unused")
    // Verdiene er ikke i bruk internt i Spesialist, finnes
    // kun for å få typegenerering av events vi sender til Speil
    @Serializable
    enum class Event {
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
