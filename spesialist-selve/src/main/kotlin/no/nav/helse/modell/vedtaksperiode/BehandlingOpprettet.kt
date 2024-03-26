package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

data class SpleisBehandling(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate
) {
    internal fun erRelevantFor(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}

internal class BehandlingOpprettet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val json: String
): Vedtaksperiodemelding {

    internal constructor(packet: JsonMessage): this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = packet["behandlingId"].asUUID(),
        fom = packet["fom"].asLocalDate(),
        tom = packet["tom"].asLocalDate(),
        json = packet.toJson()
    )
    internal constructor(jsonNode: JsonNode): this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
        vedtaksperiodeId = jsonNode["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = jsonNode["behandlingId"].asUUID(),
        fom = jsonNode["fom"].asLocalDate(),
        tom = jsonNode["tom"].asLocalDate(),
        json = jsonNode.toString()
    )

    override fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {
        person.nySpleisBehandling(SpleisBehandling(organisasjonsnummer, vedtaksperiodeId, spleisBehandlingId, fom, tom))
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json
}