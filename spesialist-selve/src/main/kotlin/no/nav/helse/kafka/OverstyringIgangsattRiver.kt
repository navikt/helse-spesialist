package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OverstyringIgangsattRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "overstyring_igangsatt")
            it.requireKey("kilde")
            it.requireArray("berørtePerioder") {
                requireKey("vedtaksperiodeId")
            }
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLogg.error("Forstod ikke overstyring_igangsatt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(OverstyringIgangsatt(packet), context)
    }
}
