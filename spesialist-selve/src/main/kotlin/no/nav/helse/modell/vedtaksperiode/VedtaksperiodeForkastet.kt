package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import java.util.UUID

class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Vedtaksperiodemelding {
    internal constructor(jsonNode: JsonNode) : this(
        UUID.fromString(jsonNode["@id"].asText()),
        UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) {
        person.vedtaksperiodeForkastet(vedtaksperiodeId)
        kommandostarter { vedtaksperiodeForkastet(this@VedtaksperiodeForkastet, transactionalSession) }
    }

    override fun toJson() = json
}

class VedtaksperiodeForkastetCommand(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val id: UUID,
    commandContextRepository: CommandContextRepository,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    oppgaveDao: OppgaveDao,
    totrinnsvurderingService: TotrinnsvurderingService,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            AvbrytCommand(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                commandContextRepository = commandContextRepository,
                oppgaveService = oppgaveService,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                oppgaveDao = oppgaveDao,
                totrinnsvurderingService = totrinnsvurderingService,
            ),
        )
}
