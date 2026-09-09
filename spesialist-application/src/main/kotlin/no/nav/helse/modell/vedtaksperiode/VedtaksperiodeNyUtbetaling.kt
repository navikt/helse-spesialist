package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import tools.jackson.databind.JsonNode
import java.util.*

class VedtaksperiodeNyUtbetaling(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asString()),
        fødselsnummer = jsonNode["fødselsnummer"].asString(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asString()),
        utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asString()),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId)
        val utbetalingId = UtbetalingId(utbetalingId)
        val gjeldendeBehandling = sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiodeId) ?: error("Fant ikke behandling")
        gjeldendeBehandling.nyUtbetaling(utbetalingId)
        sessionContext.behandlingRepository.lagre(gjeldendeBehandling)

        kommandostarter {
            VedtaksperiodeNyUtbetalingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
            )
        }
    }

    override fun toJson(): String = json
}

internal class VedtaksperiodeNyUtbetalingCommand(
    vedtaksperiodeId: VedtaksperiodeId,
    utbetalingId: UtbetalingId,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
            ),
        )
}
