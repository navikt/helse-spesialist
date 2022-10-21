package no.nav.helse.spesialist.api.oppgave.experimental

import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Oppgaver
import no.nav.helse.spesialist.api.graphql.schema.Paginering
import kotlin.math.ceil

/**
 * Ikke i bruk i produksjon per nå. For utforsking av server side paginering.
 */
class OppgaveService(
    private val oppgavePagineringDao: OppgavePagineringDao,
) {

    fun hentOppgaver(
        tilganger: SaksbehandlerTilganger,
        antall: Int,
        side: Int,
    ): Oppgaver {
        val oppgaverForSiden: List<OppgaveForOversiktsvisning> =
            oppgavePagineringDao.finnOppgaver(tilganger = tilganger, antall = antall, side = side)
        val totaltAntallOppgaver = getAntallOppgaver(tilganger)
        return Oppgaver(
            oppgaver = oppgaverForSiden,
            paginering = Paginering(
                side = side,
                elementerPerSide = oppgaverForSiden.size,
                antallSider = ceil(totaltAntallOppgaver.toDouble() / antall).toInt(),
            )
        )
    }

    private fun getAntallOppgaver(saksbehandlerTilganger: SaksbehandlerTilganger): Int =
        oppgavePagineringDao.getAntallOppgaver(saksbehandlerTilganger)

}
