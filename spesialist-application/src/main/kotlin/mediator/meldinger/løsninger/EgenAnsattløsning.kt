package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.time.LocalDateTime
import java.time.ZoneId

class EgenAnsattløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erEgenAnsatt: Boolean,
) {
    internal fun lagre(personRepository: PersonRepository) {
        val person = personRepository.finn(Identitetsnummer.fraString(fødselsnummer)) ?: return
        person.oppdaterEgenAnsattStatus(
            erEgenAnsatt = erEgenAnsatt,
            oppdatertTidspunkt = opprettet.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
        )
        personRepository.lagre(person)
    }
}
