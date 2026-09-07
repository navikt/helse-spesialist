package no.nav.helse.modell.automatisering

import io.opentelemetry.api.trace.Span
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class VurderAutomatiskInnvilgelse(
    private val automatisering: Automatisering,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val godkjenningsbehov: GodkjenningsbehovData,
    private val oppgaveService: OppgaveService,
    private val spleisBehandlingId: SpleisBehandlingId,
    private val identitetsnummer: Identitetsnummer,
) : Command {
    private val utbetalingId = godkjenningsbehov.utbetalingId
    private val hendelseId = godkjenningsbehov.id

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val gjeldendeBehandling =
            sessionContext.behandlingRepository.finn(spleisBehandlingId)
                ?: error("Fant ikke behandling med id $spleisBehandlingId")
        val behandlingspakke = sessionContext.behandlingRepository.finnBehandlingspakke(gjeldendeBehandling, identitetsnummer.value)
        val resultat =
            automatisering.utfør(
                fødselsnummer = godkjenningsbehov.fødselsnummer,
                vedtaksperiodeId = gjeldendeBehandling.vedtaksperiodeId,
                utbetaling = utbetaling,
                periodetype = godkjenningsbehov.periodetype,
                behandlingspakke = behandlingspakke,
                gjeldendeBehandling = gjeldendeBehandling,
                organisasjonsnummer = godkjenningsbehov.organisasjonsnummer,
                yrkesaktivitetstype = godkjenningsbehov.yrkesaktivitetstype,
                maksdato = godkjenningsbehov.foreløpigBeregnetSluttPåSykepenger,
                tags = godkjenningsbehov.tags,
            )

        when (resultat) {
            is Automatiseringsresultat.KanIkkeAutomatiseres -> {
                loggInfo(
                    "Behandler ikke perioden ferdig automatisk, den kan ikke automatiseres",
                    "vedtaksperiodeId" to gjeldendeBehandling.vedtaksperiodeId.value,
                    "utbetalingId" to utbetalingId,
                    "problemer" to resultat.problemer.joinToString(),
                )
                manuellSaksbehandling(sessionContext.automatiseringDao, resultat.problemer, gjeldendeBehandling.vedtaksperiodeId)
            }

            is Automatiseringsresultat.Stikkprøve -> {
                loggInfo(
                    "Behandler ikke perioden ferdig automatisk, plukket ut til stikkprøve for ${resultat.årsak}",
                    "vedtaksperiodeId" to gjeldendeBehandling.vedtaksperiodeId.value,
                    "utbetalingId" to utbetalingId,
                )
                stikkprøve(sessionContext.automatiseringDao, gjeldendeBehandling.vedtaksperiodeId)
            }

            is Automatiseringsresultat.KanAutomatiseres -> {
                loggInfo(
                    "Behandler perioden ferdig automatisk",
                    "vedtaksperiodeId" to gjeldendeBehandling.vedtaksperiodeId.value,
                    "utbetalingId" to utbetalingId,
                )
                automatiserSaksbehandling(commandContext, sessionContext, gjeldendeBehandling.vedtaksperiodeId, spleisBehandlingId)
                return ferdigstill(commandContext)
            }
        }

        return true
    }

    private fun manuellSaksbehandling(
        automatiseringDao: AutomatiseringDao,
        problemer: List<String>,
        vedtaksperiodeId: VedtaksperiodeId,
    ) {
        automatiseringDao.manuellSaksbehandling(problemer, vedtaksperiodeId.value, hendelseId, utbetalingId)
    }

    private fun stikkprøve(
        automatiseringDao: AutomatiseringDao,
        vedtaksperiodeId: VedtaksperiodeId,
    ) {
        automatiseringDao.stikkprøve(vedtaksperiodeId.value, hendelseId, utbetalingId)
    }

    private fun automatiserSaksbehandling(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        vedtaksperiodeId: VedtaksperiodeId,
        spleisBehandlingId: SpleisBehandlingId,
    ) {
        val vedtak =
            sessionContext.vedtakRepository.finn(spleisBehandlingId).let { vedtak ->
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

        sessionContext.vedtakRepository.lagre(vedtak)
        sessionContext.automatiseringDao.automatisert(vedtaksperiodeId.value, hendelseId, utbetalingId)
        godkjenningMediator.automatiskUtbetaling(commandContext, godkjenningsbehov)
        oppgaveService.avbrytOppgaveFor(vedtaksperiodeId.value)
        Span.current().setAttribute("speil.saksbehandling.spesialist", "vedtak_fattet_automatisk")
    }
}
