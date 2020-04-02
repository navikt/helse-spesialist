package no.nav.helse.mediator.kafka

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.rapids_rivers.RapidsConnection

internal class SpleisBehovMediator() {
    internal fun håndter(context: RapidsConnection.MessageContext, spleisBehov: SpleisBehov) {
        spleisBehov.execute()
        spleisBehov.behov()?.also { behov ->
            context.send(behov.fødselsnummer, behov.toJson())
        }

        // TODO: Persister spleisBehov til databasen
    }

    internal fun håndter(spleisBehovId: String, behandlendeEnhet: HentEnhetLøsning?, hentPersoninfoLøsning: HentPersoninfoLøsning?) {
        // TODO: Hente spleis behov fra databasen
        val spleisBehov: SpleisBehov = null!!
        behandlendeEnhet?.also(spleisBehov::fortsett)
        hentPersoninfoLøsning?.also(spleisBehov::fortsett)
        spleisBehov.execute()
        // TODO: Persister spleisBehov til databasen
    }

    fun håndter(løsning: ArbeidsgiverLøsning) {
        val spleisBehov: SpleisBehov = null!!
        spleisBehov.fortsett(løsning)
        spleisBehov.execute()
    }
}
