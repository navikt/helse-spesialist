package no.nav.helse.spesialist.application

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.LegacyPersonRepository
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling

class DelegatingLegacyPersonRepository(
    private val personRepository: InMemoryPersonRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
    private val legacyVedtaksperiodeRepository: LegacyVedtaksperiodeRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val varselRepository: InMemoryVarselRepository,
    private val vedtakBegrunnelseRepository: InMemoryVedtakBegrunnelseRepository,
    private val avviksvurderingRepository: AvviksvurderingRepository,
    private val sykefraværstilfelleDao: DelegatingSykefraværstilfelleDao,
) : LegacyPersonRepository {
    override fun brukPersonHvisFinnes(fødselsnummer: String, personScope: LegacyPerson.() -> Unit) {
        val person = personRepository.alle().find { it.identitetsnummer.value == fødselsnummer }
        if (person == null) {
            logg.info("Person med fødselsnummer $fødselsnummer er ikke lagt til i testen")
            return
        }

        val legacyPerson = LegacyPerson(
            aktørId = person.aktørId,
            fødselsnummer = person.identitetsnummer.value,
            vedtaksperioder = vedtaksperiodeRepository.alle()
                .filter { it.fødselsnummer == person.identitetsnummer.value }.map { vedtaksperiode ->
                    LegacyVedtaksperiode(
                        vedtaksperiodeId = vedtaksperiode.id.value,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        forkastet = vedtaksperiode.forkastet,
                        behandlinger = behandlingRepository.alle()
                            .filter { it.vedtaksperiodeId == vedtaksperiode.id }
                            .map { behandling ->
                                LegacyBehandling.fraLagring(
                                    id = behandling.id.value,
                                    vedtaksperiodeId = vedtaksperiode.id.value,
                                    utbetalingId = behandling.utbetalingId?.value,
                                    spleisBehandlingId = behandling.spleisBehandlingId?.value,
                                    skjæringstidspunkt = behandling.skjæringstidspunkt,
                                    fom = behandling.fom,
                                    tom = behandling.tom,
                                    tilstand = when (behandling.tilstand) {
                                        Behandling.Tilstand.VedtakFattet -> LegacyBehandling.VedtakFattet
                                        Behandling.Tilstand.VidereBehandlingAvklares -> LegacyBehandling.VidereBehandlingAvklares
                                        Behandling.Tilstand.AvsluttetUtenVedtak -> LegacyBehandling.AvsluttetUtenVedtak
                                        Behandling.Tilstand.AvsluttetUtenVedtakMedVarsler -> LegacyBehandling.AvsluttetUtenVedtakMedVarsler
                                        Behandling.Tilstand.KlarTilBehandling -> LegacyBehandling.KlarTilBehandling
                                    },
                                    tags = behandling.tags.toList(),
                                    vedtakBegrunnelse = behandling.spleisBehandlingId
                                        ?.let { vedtakBegrunnelseRepository.finn(it) }
                                        ?.let { vedtakBegrunnelse ->
                                            VedtakBegrunnelse(
                                                utfall = vedtakBegrunnelse.utfall,
                                                begrunnelse = vedtakBegrunnelse.tekst
                                            )
                                        },
                                    varsler = varselRepository.alle()
                                        .filter { it.behandlingUnikId == behandling.id }.map { varsel ->
                                            LegacyVarsel(
                                                id = varsel.id.value,
                                                varselkode = varsel.kode,
                                                opprettet = varsel.opprettetTidspunkt,
                                                vedtaksperiodeId = vedtaksperiode.id.value,
                                                status = when (varsel.status) {
                                                    Varsel.Status.AKTIV -> LegacyVarsel.Status.AKTIV
                                                    Varsel.Status.INAKTIV -> LegacyVarsel.Status.INAKTIV
                                                    Varsel.Status.GODKJENT -> LegacyVarsel.Status.GODKJENT
                                                    Varsel.Status.VURDERT -> LegacyVarsel.Status.VURDERT
                                                    Varsel.Status.AVVIST -> LegacyVarsel.Status.AVVIST
                                                    Varsel.Status.AVVIKLET -> LegacyVarsel.Status.AVVIKLET
                                                }
                                            )
                                        }.toSet(),
                                    yrkesaktivitetstype = behandling.yrkesaktivitetstype,
                                )
                            }
                    )
                },
            skjønnsfastsatteSykepengegrunnlag =
                sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer)
                    .sortedBy { it.opprettet }
                    .map {
                        SkjønnsfastsattSykepengegrunnlag.gjenopprett(
                            type = it.type,
                            årsak = it.årsak,
                            skjæringstidspunkt = it.skjæringstidspunkt,
                            begrunnelseFraMal = it.begrunnelseFraMal,
                            begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                            begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                            opprettet = it.opprettet,
                        )
                    },
            avviksvurderinger = avviksvurderingRepository.finnAvviksvurderinger(person.identitetsnummer.value),
        )
        val dtoFør = legacyPerson.toDto()
        legacyPerson.personScope()
        val dtoEtter = legacyPerson.toDto()
        if (dtoFør != dtoEtter) lagrePerson(dtoFør, dtoEtter)
    }

    private fun lagrePerson(
        dtoFør: PersonDto,
        dtoEtter: PersonDto,
    ) {
        val vedtaksperioderSomSkalLagres = finnEndredeVedtaksperioder(dtoFør, dtoEtter)
        legacyVedtaksperiodeRepository.lagreVedtaksperioder(dtoEtter.fødselsnummer, vedtaksperioderSomSkalLagres)
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

    override fun finnFødselsnumre(aktørId: String) =
        personRepository.alle().filter { it.aktørId == aktørId }.map { it.identitetsnummer.value }
}
