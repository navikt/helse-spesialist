package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.AvbrytOppgaveCommand
import no.nav.helse.modell.kommando.AvbrytTotrinnsvurderingCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.util.UUID

class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    val spleisBehandlingId: SpleisBehandlingId?,
    private val fødselsnummer: String,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        UUID.fromString(jsonNode["@id"].asText()),
        UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        UUID.fromString(jsonNode["behandlingId"].asText()).takeUnless { it == null }?.let { SpleisBehandlingId(it) },
        jsonNode["fødselsnummer"].asText(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun behandle(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        person.vedtaksperiodeForkastet(vedtaksperiodeId)
        kommandostarter {
            vedtaksperiodeForkastet(
                this@VedtaksperiodeForkastet,
                person.forkastedeVedtaksperiodeIder(),
                sessionContext,
            )
        }
    }

    override fun toJson() = json
}

class VedtaksperiodeForkastetCommand(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: SpleisBehandlingId?,
    val alleForkastedeVedtaksperiodeIder: List<UUID>,
    val oppgaveRepository: OppgaveRepository,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            AvbrytOppgaveCommand(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                vedtaksperiodeId = vedtaksperiodeId,
            ),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId),
            AvbrytTotrinnsvurderingCommand(
                fødselsnummer = fødselsnummer,
                alleForkastedeVedtaksperiodeIder = alleForkastedeVedtaksperiodeIder,
            ),
        )
}
