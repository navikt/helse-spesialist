package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.PersonEgenskap
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class PersoninfoMessage(
    val spleisbehovId: UUID,
    val enhetNr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    private fun asBehandlendeEnhet() = HentEnhetLøsning(
        enhetNr = enhetNr
    )

    private fun asHentNavnLøsning() = HentPersoninfoLøsning(
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn
    )

    internal class Factory(rapidsConnection: RapidsConnection, private val spleisbehovMediator: SpleisbehovMediator) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", Behovtype.HentEnhet, Behovtype.HentPersoninfo)
                    it.requireKey("@løsning")
                    it.requireValue("@final", true)
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hentEnhet = packet["@løsning"]["HentEnhet"].asText()
            val hentPersoninfo = packet["@løsning"]["HentNavn"]

            val behov = PersoninfoMessage(
                spleisbehovId = UUID.fromString(packet["spleisBehovId"].asText()),
                enhetNr = hentEnhet,
                fornavn = hentPersoninfo["fornavn"].asText(),
                mellomnavn = hentPersoninfo.takeIf { it.hasNonNull("mellomavn") }?.get("mellomnavn")?.asText(),
                etternavn = hentPersoninfo["etternavn"].asText()
            )

            spleisbehovMediator.håndter(behov.spleisbehovId, behov.asBehandlendeEnhet(), behov.asHentNavnLøsning())
        }
    }
}
