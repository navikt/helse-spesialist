@file:UseContextualSerialization(
    LocalDate::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ApiPaVentForPerson(
    val paVent: ApiPaVent?,
)

@Serializable
data class ApiPaVent(
    val frist: LocalDate?,
    val oid: UUID,
)
