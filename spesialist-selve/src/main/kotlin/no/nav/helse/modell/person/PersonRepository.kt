package no.nav.helse.modell.person

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TransactionalPersonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import javax.sql.DataSource

internal class PersonRepository(
    private val dataSource: DataSource,
) {
    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
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

    private fun TransactionalSession.hentPerson(fødselsnummer: String): Person? =
        finnPerson(fødselsnummer)
            ?.copy(vedtaksperioder = with(generasjonRepository) { finnVedtaksperioder(fødselsnummer) })
            ?.copy(
                skjønnsfastsatteSykepengegrunnlag =
                    with(sykefraværstilfelleDao) { finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer) },
            )?.copy(
                avviksvurderinger =
                    with(avviksvurderingDao) {
                        this@hentPerson.finnAvviksvurderinger(fødselsnummer)
                    },
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
        with(generasjonRepository) {
            lagreVedtaksperioder(person.fødselsnummer, person.vedtaksperioder)
        }
    }

    private fun TransactionalSession.finnPerson(fødselsnummer: String): PersonDto? = TransactionalPersonDao(this).finnPerson(fødselsnummer)
}
