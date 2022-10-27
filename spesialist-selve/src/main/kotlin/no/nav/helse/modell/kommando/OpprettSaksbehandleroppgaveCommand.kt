package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.delvisRefusjon
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetalingTilSykmeldt
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.slf4j.LoggerFactory

internal class OpprettSaksbehandleroppgaveCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveMediator: OppgaveMediator,
    private val automatisering: Automatisering,
    private val hendelseId: UUID,
    private val personDao: PersonDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val utbetalingId: UUID,
    private val utbetalingtype: Utbetalingtype,
    private val snapshotMediator: SnapshotMediator,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettSaksbehandleroppgaveCommand::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
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

            vedtaksperiodensUtbetaling.delvisRefusjon() -> Oppgave.delvisRefusjon(vedtaksperiodeId, utbetalingId)
            vedtaksperiodensUtbetaling.utbetalingTilSykmeldt() -> Oppgave.utbetalingTilSykmeldt(
                vedtaksperiodeId,
                utbetalingId
            )

            else -> Oppgave.søknad(vedtaksperiodeId, utbetalingId)
        }
        logg.info("Oppretter saksbehandleroppgave $oppgave")
        sikkerLogg.info("Oppretter saksbehandleroppgave $oppgave")
        oppgaveMediator.opprett(oppgave)
        return true
    }

    private val harFortroligAdressebeskyttelse
        get() =
            personDao.findAdressebeskyttelse(fødselsnummer) == Adressebeskyttelse.Fortrolig

    private val vedtaksperiodensUtbetaling by lazy { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) }
}
