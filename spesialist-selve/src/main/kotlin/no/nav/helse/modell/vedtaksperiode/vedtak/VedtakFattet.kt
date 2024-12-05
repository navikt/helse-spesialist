package no.nav.helse.modell.vedtaksperiode.vedtak

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import no.nav.helse.db.VedtakDao
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import java.util.UUID

internal class VedtakFattet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
    private val spleisBehandlingId: UUID,
) : Vedtaksperiodemelding {
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

    internal fun doFinally(vedtakDao: VedtakDao) {
        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) vedtakDao.spesialsakFerdigbehandlet(vedtaksperiodeId)
    }

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        transactionalSession: TransactionalSession,
    ) = person.vedtakFattet(vedtaksperiodeId = vedtaksperiodeId, spleisBehandlingId = spleisBehandlingId)
}
