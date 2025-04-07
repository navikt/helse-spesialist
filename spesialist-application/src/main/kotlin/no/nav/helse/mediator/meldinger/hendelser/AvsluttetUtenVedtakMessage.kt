package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import java.util.UUID

class AvsluttetUtenVedtakMessage(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        spleisBehandlingId = UUID.fromString(jsonNode["behandlingId"].asText()),
        json = jsonNode.toString(),
    )

    private val avsluttetUtenVedtak get() =
        AvsluttetUtenVedtak(
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
        )

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun toJson(): String = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
        syncPersonTilDatabase: () -> Unit,
    ) = person.avsluttetUtenVedtak(avsluttetUtenVedtak)
}
