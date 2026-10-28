@file:UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.util.UUID

@Serializable
data class ApiTotrinnsvurderingForPerson(
    val totrinnsvurdering: ApiTotrinnsvurdering?,
)

@Serializable
data class ApiTotrinnsvurdering(
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val erBeslutteroppgave: Boolean,
)
