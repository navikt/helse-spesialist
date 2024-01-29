package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.meldinger.VedtaksperiodeHendelse
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao

internal class VedtaksperiodeNyUtbetaling(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    private val json: String,
    utbetalingDao: UtbetalingDao,
    gjeldendeGenerasjon: Generasjon
) : Kommandohendelse, VedtaksperiodeHendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettKoblingTilUtbetalingCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao,
        ),
        OpprettKoblingTilGenerasjonCommand(
            hendelseId = id,
            utbetalingId = utbetalingId,
            gjeldendeGenerasjon = gjeldendeGenerasjon
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json
}
