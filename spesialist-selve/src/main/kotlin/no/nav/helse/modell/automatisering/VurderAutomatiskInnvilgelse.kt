package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.AutomatiseringRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.vedtaksperiode.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderAutomatiskInnvilgelse(
    private val automatisering: Automatisering,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val godkjenningsbehov: GodkjenningsbehovData,
    private val automatiseringRepository: AutomatiseringRepository,
    private val oppgaveService: OppgaveService,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskInnvilgelse::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId
    private val utbetalingId = godkjenningsbehov.utbetalingId
    private val hendelseId = godkjenningsbehov.id

    private fun utfallslogger(
        tekst: String,
        problemer: List<String> = emptyList(),
    ) = sikkerlogg.info(
        tekst,
        keyValue("vedtaksperiodeId", vedtaksperiodeId),
        keyValue("utbetalingId", utbetalingId),
        problemer,
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
            )

        when (resultat) {
            is Automatiseringsresultat.KanIkkeAutomatiseres -> {
                utfallslogger("Automatiserer ikke {} ({}) fordi: {}", resultat.problemer)
                manuellSaksbehandling(resultat.problemer)
            }
            is Automatiseringsresultat.Stikkprøve -> {
                utfallslogger("Automatiserer ikke {} ({}), plukket ut til stikkprøve for ${resultat.årsak}")
                logg.info(
                    "Automatisk godkjenning av {} avbrutt, sendes til manuell behandling",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId),
                )
                stikkprøve()
            }
            is Automatiseringsresultat.KanAutomatiseres -> {
                utfallslogger("Automatiserer {} ({})")
                automatiserSaksbehandling(context)
                logg.info("Automatisk godkjenning for vedtaksperiode $vedtaksperiodeId")
                return ferdigstill(context)
            }
        }

        return true
    }

    private fun manuellSaksbehandling(problemer: List<String>) {
        automatiseringRepository.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId)
    }

    private fun stikkprøve() {
        automatiseringRepository.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId)
    }

    private fun automatiserSaksbehandling(context: CommandContext) {
        automatiseringRepository.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
        godkjenningMediator.automatiskUtbetaling(context, godkjenningsbehov, utbetaling)
        oppgaveService.avbrytOppgaveFor(vedtaksperiodeId)
    }
}
