package no.nav.helse.modell.person

import no.nav.helse.db.SessionContext

class PersonService(
    sessionContext: SessionContext,
) {
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val sykefraværstilfelleDao = sessionContext.sykefraværstilfelleDao
    private val avviksvurderingDao = sessionContext.avviksvurderingDao
    private val personDao = sessionContext.personDao

    fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: Person.() -> Unit,
    ) {
        val person = hentPerson(fødselsnummer) ?: return
        personScope(person)
        lagrePerson(person.toDto())
    }

    private fun hentPerson(fødselsnummer: String): Person? =
        finnPerson(fødselsnummer)
            ?.copy(vedtaksperioder = vedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer))
            ?.copy(
                skjønnsfastsatteSykepengegrunnlag = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer),
            )?.copy(
                avviksvurderinger = avviksvurderingDao.finnAvviksvurderinger(fødselsnummer),
            )?.let {
                Person.gjenopprett(
                    aktørId = it.aktørId,
                    fødselsnummer = it.fødselsnummer,
                    vedtaksperioder = it.vedtaksperioder,
                    skjønnsfastsattSykepengegrunnlag = it.skjønnsfastsatteSykepengegrunnlag,
                    avviksvurderinger = it.avviksvurderinger,
                )
            }

    private fun lagrePerson(person: PersonDto) {
        vedtaksperiodeRepository.lagreVedtaksperioder(person.fødselsnummer, person.vedtaksperioder)
    }

    private fun finnPerson(fødselsnummer: String): PersonDto? = personDao.finnPerson(fødselsnummer)
}
