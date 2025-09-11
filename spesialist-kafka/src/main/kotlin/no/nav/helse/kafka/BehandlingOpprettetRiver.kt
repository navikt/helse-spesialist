package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.FeatureToggles
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.logg.logg

class BehandlingOpprettetRiver(
    private val mediator: MeldingMediator,
    private val featureToggles: FeatureToggles,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behandling_opprettet")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "vedtaksperiodeId",
                "behandlingId",
                "fødselsnummer",
                "organisasjonsnummer",
                "yrkesaktivitetstype",
            )
            it.requireKey("fom", "tom")
        }

    fun behandlerIkke(organisasjonsnummer: String) {
        logg.info("Tar ikke imot behandling opprettet for: $organisasjonsnummer")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        if (organisasjonsnummer in listOf("ARBEIDSLEDIG", "FRILANS")) {
            behandlerIkke(organisasjonsnummer)
            return
        }
        val yrkesaktivitetstype = Yrkesaktivitetstype.valueOf(packet["yrkesaktivitetstype"].asText())
        if (yrkesaktivitetstype == Yrkesaktivitetstype.SELVSTENDIG) {
            behandlerIkke(organisasjonsnummer)
            return
        }
        mediator.mottaMelding(
            BehandlingOpprettet(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                spleisBehandlingId = packet["behandlingId"].asUUID(),
                fom = packet["fom"].asLocalDate(),
                tom = packet["tom"].asLocalDate(),
                yrkesaktivitetstype = yrkesaktivitetstype,
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }
}
