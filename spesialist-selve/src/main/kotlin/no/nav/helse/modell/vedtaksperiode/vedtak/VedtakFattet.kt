package no.nav.helse.modell.vedtaksperiode.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class VedtakFattet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
    private val spleisBehandlingId: UUID,
) : Vedtaksperiodemelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = packet["behandlingId"].asUUID(),
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = jsonNode["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = jsonNode["behandlingId"].asUUID(),
        json = jsonNode.toString(),
    )

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    internal fun spleisBehandlingId() = spleisBehandlingId

    override fun toJson(): String = json

    internal fun erRelevantFor(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId

    internal fun doFinally(vedtakDao: VedtakDao) {
        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) vedtakDao.spesialsakFerdigbehandlet(vedtaksperiodeId)
    }

    override fun behandle(
        person: Person,
        kommandofabrikk: Kommandofabrikk,
    ) {
        person.vedtakFattet(this)
    }
}
