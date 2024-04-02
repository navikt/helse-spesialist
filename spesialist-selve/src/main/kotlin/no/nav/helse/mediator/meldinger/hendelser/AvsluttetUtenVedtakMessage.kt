package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import no.nav.helse.rapids_rivers.JsonMessage

internal class AvsluttetUtenVedtakMessage private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val hendelser: List<UUID>,
    private val json: String
): Vedtaksperiodemelding {

    internal constructor(jsonNode: JsonNode): this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(jsonNode["vedtaksperiodeId"].asText()),
        hendelser = jsonNode["hendelser"].map { it.asUUID() },
        json = jsonNode.toString()
    )

    internal constructor(packet: JsonMessage): this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        hendelser = packet["hendelser"].map { it.asUUID() },
        json = packet.toJson()
    )

    private val avsluttetUtenVedtak get() = AvsluttetUtenVedtak(
        vedtaksperiodeId = vedtaksperiodeId,
        hendelser = hendelser,
    )
    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun toJson(): String = json

    override fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {
        person.avsluttetUtenVedtak(avsluttetUtenVedtak)
    }
}
