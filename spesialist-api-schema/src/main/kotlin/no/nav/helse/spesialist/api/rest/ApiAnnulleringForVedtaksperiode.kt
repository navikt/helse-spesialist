package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.rest.saksbilde.ApiAnnullering

@Serializable
data class ApiAnnulleringForVedtaksperiode(
    val annullering: ApiAnnullering?,
)
