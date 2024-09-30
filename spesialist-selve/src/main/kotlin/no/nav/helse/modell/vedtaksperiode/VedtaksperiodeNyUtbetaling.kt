package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.UtbetalingRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class VedtaksperiodeNyUtbetaling private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        person.nyUtbetalingForVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        kommandostarter { vedtaksperiodeNyUtbetaling(this@VedtaksperiodeNyUtbetaling) }
    }

    override fun toJson(): String = json
}

internal class VedtaksperiodeNyUtbetalingCommand(
    vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    utbetalingRepository: UtbetalingRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                utbetalingRepository = utbetalingRepository,
            ),
        )
}
