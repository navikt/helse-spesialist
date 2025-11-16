package no.nav.helse.spesialist.application

import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.LocalDateTime
import java.time.ZoneId

class InMemoryEgenAnsattDao(private val personRepository: InMemoryPersonRepository) : EgenAnsattDao {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus?.erEgenAnsatt

    override fun lagre(fødselsnummer: String, erEgenAnsatt: Boolean, opprettet: LocalDateTime) {
        val person = personRepository.finn(Identitetsnummer.fraString(fødselsnummer))
        person!!.oppdaterEgenAnsattStatus(
            erEgenAnsatt = erEgenAnsatt,
            oppdatertTidspunkt = opprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant()
        )
    }
}
