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
data class ApiBehandletOppgaveProjeksjon(
    val id: Long,
    val personPseudoId: UUID,
    val ferdigstiltTidspunkt: LocalDateTime,
    val beslutter: String?,
    val saksbehandler: String?,
    val personnavn: ApiPersonnavn,
)

@Serializable
data class ApiBehandletOppgaveProjeksjonSide(
    val totaltAntall: Long,
    val sidetall: Int,
    val sidestoerrelse: Int,
    val elementer: List<ApiBehandletOppgaveProjeksjon>,
)
