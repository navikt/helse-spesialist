package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.schema.ApiUnntattFraAutomatiskGodkjenning

interface StansAutomatiskBehandlinghåndterer {
    fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): ApiUnntattFraAutomatiskGodkjenning
}
