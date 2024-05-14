package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning

interface StansAutomatiskBehandlinghåndterer {
    fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning

    fun erUnntatt(fødselsnummer: String): Boolean
}
