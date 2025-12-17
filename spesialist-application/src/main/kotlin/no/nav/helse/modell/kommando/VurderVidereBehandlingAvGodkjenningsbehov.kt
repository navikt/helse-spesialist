package no.nav.helse.modell.kommando

import no.nav.helse.db.MeldingDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggWarn
import java.util.UUID

internal class VurderVidereBehandlingAvGodkjenningsbehov(
    private val fødselsnummer: String,
    private val commandData: GodkjenningsbehovData,
    private val utbetalingDao: UtbetalingDao,
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val reservasjonDao: ReservasjonDao,
    private val vedtakDao: VedtakDao,
    private val meldingDao: MeldingDao,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val utbetalingId = commandData.utbetalingId
        val meldingId = commandData.id

        if (utbetalingDao.erUtbetalingForkastet(utbetalingId)) {
            loggInfo(
                "Ignorerer godkjenningsbehov med id: $meldingId for utbetalingId: $utbetalingId fordi utbetalingen er forkastet",
            )
            return ferdigstill(context)
        }

        if (vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            loggInfo("Ignorerer godkjenningsbehov for utbetalingId: $utbetalingId. Er allerede automatisk godkjent")
            loggWarn(
                "utbetalingId: $utbetalingId er allerede ferdig behandlet. " +
                    "Det var litt rart at dette kom inn, " +
                    "men det kan være normalt dersom behandlingen i Spesialist nettopp ble ferdig.",
            )
            return ferdigstill(context)
        }

        val oppgave = oppgaveRepository.finnSisteOppgaveForUtbetaling(utbetalingId) ?: return true

        oppgaveDao.oppdaterPekerTilGodkjenningsbehov(meldingId, utbetalingId)
        loggInfo("Oppdaterte peker til godkjenningsbehov for oppgave med utbetalingId=$utbetalingId til id=$meldingId")

        if (oppgave.tilstand is Oppgave.Invalidert) return true

        val harEndringerIGodkjenningsbehov = harEndringerIGodkjenningsbehov(oppgave.godkjenningsbehovId)

        if (oppgave.tilstand is Oppgave.Ferdigstilt) {
            if (!harEndringerIGodkjenningsbehov) {
                loggInfo("Ignorerer duplikat av godkjenningsbehov for utbetalingId=$utbetalingId. Er allerede ferdigstilt.")
                logg.warn(
                    "utbetalingId=$utbetalingId er allerede ferdig behandlet. " +
                        "Det var litt rart at dette kom inn, " +
                        "men det kan være normalt dersom behandlingen i Spesialist nettopp ble ferdig.",
                )
                return ferdigstill(context)
            }
            error("Endringer i godkjenningsbehov der oppgaven med oppgaveId=${oppgave.id} er ferdigstilt")
        }

        return if (harEndringerIGodkjenningsbehov) {
            val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer)
            if (tildeltSaksbehandler != null) {
                reservasjonDao.reserverPerson(tildeltSaksbehandler.oid, fødselsnummer)
                loggInfo(
                    "Reserverer person til ${tildeltSaksbehandler.oid} pga eksisterende tildeling",
                    "fødselsnummer: $fødselsnummer",
                )
            }
            loggInfo("Invaliderer oppgave med oppgaveId=${oppgave.id} pga endringer i godkjenningsbehovet")
            oppgaveDao.invaliderOppgave(oppgave.id)
            true
        } else {
            loggInfo("Ignorerer duplikat av godkjenningsbehov for utbetalingId=$utbetalingId")
            ferdigstill(context)
        }
    }

    private fun harEndringerIGodkjenningsbehov(godkjenningsbehovId: UUID): Boolean {
        val godkjenningsbehovData =
            (meldingDao.finn(godkjenningsbehovId) as? Godkjenningsbehov)?.data()
                ?: error("Fant ikke lagret godkjenningsbehov med id $godkjenningsbehovId")

        return harEndringerIGodkjenningsbehov(commandData, godkjenningsbehovData)
    }

    private fun harEndringerIGodkjenningsbehov(
        gammelt: GodkjenningsbehovData,
        nytt: GodkjenningsbehovData,
    ): Boolean =
        nytt.fødselsnummer != gammelt.fødselsnummer ||
            nytt.organisasjonsnummer != gammelt.organisasjonsnummer ||
            nytt.vedtaksperiodeId != gammelt.vedtaksperiodeId ||
            nytt.spleisVedtaksperioder.sortedBy { it.vedtaksperiodeId } != gammelt.spleisVedtaksperioder.sortedBy { it.vedtaksperiodeId } ||
            nytt.spleisBehandlingId != gammelt.spleisBehandlingId ||
            nytt.vilkårsgrunnlagId != gammelt.vilkårsgrunnlagId ||
            nytt.tags.sorted() != gammelt.tags.sorted() ||
            nytt.periodeFom != gammelt.periodeFom ||
            nytt.periodeTom != gammelt.periodeTom ||
            nytt.periodetype != gammelt.periodetype ||
            nytt.førstegangsbehandling != gammelt.førstegangsbehandling ||
            nytt.utbetalingtype != gammelt.utbetalingtype ||
            nytt.kanAvvises != gammelt.kanAvvises ||
            nytt.inntektskilde != gammelt.inntektskilde ||
            nytt.orgnummereMedRelevanteArbeidsforhold.sorted() != gammelt.orgnummereMedRelevanteArbeidsforhold.sorted() ||
            nytt.skjæringstidspunkt != gammelt.skjæringstidspunkt ||
            nytt.sykepengegrunnlagsfakta != gammelt.sykepengegrunnlagsfakta
}
