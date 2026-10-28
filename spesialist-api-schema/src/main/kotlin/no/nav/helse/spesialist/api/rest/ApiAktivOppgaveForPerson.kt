@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ApiAktivOppgaveForPerson(
    val aktivOppgave: ApiAktivOppgave?,
)

@Serializable
data class ApiAktivOppgave(
    val id: String,
    val vedtaksperiodeId: UUID,
)
