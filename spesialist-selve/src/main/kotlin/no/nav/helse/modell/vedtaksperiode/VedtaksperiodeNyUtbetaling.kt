package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.meldinger.VedtaksperiodemeldingOld
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.rapids_rivers.JsonMessage

internal class VedtaksperiodeNyUtbetaling private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    private val json: String
) : VedtaksperiodemeldingOld {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        json = packet.toJson()
    )
    internal constructor(jsonNode: JsonNode): this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        json = jsonNode.toString()
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json
}

internal class VedtaksperiodeNyUtbetalingCommand(
    id: UUID,
    vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    gjeldendeGenerasjon: Generasjon,
    utbetalingDao: UtbetalingDao,
): MacroCommand() {
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
}
