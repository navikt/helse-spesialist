package no.nav.helse

import java.util.UUID

interface AutomatiseringStansetSjekker {
    fun sjekkOmAutomatiseringErStanset(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): Boolean
}
