package no.nav.helse.modell.person

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.command.Command
import java.time.Duration
import java.util.*

internal class OpprettPersonCommand(
    private val personDao: PersonDao,
    private val fødselsnummer: String,
    private val aktørId: String,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    private var navnId: Int? = null
    private var enhetId: Int? = null

    override fun execute(): Resultat = if (personDao.findPersonByFødselsnummer(fødselsnummer.toLong()) != null) {
        Resultat.Ok.System
    } else if (navnId != null && enhetId != null) {
        personDao.insertPerson(fødselsnummer.toLong(), aktørId.toLong(), navnId!!, enhetId!!)
        Resultat.Ok.System
    } else {
        Resultat.HarBehov(Behovtype.HentPersoninfo, Behovtype.HentEnhet)
    }

    override fun fortsett(løsning: HentEnhetLøsning) {
        enhetId = løsning.enhetNr.toInt()
    }

    override fun fortsett(løsning: HentPersoninfoLøsning) {
        navnId = personDao.insertNavn(løsning.fornavn, løsning.mellomnavn, løsning.etternavn)
    }
}
