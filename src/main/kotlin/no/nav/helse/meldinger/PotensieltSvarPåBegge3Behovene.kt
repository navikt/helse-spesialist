package no.nav.helse.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.løsning.BehandlendeEnhetLøsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class PotensieltSvarPåBegge3Behovene(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val spleisBehovId: String,
    val behandlendeEnhet: String
) {
    private fun asBehandlendeEnhet() = BehandlendeEnhetLøsning(
        spleisBehovId = spleisBehovId,
        enhetNr = behandlendeEnhet
    )

    internal class Factory(rapidsConnection: RapidsConnection, private val spleisBehovMediator: SpleisBehovMediator) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", listOf("BehandlendeEnhet"))
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                    it.requireValue("final", true)
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = PotensieltSvarPåBegge3Behovene(
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText(),
                spleisBehovId = packet["@id"].asText(),
                behandlendeEnhet = packet["BehandlendeEnhet"].asText()
            )
            spleisBehovMediator.håndter(behov.asBehandlendeEnhet())
        }
    }
}
