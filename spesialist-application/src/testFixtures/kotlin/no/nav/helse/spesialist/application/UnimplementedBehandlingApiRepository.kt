package no.nav.helse.spesialist.application

import no.nav.helse.db.api.BehandlingApiRepository
import no.nav.helse.db.api.VedtaksperiodeDbDto

class UnimplementedBehandlingApiRepository : BehandlingApiRepository {
    override fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto> {
        TODO("Not yet implemented")
    }

    override fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto {
        TODO("Not yet implemented")
    }
}
