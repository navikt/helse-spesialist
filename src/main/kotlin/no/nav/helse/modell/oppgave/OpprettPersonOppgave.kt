package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDateTime

internal class OpprettPersonOppgave(
    private val spleisBehov: SpleisBehov,
    private val personDao: PersonDao
) : Oppgave() {
    override var ferdigstilt: LocalDateTime? = null
    private var navnId: Int? = null
    private var enhetId: Int? = null

    override fun execute() {
        if (personDao.finnPerson(spleisBehov.fødselsnummer.toLong())) {
            ferdigstilt = LocalDateTime.now()
        } else if (navnId != null && enhetId != null) {
            personDao.insertPerson(spleisBehov.fødselsnummer.toLong(), spleisBehov.aktørId.toLong(), navnId!!, enhetId!!)
            ferdigstilt = LocalDateTime.now()
        } else {
            spleisBehov.håndter(Behovtype.HentNavn)
            spleisBehov.håndter(Behovtype.HentEnhet)
        }
    }

    override fun fortsett(hentEnhetLøsning: HentEnhetLøsning) {
        enhetId = hentEnhetLøsning.enhetNr.toInt()
    }

    override fun fortsett(hentNavnLøsning: HentNavnLøsning) {
        navnId = personDao.insertNavn(hentNavnLøsning.fornavn, hentNavnLøsning.mellomnavn, hentNavnLøsning.etternavn)
    }

}
