@file:kotlinx.serialization.UseContextualSerialization(
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiSaksbehandlerhandling(
    val handlingId: UUID,
    val handlingType: ApiHandlingType,
    val saksbehandlerIdent: String,
    val tidspunkt: LocalDateTime,
    val skjæringstidspunkt: LocalDate?,
    val behandlingId: UUID?,
    val erDelAvAktivTotrinnsvurdering: Boolean,
)

@Serializable
enum class ApiHandlingType {
    MANUELT_VURDERT_OPPTJENING,
}
