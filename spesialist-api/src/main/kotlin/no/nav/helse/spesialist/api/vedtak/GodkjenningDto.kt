package no.nav.helse.spesialist.api.vedtak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spesialist.api.graphql.mutation.Avslag

@JsonIgnoreProperties(ignoreUnknown = true)
data class GodkjenningDto(
    val oppgavereferanse: Long,
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val avslag: Avslag? = null,
) {
    init {
        if (!godkjent) requireNotNull(årsak)
    }
}
