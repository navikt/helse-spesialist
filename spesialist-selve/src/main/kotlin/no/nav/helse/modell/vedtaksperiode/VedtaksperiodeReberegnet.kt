package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.ReservasjonRepository
import no.nav.helse.db.TildelingRepository
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class VedtaksperiodeReberegnet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    internal constructor(packet: JsonMessage) : this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
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
        transactionalSession: TransactionalSession,
    ) {
        val vedtaksperiode = person.vedtaksperiodeOrNull(vedtaksperiodeId)
        checkNotNull(vedtaksperiode)
        kommandostarter { vedtaksperiodeReberegnet(this@VedtaksperiodeReberegnet, vedtaksperiode, transactionalSession) }
    }
}

internal class VedtaksperiodeReberegnetCommand(
    fødselsnummer: String,
    vedtaksperiode: Vedtaksperiode,
    periodehistorikkDao: PeriodehistorikkDao,
    commandContextRepository: CommandContextRepository,
    oppgaveService: OppgaveService,
    reservasjonRepository: ReservasjonRepository,
    tildelingRepository: TildelingRepository,
    oppgaveDao: OppgaveDao,
    totrinnsvurderingService: TotrinnsvurderingService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VedtaksperiodeReberegnetPeriodehistorikk(
                vedtaksperiode = vedtaksperiode,
                periodehistorikkDao = periodehistorikkDao,
            ),
            ReserverPersonHvisTildeltCommand(
                fødselsnummer = fødselsnummer,
                reservasjonRepository = reservasjonRepository,
                tildelingRepository = tildelingRepository,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingService = totrinnsvurderingService,
            ),
            AvbrytCommand(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId(),
                commandContextRepository = commandContextRepository,
                oppgaveService = oppgaveService,
                reservasjonRepository = reservasjonRepository,
                tildelingRepository = tildelingRepository,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingService = totrinnsvurderingService,
            ),
        )
}
