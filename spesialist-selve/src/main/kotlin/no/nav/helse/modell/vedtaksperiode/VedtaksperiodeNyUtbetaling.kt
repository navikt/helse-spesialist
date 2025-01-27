package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.person.Person
import java.util.UUID

class VedtaksperiodeNyUtbetaling(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
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
        sessionContext: SessionContext,
    ) {
        person.nyUtbetalingForVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        kommandostarter { vedtaksperiodeNyUtbetaling(this@VedtaksperiodeNyUtbetaling, sessionContext) }
    }

    override fun toJson(): String = json
}

internal class VedtaksperiodeNyUtbetalingCommand(
    vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    utbetalingDao: UtbetalingDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                utbetalingDao = utbetalingDao,
            ),
        )
}
