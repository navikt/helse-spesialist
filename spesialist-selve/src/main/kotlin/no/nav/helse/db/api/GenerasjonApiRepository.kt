package no.nav.helse.db.api

interface GenerasjonApiRepository {
    fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto>

    fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto
}
