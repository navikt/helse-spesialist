package no.nav.helse.mediator.meldinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.OppdaterSykefraværstilfellerCommand
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class Sykefraværstilfeller(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    vedtaksperioder: List<Vedtaksperiode>,
    private val json: String,
    generasjonRepository: GenerasjonRepository,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSykefraværstilfellerCommand(
            fødselsnummer,
            aktørId,
            vedtaksperioder,
            generasjonRepository,
        )
    )
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
    ) : no.nav.helse.rapids_rivers.River.PacketListener {

        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

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
                    Vedtaksperiode(
                        UUID.fromString(it["vedtaksperiodeId"].asText()),
                        skjæringstidspunkt,
                        Periode(
                            LocalDate.parse(it["fom"].asText()),
                            LocalDate.parse(it["tom"].asText()),
                        )
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
}