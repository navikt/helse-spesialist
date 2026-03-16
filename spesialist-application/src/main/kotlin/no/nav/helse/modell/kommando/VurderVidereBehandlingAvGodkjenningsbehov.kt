package no.nav.helse.modell.kommando

import no.nav.helse.db.MeldingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.util.UUID

internal class VurderVidereBehandlingAvGodkjenningsbehov(
    private val fødselsnummer: String,
    private val commandData: GodkjenningsbehovData,
    private val oppgaveRepository: OppgaveRepository,
    private val reservasjonDao: ReservasjonDao,
    private val vedtakDao: VedtakDao,
    private val meldingDao: MeldingDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val utbetalingId = commandData.utbetalingId
        val meldingId = commandData.id

        if (vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            loggInfo("Ignorerer godkjenningsbehov for utbetalingId: $utbetalingId. Er allerede automatisk godkjent")
            loggWarn(
                "utbetalingId: $utbetalingId er allerede ferdig behandlet. " +
                    "Det var litt rart at dette kom inn, " +
                    "men det kan være normalt dersom behandlingen i Spesialist nettopp ble ferdig.",
            )
            return ferdigstill(context)
        }

        val oppgave = oppgaveRepository.finn(SpleisBehandlingId(commandData.spleisBehandlingId)) ?: return true
        if (oppgave.tilstand is Oppgave.Invalidert) return true

        val gammelGodkjenningsbehovId = oppgave.godkjenningsbehovId
        oppgave.nyttGodkjenningsbehov(meldingId)
        oppgaveRepository.lagre(oppgave)
        loggInfo("Oppdaterte peker til godkjenningsbehov for oppgave med utbetalingId=$utbetalingId til id=$meldingId")

        val harEndringerIGodkjenningsbehov = harEndringerIGodkjenningsbehov(gammelGodkjenningsbehovId)

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
            error("Endringer i godkjenningsbehov der oppgaven med oppgaveId=${oppgave.id.value} er ferdigstilt")
        }

        if (harEndringerIGodkjenningsbehov) {
            val tildeltSaksbehandler = oppgave.tildeltTil
            if (tildeltSaksbehandler != null) {
                reservasjonDao.reserverPerson(tildeltSaksbehandler.value, fødselsnummer)
                loggInfo(
                    "Reserverer person til ${tildeltSaksbehandler.value} pga eksisterende tildeling",
                    "fødselsnummer" to fødselsnummer,
                )
            }
            loggInfo("Invaliderer oppgave med oppgaveId=${oppgave.id.value} pga endringer i godkjenningsbehovet")
            oppgave.avbryt()
            oppgaveRepository.lagre(oppgave)
        } else {
            loggInfo("Ignorerer duplikat av godkjenningsbehov for utbetalingId=$utbetalingId")
            ferdigstill(context)
        }
        return true
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
