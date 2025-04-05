package no.nav.helse.db.api

interface BehandlingApiRepository {
    fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto>

    fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto
}
