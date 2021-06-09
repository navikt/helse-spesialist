package no.nav.helse.mediator

import no.nav.helse.automatiseringsteller
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeAvvist
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
        context.publiser(lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, behov).toJson())
    }

    internal fun saksbehandlerAvvisning(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        context.publiser(behov.toJson())
        context.publiser(lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, behov).toJson())
    }

    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        context.publiser(lagVedtaksperiodeGodkjent(vedtaksperiodeId, fødselsnummer, behov).toJson())
        automatiseringsteller.inc()
    }

    private fun lagVedtaksperiodeGodkjent(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        behov: UtbetalingsgodkjenningMessage
    ) = VedtaksperiodeGodkjent(
        vedtaksperiodeId = vedtaksperiodeId,
        fødselsnummer = fødselsnummer,
        warnings = warningDao.finnWarnings(vedtaksperiodeId).map { it.dto() },
        periodetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId),
        løsning = behov.løsning()
    )

    internal fun lagVedtaksperiodeAvvist(
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        behov: UtbetalingsgodkjenningMessage
    ) = VedtaksperiodeAvvist(
        vedtaksperiodeId = vedtaksperiodeId,
        fødselsnummer = fødselsnummer,
        warnings = warningDao.finnWarnings(vedtaksperiodeId).map { it.dto() },
        periodetype = vedtakDao.finnVedtakId(vedtaksperiodeId)
            ?.let { vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) },
        løsning = behov.løsning()
    )
}
