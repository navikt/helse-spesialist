package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class SykefraværstilfellerRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "sykefraværstilfeller")
                it.requireKey("fødselsnummer", "aktørId", "@id")
                it.requireArray("tilfeller") {
                    require("dato") { datoNode ->
                        LocalDate.parse(datoNode.asText())
                    }
                    requireArray("perioder") {
                        require("vedtaksperiodeId") { vedtaksperiodeIdNode ->
                            UUID.fromString(vedtaksperiodeIdNode.asText())
                        }
                        requireKey("organisasjonsnummer")
                        require("fom") { fomNode ->
                            LocalDate.parse(fomNode.asText())
                        }
                        require("tom") { tomNode ->
                            LocalDate.parse(tomNode.asText())
                        }
                    }
                }

            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke sykefraværstilfeller:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val id = UUID.fromString(packet["@id"].asText())
        val vedtaksperioder = packet["tilfeller"].flatMap { tilfelleNode ->
            val skjæringstidspunkt = tilfelleNode["dato"].asLocalDate()
            tilfelleNode["perioder"].map {
                VedtaksperiodeOppdatering(
                    skjæringstidspunkt = skjæringstidspunkt,
                    fom = LocalDate.parse(it["fom"].asText()),
                    tom = LocalDate.parse(it["tom"].asText()),
                    vedtaksperiodeId = UUID.fromString(it["vedtaksperiodeId"].asText()),
                )
            }
        }
        mediator.sykefraværstilfeller(
            packet.toJson(),
            id,
            vedtaksperioder,
            packet["fødselsnummer"].asText(),
            packet["aktørId"].asText(),
            context,
        )
    }
}