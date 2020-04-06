package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.LocalDateTime

internal class OpprettPersonCommand(
    private val spleisbehov: Spleisbehov,
    private val personDao: PersonDao
) : Command() {
    override var ferdigstilt: LocalDateTime? = null
    private var navnId: Int? = null
    private var enhetId: Int? = null

    override fun execute() {
        if (personDao.findPerson(spleisbehov.fødselsnummer.toLong()) != null) {
            ferdigstilt = LocalDateTime.now()
        } else if (navnId != null && enhetId != null) {
            personDao.insertPerson(spleisbehov.fødselsnummer.toLong(), spleisbehov.aktørId.toLong(), navnId!!, enhetId!!)
            ferdigstilt = LocalDateTime.now()
        } else {
            spleisbehov.håndter(Behovtype.HentPersoninfo)
            spleisbehov.håndter(Behovtype.HentEnhet)
        }
    }

    override fun fortsett(hentEnhetLøsning: HentEnhetLøsning) {
        enhetId = hentEnhetLøsning.enhetNr.toInt()
    }

    override fun fortsett(hentPersoninfoLøsning: HentPersoninfoLøsning) {
        navnId = personDao.insertNavn(hentPersoninfoLøsning.fornavn, hentPersoninfoLøsning.mellomnavn, hentPersoninfoLøsning.etternavn)
    }

}
