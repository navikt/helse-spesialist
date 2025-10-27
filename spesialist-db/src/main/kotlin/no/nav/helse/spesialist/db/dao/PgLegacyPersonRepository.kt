package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.db.PersonDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.LegacyPersonRepository
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.list

class PgLegacyPersonRepository(
    private val session: Session,
    private val legacyVedtaksperiodeRepository: LegacyVedtaksperiodeRepository,
    private val sykefraværstilfelleDao: SykefraværstilfelleDao,
    private val personDao: PersonDao,
) : LegacyPersonRepository {
    private val avviksvurderingRepository = PgAvviksvurderingRepository(session)

    override fun brukPersonHvisFinnes(
        fødselsnummer: String,
        personScope: LegacyPerson.() -> Unit,
    ) {
        val person =
            hentPerson(fødselsnummer) ?: run {
                "Behandler ikke melding for ukjent person".let { melding ->
                    logg.info(melding)
                    sikkerlogg.info("$melding med fødselsnummer={}", fødselsnummer)
                }
                return
            }
        val dtoFør = person.toDto()
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

    private fun hentPerson(fødselsnummer: String): LegacyPerson? {
        val minimalPerson = personDao.finnMinimalPerson(fødselsnummer) ?: return null

        return LegacyPerson.gjenopprett(
            aktørId = minimalPerson.aktørId,
            fødselsnummer = minimalPerson.fødselsnummer,
            vedtaksperioder = legacyVedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer),
            skjønnsfastsattSykepengegrunnlag =
                sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(
                    fødselsnummer,
                ),
            avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(fødselsnummer),
        )
    }

    private fun lagrePerson(
        dtoFør: PersonDto,
        dtoEtter: PersonDto,
    ) {
        val vedtaksperioderSomSkalLagres = finnEndredeVedtaksperioder(dtoFør, dtoEtter)
        legacyVedtaksperiodeRepository.lagreVedtaksperioder(dtoEtter.fødselsnummer, vedtaksperioderSomSkalLagres)
    }

    override fun finnFødselsnumre(aktørId: String): List<String> =
        asSQL(
            " SELECT fødselsnummer FROM person WHERE aktør_id = :aktor_id; ",
            "aktor_id" to aktørId,
        ).list(session) { it.string("fødselsnummer") }
}
