package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.Kjønn
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class PersoninfoLøsningMessage {
    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisbehovMediator: SpleisbehovMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAllOrAny("@behov", listOf("HentEnhet", "HentPersoninfo"))
                    it.requireKey("@løsning", "spleisBehovId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke HentEnhet eller HentPersoninfo:\n${problems.toExtendedReport()}")
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
                val fødselsdato = LocalDate.parse(hentPersoninfo["fødelsdato"].asText())
                val kjønn = Kjønn.valueOf(hentPersoninfo["kjønn"].textValue())
                HentPersoninfoLøsning(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)
            }

        private fun JsonNode.tilHentEnhetLøsning() =
            takeIf { hasNonNull("HentEnhet") }?.let { HentEnhetLøsning(it["HentEnhet"].asText()) }
    }
}
