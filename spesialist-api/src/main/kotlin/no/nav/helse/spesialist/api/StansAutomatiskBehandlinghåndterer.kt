package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import java.time.LocalDateTime

interface StansAutomatiskBehandlinghåndterer {
    fun lagre(
        fødselsnummer: String,
        status: String,
        årsaker: Set<String>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    )

    fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning

    fun erUnntatt(fødselsnummer: String): Boolean
}
