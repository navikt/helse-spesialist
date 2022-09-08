package no.nav.helse.spesialist.api.oppgave.experimental

import java.time.LocalDateTime
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.oppgave.OppgaveForOversiktsvisningDto
import kotlin.math.ceil

/**
 * Ikke i bruk i produksjon per nå. For utforsking av server side paginering.
 */
class OppgaveService(
    private val oppgavePagineringDao: OppgavePagineringDao,
) {

    fun hentOppgaver(
        tilganger: SaksbehandlerTilganger,
        fra: LocalDateTime?,
        antall: Int
    ): Paginering<OppgaveForOversiktsvisningDto, LocalDateTime> {
        val oppgaverForSiden: List<PaginertOppgave> =
            oppgavePagineringDao.finnOppgaver(tilganger, fra, antall)
        val totaltAntallOppgaver = getAntallOppgaver(tilganger)
        return Paginering(
            elementer = oppgaverForSiden.map { it.oppgave },
            peker = oppgaverForSiden.last().oppgave.opprettet,
            sidestørrelse = oppgaverForSiden.size,
            nåværendeSide = ceil(oppgaverForSiden.first().radnummer.toDouble() / antall).toInt(),
            totaltAntallSider = ceil(totaltAntallOppgaver.toDouble() / antall).toInt(),
        )
    }

    private fun getAntallOppgaver(saksbehandlerTilganger: SaksbehandlerTilganger): Int =
        oppgavePagineringDao.getAntallOppgaver(saksbehandlerTilganger)

}

data class Paginering<V, P>(
    val elementer: List<V>,
    val peker: P,
    val sidestørrelse: Int,
    val nåværendeSide: Int,
    val totaltAntallSider: Int,
)
