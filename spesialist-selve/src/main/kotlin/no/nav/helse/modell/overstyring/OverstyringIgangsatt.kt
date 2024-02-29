package no.nav.helse.modell.overstyring

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.rapids_rivers.JsonMessage

internal class OverstyringIgangsatt private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val kilde: UUID,
    val berørteVedtaksperiodeIder: List<UUID>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        kilde = UUID.fromString(packet["kilde"].asText()),
        berørteVedtaksperiodeIder = packet["berørtePerioder"].map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
        json = packet.toJson()
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}