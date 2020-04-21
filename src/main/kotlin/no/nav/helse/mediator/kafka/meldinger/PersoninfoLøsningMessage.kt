package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class PersoninfoLøsningMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_name", "behov")
                    it.require("@behov") { node ->
                        require(node.isArray) { "@behov må være et array" }
                        require(node.any { item -> item.asText() in arrayOf("HentEnhet", "HentPersoninfo") }) {
                            "Løsning må være av typen HentEnhet eller HentPersoninfo"
                        }
                    }
                    it.requireAll("@behov", Behovtype.HentPersoninfo)
                    it.requireKey("@løsning", "spleisBehovId")
                    it.requireValue("@final", true)
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.info(problems.toExtendedReport())
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hentEnhet = packet["@løsning"].tilHentEnhetLøsning()
            val hentPersoninfo = packet["@løsning"].tilPersonInfoLøsning()

            val spleisbehovId = UUID.fromString(packet["spleisBehovId"].asText())

            spleisbehovMediator.håndter(spleisbehovId, hentEnhet, hentPersoninfo)
        }

        private fun JsonNode.tilPersonInfoLøsning() =
            takeIf { hasNonNull("HentPersoninfo") }?.let {
                val hentPersoninfo = it["HentPersoninfo"]
                val fornavn = hentPersoninfo["fornavn"].asText()
                val mellomnavn = hentPersoninfo.takeIf { it.hasNonNull("mellomavn") }?.get("mellomnavn")?.asText()
                val etternavn = hentPersoninfo["etternavn"].asText()
                HentPersoninfoLøsning(fornavn, mellomnavn, etternavn)
            }

        private fun JsonNode.tilHentEnhetLøsning() =
            takeIf { hasNonNull("HentEnhet") }?.let { HentEnhetLøsning(it["HentEnhet"].asText()) }
    }
}
