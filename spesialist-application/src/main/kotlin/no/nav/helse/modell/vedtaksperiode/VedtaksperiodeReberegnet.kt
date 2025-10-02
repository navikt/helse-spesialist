package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.util.UUID

class VedtaksperiodeReberegnet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun toJson(): String = json

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun behandle(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiodeOrNull(vedtaksperiodeId)) { "Fant ikke vedtaksperiode med id: $vedtaksperiodeId" }
        kommandostarter { vedtaksperiodeReberegnet(this@VedtaksperiodeReberegnet, vedtaksperiode, sessionContext) }
    }
}

internal class VedtaksperiodeReberegnetCommand(
    fødselsnummer: String,
    vedtaksperiode: Vedtaksperiode,
    periodehistorikkDao: PeriodehistorikkDao,
    commandContextDao: CommandContextDao,
    oppgaveService: OppgaveService,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            VedtaksperiodeReberegnetPeriodehistorikk(
                vedtaksperiode = vedtaksperiode,
                periodehistorikkDao = periodehistorikkDao,
            ),
            AvbrytCommand(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId(),
                commandContextDao = commandContextDao,
                oppgaveService = oppgaveService,
                reservasjonDao = reservasjonDao,
                tildelingDao = tildelingDao,
                totrinnsvurderingRepository = totrinnsvurderingRepository,
            ),
        )
}
