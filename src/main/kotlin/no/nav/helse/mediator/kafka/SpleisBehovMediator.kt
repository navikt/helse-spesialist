package no.nav.helse.mediator.kafka

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.rapids_rivers.RapidsConnection

internal class SpleisBehovMediator(private val kafkaRapidsConnection: RapidsConnection) {
    fun håndter(spleisBehov: SpleisBehov) {
        spleisBehov.start()
        spleisBehov.behov.forEach { kafkaRapidsConnection.publish(it.fødselsnummer, it.toJson()) }
    }
}
