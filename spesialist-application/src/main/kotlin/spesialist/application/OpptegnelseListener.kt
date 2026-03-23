package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer

interface OpptegnelseListener {
    suspend fun onOpptegnelse(
        identitetsnummer: Identitetsnummer,
        block: suspend () -> Unit,
    )
}
