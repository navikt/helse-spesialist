package no.nav.helse.modell.person

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.db.Repositories
import no.nav.helse.modell.vedtaksperiode.GenerasjonService
import javax.sql.DataSource

class PersonService(
    private val dataSource: DataSource,
    private val repositories: Repositories,
) {
    private val generasjonService = GenerasjonService(repositories)

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

    private fun TransactionalSession.hentPerson(fødselsnummer: String): Person? =
        finnPerson(fødselsnummer)
            ?.copy(vedtaksperioder = with(generasjonService) { finnVedtaksperioder(fødselsnummer) })
            ?.copy(
                skjønnsfastsatteSykepengegrunnlag =
                    repositories.withSessionContext(this).sykefraværstilfelleDao
                        .finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer),
            )?.copy(
                avviksvurderinger = repositories.withSessionContext(this).avviksvurderingDao.finnAvviksvurderinger(fødselsnummer),
            )?.let {
                Person.gjenopprett(
                    aktørId = it.aktørId,
                    fødselsnummer = it.fødselsnummer,
                    vedtaksperioder = it.vedtaksperioder,
                    skjønnsfastsattSykepengegrunnlag = it.skjønnsfastsatteSykepengegrunnlag,
                    avviksvurderinger = it.avviksvurderinger,
                )
            }

    private fun TransactionalSession.lagrePerson(person: PersonDto) {
        with(generasjonService) {
            lagreVedtaksperioder(person.fødselsnummer, person.vedtaksperioder)
        }
    }

    private fun Session.finnPerson(fødselsnummer: String): PersonDto? =
        repositories.withSessionContext(this).personDao.finnPerson(fødselsnummer)
}
