package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.Behovtype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class ArbeidsgiverMessage(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val spleisBehovId: String,
    val arbeidsgiverNavn: String
) {
    fun asArbeidsgiverLøsning() = ArbeidsgiverLøsning(
        arbeidsgiverNavn
    )

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val spleisBehovMediator: SpleisBehovMediator
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", Behovtype.HentArbeidsgiverNavn)
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                    it.requireKey("HentArbeidsgiverNavn")
                    it.requireValue("final", true)
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = ArbeidsgiverMessage(
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText(),
                spleisBehovId = packet["@id"].asText(),
                arbeidsgiverNavn = packet["HentArbeidsgiverNavn"].asText()
            )

            spleisBehovMediator.håndter(behov.asArbeidsgiverLøsning())
        }
    }
}
