package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import org.slf4j.LoggerFactory

internal class InntektLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "InntekterForSykepengegrunnlag"

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf(behov))
            it.requireKey("contextId", "hendelseId")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.requireArray("@løsning.$behov") {
                require("årMåned", JsonNode::asYearMonth)
                requireArray("inntektsliste") {
                    requireKey("beløp")
                    interestedIn("orgnummer")
                }
            }
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLog.error("forstod ikke Inntekter:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()

        val inntektsløsning =
            Inntektløsning(
                packet["@løsning.$behov"].map { løsning ->
                    Inntekter(
                        løsning["årMåned"].asYearMonth(),
                        løsning["inntektsliste"].map {
                            Inntekter.Inntekt(
                                it["beløp"].asDouble(),
                                it["orgnummer"].asText(),
                            )
                        },
                    )
                },
            )

        mediator.løsning(hendelseId, contextId, packet["@id"].asUUID(), inntektsløsning, context)
    }
}
