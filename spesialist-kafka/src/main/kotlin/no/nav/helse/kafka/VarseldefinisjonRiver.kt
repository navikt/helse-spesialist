package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import tools.jackson.databind.JsonNode

class VarseldefinisjonRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "varselkode_ny_definisjon")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.requireKey("varselkode")
            it.requireKey("gjeldende_definisjon")
            it.requireKey(
                "gjeldende_definisjon.id",
                "gjeldende_definisjon.kode",
                "gjeldende_definisjon.tittel",
                "gjeldende_definisjon.avviklet",
                "gjeldende_definisjon.opprettet",
            )
            it.interestedIn("gjeldende_definisjon.forklaring", "gjeldende_definisjon.handling")
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        loggInfo("Mottok melding om ny definisjon for varselkode: ${packet["varselkode"].asString()}")

        val varseldefinisjon =
            Varseldefinisjon(
                id = VarseldefinisjonId(packet["gjeldende_definisjon.id"].asUUID()),
                kode = packet["varselkode"].asString(),
                tittel = packet["gjeldende_definisjon.tittel"].asString(),
                forklaring = packet["gjeldende_definisjon.forklaring"].takeUnless(JsonNode::isMissingOrNull)?.stringValue(),
                handling = packet["gjeldende_definisjon.handling"].takeUnless(JsonNode::isMissingOrNull)?.stringValue(),
                avviklet = packet["gjeldende_definisjon.avviklet"].asBoolean(),
                opprettet = packet["gjeldende_definisjon.opprettet"].asLocalDateTime(),
            )
        transaksjon.varseldefinisjonRepository.lagre(varseldefinisjon)
        if (varseldefinisjon.avviklet) {
            transaksjon.varselRepository.avvikle(varseldefinisjon)
        }
    }
}
