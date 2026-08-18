package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.*
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import tools.jackson.databind.JsonNode
import java.util.*

class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    val spleisBehandlingId: SpleisBehandlingId?,
    private val fødselsnummer: String,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        UUID.fromString(jsonNode["@id"].asString()),
        UUID.fromString(jsonNode["vedtaksperiodeId"].asString()),
        UUID.fromString(jsonNode["behandlingId"].asString()).takeUnless { it == null }?.let { SpleisBehandlingId(it) },
        jsonNode["fødselsnummer"].asString(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun behandleMedLegacyPerson(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        person.vedtaksperiodeForkastet(vedtaksperiodeId)
        kommandostarter {
            VedtaksperiodeForkastetCommand(
                fødselsnummer = fødselsnummer(),
                vedtaksperiodeId = vedtaksperiodeId(),
                spleisBehandlingId = spleisBehandlingId,
                alleForkastedeVedtaksperiodeIder = person.forkastedeVedtaksperiodeIder(),
                oppgaveRepository = sessionContext.oppgaveRepository,
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
            ikkesuspenderendeCommand("opprettOpptegnelse") { sessionContext, _ ->
                sessionContext.opptegnelseRepository.lagre(
                    Opptegnelse.ny(
                        identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                        type = Opptegnelse.Type.PERSONDATA_OPPDATERT,
                    ),
                )
            },
        )
}
