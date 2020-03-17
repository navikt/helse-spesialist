package no.nav.helse.mediator.kafka

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import no.nav.helse.rapids_rivers.RapidsConnection

internal class SpleisBehovMediator {
    internal fun håndter(context: RapidsConnection.MessageContext, spleisBehov: SpleisBehov) {
        spleisBehov.start()
        spleisBehov.behov()?.also { behov ->
            context.send(behov.fødselsnummer, behov.toJson())
        }

        // TODO: Persister spleisBehov til databasen
    }

    internal fun håndter(spleisBehovId: String, behandlendeEnhet: HentEnhetLøsning?, hentNavnLøsning: HentNavnLøsning?) {
        // TODO: Hente spleis behov fra databasen
        val spleisBehov: SpleisBehov = null!!
        behandlendeEnhet?.also(spleisBehov::fortsett)
        hentNavnLøsning?.also(spleisBehov::fortsett)
        // TODO: Persister spleisBehov til databasen
    }
}
