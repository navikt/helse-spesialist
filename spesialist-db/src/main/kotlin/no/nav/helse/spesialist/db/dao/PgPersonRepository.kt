package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.PersonRepository
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto

class PgPersonRepository(
    session: Session,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val sykefraværstilfelleDao: SykefraværstilfelleDao,
    private val personDao: PersonDao,
) : PersonRepository {
    private val avviksvurderingRepository = PgAvviksvurderingRepository(session)

    override fun brukPersonHvisFinnes(
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
                avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer),
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
