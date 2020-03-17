package no.nav.helse.mediator.kafka

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.løsning.BehandlendeEnhetLøsning
import no.nav.helse.rapids_rivers.RapidsConnection

internal class SpleisBehovMediator {
    internal fun håndter(context: RapidsConnection.MessageContext, spleisBehov: SpleisBehov) {
        spleisBehov.start()
        spleisBehov.behov()?.also { behov ->
            context.send(behov.fødselsnummer, behov.toJson())
        }

        // TODO: Persister spleisBehov til databasen
    }

    internal fun håndter(behandlendeEnhet: BehandlendeEnhetLøsning) {
        // TODO: Hente spleis behov fra databasen
        val spleisBehov: SpleisBehov = null!!
        spleisBehov.fortsett(behandlendeEnhet)
        // TODO: Persister spleisBehov til databasen
    }
}
