package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.ReservasjonRepository
import no.nav.helse.db.TildelingRepository
import no.nav.helse.db.UtbetalingRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import java.util.UUID

internal class VedtaksperiodeReberegnet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        json = packet.toJson(),
    )

    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        kommandostarter { vedtaksperiodeReberegnet(this@VedtaksperiodeReberegnet) }
    }
}

internal class VedtaksperiodeReberegnetCommand(
    vedtaksperiodeId: UUID,
    fødselsnummer: String,
    utbetalingRepository: UtbetalingRepository,
    periodehistorikkDao: PeriodehistorikkDao,
    commandContextRepository: CommandContextRepository,
    oppgaveService: OppgaveService,
    reservasjonRepository: ReservasjonRepository,
    tildelingRepository: TildelingRepository,
    oppgaveRepository: OppgaveRepository,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VedtaksperiodeReberegnetPeriodehistorikk(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingRepository = utbetalingRepository,
                periodehistorikkDao = periodehistorikkDao,
            ),
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonRepository = reservasjonRepository,
                tildelingRepository = tildelingRepository,
                oppgaveRepository = oppgaveRepository,
                totrinnsvurderingMediator = totrinnsvurderingMediator,
            ),
            AvbrytCommand(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                commandContextRepository = commandContextRepository,
                oppgaveService = oppgaveService,
                reservasjonRepository = reservasjonRepository,
                tildelingRepository = tildelingRepository,
                oppgaveRepository = oppgaveRepository,
                totrinnsvurderingMediator = totrinnsvurderingMediator,
            ),
        )
}
