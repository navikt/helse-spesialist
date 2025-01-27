package no.nav.helse.modell.person

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto

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
        val (person, dtoFør) = hentPerson(fødselsnummer) ?: return
        personScope(person)
        val dtoEtter = person.toDto()
        if (dtoFør != dtoEtter) lagrePerson(dtoFør, dtoEtter)
    }

    private fun finnEndredeVedtaksperioder(
        dtoFør: PersonDto,
        dtoEtter: PersonDto,
    ): List<VedtaksperiodeDto> {
        val vedtaksperioderFør = dtoFør.vedtaksperioder.associateBy { it.vedtaksperiodeId }
        val vedtaksperioderEtter = dtoEtter.vedtaksperioder

        val tilLagring =
            vedtaksperioderEtter.filter { vedtaksperiodeEtter ->
                val vedtaksperiodeFør = vedtaksperioderFør[vedtaksperiodeEtter.vedtaksperiodeId] ?: return@filter true
                vedtaksperiodeFør != vedtaksperiodeEtter
            }
        return tilLagring
    }

    private fun hentPerson(fødselsnummer: String): Pair<Person, PersonDto>? =
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
                ) to it
            }

    private fun lagrePerson(
        dtoFør: PersonDto,
        dtoEtter: PersonDto,
    ) {
        val vedtaksperioderSomSkalLagres = finnEndredeVedtaksperioder(dtoFør, dtoEtter)
        vedtaksperiodeRepository.lagreVedtaksperioder(dtoEtter.fødselsnummer, vedtaksperioderSomSkalLagres)
    }

    private fun finnPerson(fødselsnummer: String): PersonDto? = personDao.finnPerson(fødselsnummer)
}
