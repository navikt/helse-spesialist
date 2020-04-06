package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.PersonEgenskap
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
    val etternavn: String,
    val egenskap: PersonEgenskap?
) {
    private fun asBehandlendeEnhet() = HentEnhetLøsning(
        enhetNr = enhetNr
    )

    private fun asHentNavnLøsning() = HentPersoninfoLøsning(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        egenskap = egenskap
    )

    internal class Factory(rapidsConnection: RapidsConnection, private val spleisBehovMediator: SpleisBehovMediator) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", Behovtype.HentEnhet, Behovtype.HentPersoninfo)
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
            val hentPersoninfo = packet["HentNavn"]

            val behov = PersoninfoMessage(
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText(),
                spleisBehovId = packet["spleisBehovId"].asText(),
                enhetNr = hentEnhet,
                fornavn = hentPersoninfo["fornavn"].asText(),
                mellomnavn = hentPersoninfo.takeIf { it.hasNonNull("mellomavn") }?.get("mellomnavn")?.asText(),
                etternavn = hentPersoninfo["etternavn"].asText(),
                egenskap = hentPersoninfo ["diskresjonskode"]?.let { PersonEgenskap.find(it.asText()) }
            )

            spleisBehovMediator.håndter(behov.spleisBehovId, behov.asBehandlendeEnhet(), behov.asHentNavnLøsning())
        }
    }
}
