package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_3
import java.time.LocalDateTime
import java.util.UUID

class ÅpneGosysOppgaverløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val antall: Int?,
    private val oppslagFeilet: Boolean,
) {
    internal fun lagre(åpneGosysOppgaverDao: ÅpneGosysOppgaverDao) {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                opprettet = opprettet,
            ),
        )
    }

    internal fun evaluer(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        varslerForOppslagFeilet(vedtaksperiodeId, sykefraværstilfelle)
        varslerForÅpneGosysOppgaver(vedtaksperiodeId, sykefraværstilfelle, harTildeltOppgave, oppgaveService)
    }

    private fun varslerForOppslagFeilet(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
    ) {
        if (oppslagFeilet) {
            sykefraværstilfelle.håndter(SB_EX_3.nyttVarsel(vedtaksperiodeId))
        } else {
            sykefraværstilfelle.deaktiver(SB_EX_3.nyttVarsel(vedtaksperiodeId))
        }
    }

    private fun varslerForÅpneGosysOppgaver(
        vedtaksperiodeId: UUID,
        sykefraværstilfelle: Sykefraværstilfelle,
        harTildeltOppgave: Boolean,
        oppgaveService: OppgaveService,
    ) {
        if (antall == null) return

        when {
            antall > 0 -> {
                sykefraværstilfelle.håndter(SB_EX_1.nyttVarsel(vedtaksperiodeId))
                if (sykefraværstilfelle.harKunGosysvarsel(vedtaksperiodeId)) {
                    oppgaveService.leggTilGosysEgenskap(vedtaksperiodeId)
                }
            }

            antall == 0 && !harTildeltOppgave -> {
                oppgaveService.fjernGosysEgenskap(vedtaksperiodeId)
                sykefraværstilfelle.deaktiver(SB_EX_1.nyttVarsel(vedtaksperiodeId))
            }
        }
    }
}
