package no.nav.helse.modell.kommando

import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.delvisRefusjon
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.bareUtbetalingTilSykmeldt
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.oppgave.Oppgave
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.person.Adressebeskyttelse
import org.slf4j.LoggerFactory
import java.util.*

internal class OpprettSaksbehandleroppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator,
    private val automatisering: Automatisering,
    private val hendelseId: UUID,
    private val personDao: PersonDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val utbetalingId: UUID,
    private val utbetalingtype: Utbetalingtype
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val oppgave = when {
            harFortroligAdressebeskyttelse -> Oppgave.fortroligAdressebeskyttelse(vedtaksperiodeId, utbetalingId)
            utbetalingtype == Utbetalingtype.REVURDERING -> Oppgave.revurdering(vedtaksperiodeId, utbetalingId)
            automatisering.erStikkprøve(vedtaksperiodeId, hendelseId) -> Oppgave.stikkprøve(
                vedtaksperiodeId,
                utbetalingId
            )
            risikovurderingDao.kreverSupersaksbehandler(vedtaksperiodeId) -> Oppgave.riskQA(
                vedtaksperiodeId,
                utbetalingId
            )
            vedtaksperiodensUtbetaling.bareUtbetalingTilSykmeldt() -> Oppgave.utbetalingTilSykmeldt(vedtaksperiodeId, utbetalingId)
            vedtaksperiodensUtbetaling.delvisRefusjon() -> Oppgave.delvisRefusjon(vedtaksperiodeId, utbetalingId)
            else -> Oppgave.søknad(vedtaksperiodeId, utbetalingId)
        }
        logg.info("Oppretter saksbehandleroppgave på utbetalingId $utbetalingId og vedtaksperiodeId $vedtaksperiodeId")
        oppgaveMediator.opprett(oppgave)
        return true
    }

    private val harFortroligAdressebeskyttelse get() =
        personDao.findPersoninfoAdressebeskyttelse(fødselsnummer) == Adressebeskyttelse.Fortrolig
    private val vedtaksperiodensUtbetaling get () = personDao.findVedtaksperiodeUtbetalingElement(fødselsnummer, utbetalingId)
}
