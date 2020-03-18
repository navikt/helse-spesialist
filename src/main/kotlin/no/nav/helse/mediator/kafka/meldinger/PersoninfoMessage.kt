package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import no.nav.helse.modell.Behovtype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class PersoninfoMessage(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    val spleisBehovId: String,
    val enhetNr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    private fun asBehandlendeEnhet() = HentEnhetLøsning(
        enhetNr = enhetNr
    )

    private fun asHentNavnLøsning() = HentNavnLøsning(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn
    )

    internal class Factory(rapidsConnection: RapidsConnection, private val spleisBehovMediator: SpleisBehovMediator) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", Behovtype.HentEnhet, Behovtype.HentNavn)
                    it.requireKey("fødselsnummer")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                    it.requireKey("HentNavn")
                    it.requireValue("final", true)
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hentEnhet = packet["HentEnhet"].asText()
            val hentNavn = packet["HentNavn"]

            val behov = PersoninfoMessage(
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText(),
                spleisBehovId = packet["@id"].asText(),
                enhetNr = hentEnhet,
                fornavn = hentNavn["fornavn"].asText(),
                mellomnavn = hentNavn.takeIf { it.hasNonNull("mellomavn") }?.get("mellomnavn")?.asText(),
                etternavn = hentNavn["etternavn"].asText()
            )

            spleisBehovMediator.håndter(behov.spleisBehovId, behov.asBehandlendeEnhet(), behov.asHentNavnLøsning())
        }
    }
}
