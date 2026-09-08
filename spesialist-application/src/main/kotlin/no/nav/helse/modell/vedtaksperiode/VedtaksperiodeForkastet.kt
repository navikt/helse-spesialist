package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.*
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
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

    override fun behandle(
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        val forkastedeVedtaksperiodeIder =
            sessionContext.legacyPersonRepository.brukPerson(fødselsnummer) {
                this.vedtaksperiodeForkastet(vedtaksperiodeId)
                this.forkastedeVedtaksperiodeIder()
            }

        kommandostarter {
            VedtaksperiodeForkastetCommand(
                identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
                vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
                spleisBehandlingId = spleisBehandlingId,
                alleForkastedeVedtaksperiodeIder = forkastedeVedtaksperiodeIder,
            )
        }
    }

    override fun toJson() = json
}

class VedtaksperiodeForkastetCommand(
    val identitetsnummer: Identitetsnummer,
    val vedtaksperiodeId: VedtaksperiodeId,
    val spleisBehandlingId: SpleisBehandlingId?,
    val alleForkastedeVedtaksperiodeIder: List<UUID>,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            AvbrytOppgaveCommand(
                identitetsnummer = identitetsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
            ),
            AvbrytContextCommand(vedtaksperiodeId = vedtaksperiodeId),
            AvbrytTotrinnsvurderingCommand(
                identitetsnummer = identitetsnummer,
                alleForkastedeVedtaksperiodeIder = alleForkastedeVedtaksperiodeIder,
            ),
            ikkesuspenderendeCommand("opprettOpptegnelse") { sessionContext, _ ->
                sessionContext.opptegnelseRepository.lagre(
                    Opptegnelse.ny(
                        identitetsnummer = identitetsnummer,
                        type = Opptegnelse.Type.PERSONDATA_OPPDATERT,
                    ),
                )
            },
        )
}
