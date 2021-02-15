package no.nav.helse.mediator

import no.nav.helse.automatiseringsteller
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeGodkjent
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import java.util.*

internal class GodkjenningMediator(
    private val warningDao: WarningDao,
    private val vedtakDao: VedtakDao
) {
    internal fun saksbehandlerUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        context.publiser(behov.toJson())
        publiserVedtaksperiodeGodkjent(context, vedtaksperiodeId, fødselsnummer, behov)
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        automatiseringsteller.inc()
        publiserVedtaksperiodeGodkjent(context, vedtaksperiodeId, fødselsnummer, behov)
    }

    internal fun makstidOppnådd(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        behov.makstidOppnådd(listOf("Makstid oppnådd"))
        context.publiser(behov.toJson())
        publiserVedtaksperiodeGodkjent(context, vedtaksperiodeId, fødselsnummer, behov)
    }

    private fun publiserVedtaksperiodeGodkjent(
        context: CommandContext,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        behov: UtbetalingsgodkjenningMessage
    ) {
        context.publiser(
            VedtaksperiodeGodkjent(
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer,
                warnings = warningDao.finnWarnings(vedtaksperiodeId).map { it.dto() },
                periodetype = requireNotNull(vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId)),
                løsning = behov.løsning()
            ).toJson()
        )
    }
}
