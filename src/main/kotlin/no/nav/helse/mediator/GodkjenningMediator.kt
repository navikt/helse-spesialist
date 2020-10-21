package no.nav.helse.mediator

import no.nav.helse.automatiseringsteller
import no.nav.helse.mediator.meldinger.VedtaksperiodeGodkjent
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.objectMapper
import java.util.*

internal class GodkjenningMediator(
    private val warningDao: WarningDao,
    private val vedtakDao: VedtakDao
) {
    internal fun automatiskUtbetaling(
        context: CommandContext,
        behov: UtbetalingsgodkjenningMessage,
        vedtaksperiodeId: UUID,
        fødselsnummer: String
    ) {
        behov.godkjennAutomatisk()
        context.publiser(behov.toJson())
        automatiseringsteller.inc()

        if (FeatureToggle.godkjenningsEvent) {
            context.publiser(
                objectMapper.writeValueAsString(
                    VedtaksperiodeGodkjent(
                        vedtaksperiodeId = vedtaksperiodeId,
                        fødselsnummer = fødselsnummer,
                        warnings = warningDao.finnWarnings(vedtaksperiodeId),
                        periodetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId)?.name ?: "Ukjent",
                        løsning = behov.løsning()
                    )
                )
            )
        }
    }
}
