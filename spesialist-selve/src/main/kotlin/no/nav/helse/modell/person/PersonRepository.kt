package no.nav.helse.modell.person

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import javax.sql.DataSource

internal class PersonRepository(private val dataSource: DataSource) {
    private val personDao = PersonDao(dataSource)
    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)
    private val generasjonRepository = GenerasjonRepository(dataSource)

    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: Person.() -> Unit,
    ) {
        sessionOf(dataSource).use {
            it.transaction { tx ->
                val person = tx.hentPerson(fødselsnummer) ?: return
                personScope(person)
                tx.lagrePerson(person.toDto())
            }
        }
    }

    private fun TransactionalSession.hentPerson(fødselsnummer: String): Person? {
        return with(personDao) {
            finnPerson(fødselsnummer)
                ?.copy(vedtaksperioder = with(generasjonRepository) { finnVedtaksperioder(fødselsnummer) })
                ?.copy(
                    skjønnsfastsatteSykepengegrunnlag =
                        with(
                            sykefraværstilfelleDao,
                        ) { finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer) },
                )
                ?.let { Person.gjenopprett(it.aktørId, it.fødselsnummer, it.vedtaksperioder, it.skjønnsfastsatteSykepengegrunnlag) }
        }
    }

    private fun TransactionalSession.lagrePerson(person: PersonDto) {
        with(generasjonRepository) {
            lagreVedtaksperioder(person.fødselsnummer, person.vedtaksperioder)
        }
    }
}
