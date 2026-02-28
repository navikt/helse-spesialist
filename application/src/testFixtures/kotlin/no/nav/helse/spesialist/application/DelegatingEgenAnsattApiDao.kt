package no.nav.helse.spesialist.application

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.spesialist.domain.Identitetsnummer

class DelegatingEgenAnsattApiDao(private val personRepository: InMemoryPersonRepository) : EgenAnsattApiDao {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus?.erEgenAnsatt
}
