package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfeller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
        mediator.håndter(Sykefraværstilfeller(packet), context)
    }
}