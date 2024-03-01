package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

internal class Sykefraværstilfeller private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeOppdateringer: List<VedtaksperiodeOppdatering>,
    private val json: String,
) : Personmelding {
    internal constructor(packet: JsonMessage): this(
        UUID.fromString(packet["@id"].asText()),
        packet["fødselsnummer"].asText(),
        packet["aktørId"].asText(),
        packet["tilfeller"].flatMap { tilfelleNode ->
            val skjæringstidspunkt = tilfelleNode["dato"].asLocalDate()
            tilfelleNode["perioder"].map {
                VedtaksperiodeOppdatering(
                    skjæringstidspunkt = skjæringstidspunkt,
                    fom = LocalDate.parse(it["fom"].asText()),
                    tom = LocalDate.parse(it["tom"].asText()),
                    vedtaksperiodeId = UUID.fromString(it["vedtaksperiodeId"].asText()),
                )
            }
        },
        packet.toJson(),
    )
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}