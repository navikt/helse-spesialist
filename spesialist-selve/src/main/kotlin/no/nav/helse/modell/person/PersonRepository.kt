package no.nav.helse.modell.person

import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository

internal class PersonRepository(private val dataSource: DataSource) {
    private val personDao = PersonDao(dataSource)
    private val generasjonRepository = GenerasjonRepository(dataSource)
    fun brukPersonHvisFinnes(fødselsnummer: String, personScope: Person.() -> Unit) {
        sessionOf(dataSource).use {
            it.transaction { tx ->
                val person = tx.finnPerson(fødselsnummer) ?: return
                personScope(person)
                tx.lagrePerson(person.toDto())
            }
        }
    }

    private fun TransactionalSession.finnPerson(fødselsnummer: String): Person? {
        return with(personDao) {
            finnPerson(fødselsnummer)
                ?.copy(vedtaksperioder = with(generasjonRepository) { finnVedtaksperioder(fødselsnummer) })
                ?.let { Person.gjenopprett(it.fødselsnummer, it.vedtaksperioder) }
        }
    }

    private fun TransactionalSession.lagrePerson(person: PersonDto) {
        with(generasjonRepository) {
            lagreVedtaksperioder(person.fødselsnummer, person.vedtaksperioder)
        }
    }
}