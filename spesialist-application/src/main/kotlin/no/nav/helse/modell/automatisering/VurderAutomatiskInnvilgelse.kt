package no.nav.helse.modell.automatisering

import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.application.logg.logg
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

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
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
                loggInfo(
                    "Behandler ikke perioden ferdig automatisk, den kan ikke automatiseres",
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "utbetalingId" to utbetalingId,
                    "problemer" to resultat.problemer.joinToString(),
                )
                manuellSaksbehandling(resultat.problemer)
            }

            is Automatiseringsresultat.Stikkprøve -> {
                loggInfo(
                    "Behandler ikke perioden ferdig automatisk, plukket ut til stikkprøve for ${resultat.årsak}",
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "utbetalingId" to utbetalingId,
                )
                stikkprøve()
            }

            is Automatiseringsresultat.KanAutomatiseres -> {
                loggInfo(
                    "Behandler perioden ferdig automatisk",
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "utbetalingId" to utbetalingId,
                )
                automatiserSaksbehandling(commandContext)
                return ferdigstill(commandContext)
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

    private fun automatiserSaksbehandling(commandContext: CommandContext) {
        val spleisBehandlingId = SpleisBehandlingId(godkjenningsbehov.spleisBehandlingId)
        val vedtak =
            vedtakRepository.finn(spleisBehandlingId).let { vedtak ->
                when (vedtak?.behandletAvSpleis) {
                    null -> {
                        Vedtak.automatisk(spleisBehandlingId)
                    }

                    true -> {
                        logg.info("Det er allerede fattet vedtak for behandlingen, og spleis har behandlet det")
                        return
                    }

                    false -> {
                        logg.info("Det er tidligere forsøkt å fatte vedtak for behandlingen, men spesialist har ikke sett at spleis har behandlet svaret på godkjenningsbehovet")
                        vedtak
                    }
                }
            }

        vedtakRepository.lagre(vedtak)
        automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
        godkjenningMediator.automatiskUtbetaling(commandContext, godkjenningsbehov)
        oppgaveService.avbrytOppgaveFor(vedtaksperiodeId)
    }
}
