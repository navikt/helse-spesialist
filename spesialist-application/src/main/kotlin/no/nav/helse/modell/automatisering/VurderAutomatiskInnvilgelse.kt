package no.nav.helse.modell.automatisering

import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

internal class VurderAutomatiskInnvilgelse(
    private val automatisering: Automatisering,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val godkjenningsbehov: GodkjenningsbehovData,
    private val automatiseringDao: AutomatiseringDao,
    private val oppgaveService: OppgaveService,
    private val vedtakRepository: VedtakRepository,
) : Command {
    private val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId
    private val utbetalingId = godkjenningsbehov.utbetalingId
    private val hendelseId = godkjenningsbehov.id

    private fun utfallslogger(
        tekst: String,
        problemer: List<String> = emptyList(),
    ) = loggInfo(
        tekst,
        "vedtaksperiodeId: $vedtaksperiodeId, utbetalingId: $utbetalingId, problemer: ${problemer.joinToString()}",
    )

    override fun execute(context: CommandContext): Boolean {
        val resultat =
            automatisering.utfør(
                fødselsnummer = godkjenningsbehov.fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetaling = utbetaling,
                periodetype = godkjenningsbehov.periodetype,
                sykefraværstilfelle = sykefraværstilfelle,
                organisasjonsnummer = godkjenningsbehov.organisasjonsnummer,
                yrkesaktivitetstype = godkjenningsbehov.yrkesaktivitetstype,
                maksdato = godkjenningsbehov.foreløpigBeregnetSluttPåSykepenger,
                tags = godkjenningsbehov.tags,
            )

        when (resultat) {
            is Automatiseringsresultat.KanIkkeAutomatiseres -> {
                utfallslogger("Behandler ikke perioden ferdig automatisk fordi: {}", resultat.problemer)
                manuellSaksbehandling(resultat.problemer)
            }

            is Automatiseringsresultat.Stikkprøve -> {
                utfallslogger("Behandler ikke perioden ferdig automatisk, plukket ut til stikkprøve for ${resultat.årsak}")
                loggInfo(
                    "Automatisk godkjenning avbrutt, sendes til manuell behandling",
                    "vedtaksperiodeId: $vedtaksperiodeId, utbetalingId: $utbetalingId",
                )
                stikkprøve()
            }

            is Automatiseringsresultat.KanAutomatiseres -> {
                utfallslogger("Behandler perioden ferdig automatisk")
                automatiserSaksbehandling(context)
                loggInfo("Automatisk godkjenning for vedtaksperiode", "vedtaksperiodeId: $vedtaksperiodeId")
                return ferdigstill(context)
            }
        }

        return true
    }

    private fun manuellSaksbehandling(problemer: List<String>) {
        automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId)
    }

    private fun stikkprøve() {
        automatiseringDao.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
    }

    private fun automatiserSaksbehandling(context: CommandContext) {
        val vedtak = Vedtak.automatisk(SpleisBehandlingId(godkjenningsbehov.spleisBehandlingId))
        vedtakRepository.lagre(vedtak)
        automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
        godkjenningMediator.automatiskUtbetaling(context, godkjenningsbehov)
        oppgaveService.avbrytOppgaveFor(vedtaksperiodeId)
    }
}
