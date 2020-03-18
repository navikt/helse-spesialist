package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class AvventerGodkjenningBehov(
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: String
) {
    fun asSpleisBehov(personDao: PersonDao) = SpleisBehov(fødselsnummer, organisasjonsnummer, aktørId, personDao)

    internal class Factory(
        rapidsConnection: RapidsConnection,
        private val personDao: PersonDao,
        private val spleisBehovMediator: SpleisBehovMediator
    ) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireAll("@behov", listOf("Godkjenning"))
                    it.requireKey("fødselsnummer")
                    it.requireKey("aktørId")
                    it.requireKey("organisasjonsnummer")
                    it.requireKey("vedtaksperiodeId")
                }
            }
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val behov = AvventerGodkjenningBehov(
                fødselsnummer = packet["fødselsnummer"].asText(),
                aktørId = packet["aktørId"].asText(),
                organisasjonsnummer = packet["organisasjonsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
            )
            spleisBehovMediator.håndter(context, behov.asSpleisBehov(personDao))
        }
    }
}
