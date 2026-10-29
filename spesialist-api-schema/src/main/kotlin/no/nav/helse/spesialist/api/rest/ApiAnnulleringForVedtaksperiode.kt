@file:UseContextualSerialization(
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiAnnulleringForVedtaksperiode(
    val annullering: ApiAnnullering?,
)

@Serializable
data class ApiAnnullering(
    val saksbehandlerIdent: String,
    val arbeidsgiverFagsystemId: String?,
    val personFagsystemId: String?,
    val tidspunkt: LocalDateTime,
    val arsaker: List<String>,
    val begrunnelse: String?,
    val vedtaksperiodeId: UUID,
)
