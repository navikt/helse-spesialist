package no.nav.helse.spesialist.application

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.spesialist.domain.Identitetsnummer

class InMemoryEgenAnsattApiDao(private val personRepository: InMemoryPersonRepository) : EgenAnsattApiDao {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus?.erEgenAnsatt
}
