package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal class OverstyringIgangsatt private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val kilde: UUID,
    val berørteVedtaksperiodeIder: List<UUID>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage) : this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        kilde = UUID.fromString(packet["kilde"].asText()),
        berørteVedtaksperiodeIder = packet["berørtePerioder"].map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
        json = packet.toJson(),
    )
    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode["@id"].asText()),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        kilde = UUID.fromString(jsonNode["kilde"].asText()),
        berørteVedtaksperiodeIder = jsonNode["berørtePerioder"].map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandofabrikk: Kommandofabrikk,
    ) {
        kommandofabrikk.iverksettOverstyringIgangsatt(this)
    }

    override fun fødselsnummer() = fødselsnummer

    override fun toJson() = json
}
