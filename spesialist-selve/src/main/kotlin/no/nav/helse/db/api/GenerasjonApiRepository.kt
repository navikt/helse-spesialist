package no.nav.helse.db.api

import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode

interface GenerasjonApiRepository {
    fun perioderTilBehandling(oppgaveId: Long): Set<Vedtaksperiode>

    fun periodeTilGodkjenning(oppgaveId: Long): Vedtaksperiode
}
