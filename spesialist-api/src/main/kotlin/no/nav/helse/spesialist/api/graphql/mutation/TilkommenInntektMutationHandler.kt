package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektRequest
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import no.nav.helse.spesialist.domain.gradering.TilkommenInntekt
import no.nav.helse.spesialist.domain.gradering.TilkommenInntektId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TilkommenInntektMutationHandler(
    private val sessionFactory: SessionFactory,
    private val meldingPubliserer: MeldingPubliserer,
) : TilkommenInntektMutationSchema {
    override fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val periode = Periode(fom = verdier.fom, tom = verdier.tom)
            verifiserAtErInnenforEtSykefraværstilfelle(
                periode = periode,
                fødselsnummer = fodselsnummer,
                vedtaksperiodeRepository = session.vedtaksperiodeRepository,
            )
            val alleTilkomneInntekterForFødselsnummer =
                session.tilkommenInntektRepository.finnAlleForFødselsnummer(
                    fødselsnummer = fodselsnummer,
                )
            TilkommenInntekt.validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
                fom = verdier.fom,
                tom = verdier.tom,
                organisasjonsnummer = verdier.organisasjonsnummer,
                alleTilkomneInntekterForFødselsnummer = alleTilkomneInntekterForFødselsnummer,
            )

            val tilkommenInntekt =
                TilkommenInntekt.ny(
                    fødselsnummer = fodselsnummer,
                    saksbehandlerIdent = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident,
                    notatTilBeslutter = notatTilBeslutter,
                    totrinnsvurderingId =
                        finnEllerOpprettTotrinnsvurdering(
                            fodselsnummer,
                            session.totrinnsvurderingRepository,
                        ).id(),
                    organisasjonsnummer = verdier.organisasjonsnummer,
                    fom = verdier.fom,
                    tom = verdier.tom,
                    periodebeløp = verdier.periodebelop,
                    dager = verdier.dager.toSet(),
                )
            session.tilkommenInntektRepository.lagre(tilkommenInntekt)

            meldingPubliserer.publiser(
                fødselsnummer = tilkommenInntekt.id().fødselsnummer,
                hendelse =
                    InntektsendringerEvent(
                        inntektskilder =
                            listOf(
                                InntektsendringerEvent.Inntektskilde(
                                    inntektskilde = tilkommenInntekt.organisasjonsnummer,
                                    inntekter =
                                        tilkommenInntekt.dager.sorted().map { dag ->
                                            InntektsendringerEvent.Inntektskilde.Inntekt(
                                                fom = dag,
                                                tom = dag,
                                                dagsbeløp = tilkommenInntekt.dagbeløp(),
                                            )
                                        },
                                    nullstill = emptyList(),
                                ),
                            ),
                    ),
                årsak = "tilkommen inntekt lagt til",
            )
        }

        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    private fun finnEllerOpprettTotrinnsvurdering(
        fodselsnummer: String,
        totrinnsvurderingRepository: TotrinnsvurderingRepository,
    ): Totrinnsvurdering =
        totrinnsvurderingRepository.finn(fodselsnummer)
            ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)

    override fun endreTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val vedtaksperiodeRepository: VedtaksperiodeRepository = session.vedtaksperiodeRepository
            val tilkommenInntektRepository: TilkommenInntektRepository = session.tilkommenInntektRepository
            val totrinnsvurderingRepository: TotrinnsvurderingRepository = session.totrinnsvurderingRepository
            val endretTilPeriode = Periode(fom = endretTil.fom, tom = endretTil.tom)
            val tilkommenInntekt =
                tilkommenInntektRepository.finn(TilkommenInntektId.fra(fodselsnummer, uuid))
                    ?: error("Fant ikke tilkommen inntekt med fødselsnummer $fodselsnummer og uuid $uuid")

            verifiserAtErInnenforEtSykefraværstilfelle(
                periode = endretTilPeriode,
                fødselsnummer = fodselsnummer,
                vedtaksperiodeRepository = vedtaksperiodeRepository,
            )
            val andreTilkomneInntekterForOrganisasjonsnummer =
                tilkommenInntektRepository.finnAlleForFødselsnummer(
                    fødselsnummer = fodselsnummer,
                )
                    .filter { it.organisasjonsnummer == endretTil.organisasjonsnummer }.minus(tilkommenInntekt)
            if (andreTilkomneInntekterForOrganisasjonsnummer.any { endretTilPeriode overlapper it.periode }) {
                error("Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt")
            }

            val dagerFør: Set<LocalDate> = tilkommenInntekt.dager
            val dagsbeløpFør: BigDecimal = tilkommenInntekt.dagbeløp()
            val arbeidsgiverFør: String = tilkommenInntekt.organisasjonsnummer

            tilkommenInntekt.endreTil(
                organisasjonsnummer = endretTil.organisasjonsnummer,
                fom = endretTil.fom,
                tom = endretTil.tom,
                periodebeløp = endretTil.periodebelop,
                dager = endretTil.dager.toSet(),
                saksbehandlerIdent = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident,
                notatTilBeslutter = notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer,
                        totrinnsvurderingRepository,
                    ).id(),
            )

            val dagerEtter = tilkommenInntekt.dager
            val dagsbeløpEtter = tilkommenInntekt.dagbeløp()
            val arbeidsgiverEtter = tilkommenInntekt.organisasjonsnummer

            val event =
                byggInntektsendringerEvent(
                    arbeidsgiverFør = arbeidsgiverFør,
                    arbeidsgiverEtter = arbeidsgiverEtter,
                    dagerFør = dagerFør,
                    dagerEtter = dagerEtter,
                    dagsbeløpFør = dagsbeløpFør,
                    dagsbeløpEtter = dagsbeløpEtter,
                )

            event?.let {
                meldingPubliserer.publiser(
                    fødselsnummer = tilkommenInntekt.id().fødselsnummer,
                    hendelse = it,
                    årsak = "tilkommen inntekt endret",
                )
            }
        }

        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    override fun gjenopprettTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val vedtaksperiodeRepository: VedtaksperiodeRepository = session.vedtaksperiodeRepository
            val tilkommenInntektRepository: TilkommenInntektRepository = session.tilkommenInntektRepository
            val totrinnsvurderingRepository: TotrinnsvurderingRepository = session.totrinnsvurderingRepository
            val endretTilPeriode = Periode(fom = endretTil.fom, tom = endretTil.tom)
            val tilkommenInntekt =
                tilkommenInntektRepository.finn(TilkommenInntektId.fra(fodselsnummer, uuid))
                    ?: error("Fant ikke tilkommen inntekt med fødselsnummer $fodselsnummer og uuid $uuid")

            verifiserAtErInnenforEtSykefraværstilfelle(
                periode = endretTilPeriode,
                fødselsnummer = fodselsnummer,
                vedtaksperiodeRepository = vedtaksperiodeRepository,
            )
            val andreTilkomneInntekterForOrganisasjonsnummer =
                tilkommenInntektRepository.finnAlleForFødselsnummer(
                    fødselsnummer = fodselsnummer,
                )
                    .filter { it.organisasjonsnummer == endretTil.organisasjonsnummer }.minus(tilkommenInntekt)
            if (andreTilkomneInntekterForOrganisasjonsnummer.any { endretTilPeriode overlapper it.periode }) {
                error("Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt")
            }

            tilkommenInntekt.gjenopprett(
                organisasjonsnummer = endretTil.organisasjonsnummer,
                fom = endretTil.fom,
                tom = endretTil.tom,
                periodebeløp = endretTil.periodebelop,
                dager = endretTil.dager.toSet(),
                saksbehandlerIdent = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident,
                notatTilBeslutter = notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer,
                        totrinnsvurderingRepository,
                    ).id(),
            )

            meldingPubliserer.publiser(
                fødselsnummer = tilkommenInntekt.id().fødselsnummer,
                hendelse =
                    InntektsendringerEvent(
                        inntektskilder =
                            listOf(
                                InntektsendringerEvent.Inntektskilde(
                                    inntektskilde = tilkommenInntekt.organisasjonsnummer,
                                    inntekter =
                                        tilkommenInntekt.dager.sorted().map { dag ->
                                            InntektsendringerEvent.Inntektskilde.Inntekt(
                                                fom = dag,
                                                tom = dag,
                                                dagsbeløp = tilkommenInntekt.dagbeløp(),
                                            )
                                        },
                                    nullstill = emptyList(),
                                ),
                            ),
                    ),
                årsak = "tilkommen inntekt gjenopprettet",
            )
        }

        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    override fun fjernTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val tilkommenInntektRepository: TilkommenInntektRepository = session.tilkommenInntektRepository
            val totrinnsvurderingRepository: TotrinnsvurderingRepository = session.totrinnsvurderingRepository
            val tilkommenInntekt =
                tilkommenInntektRepository.finn(TilkommenInntektId.fra(fodselsnummer, uuid))
                    ?: error("Fant ikke tilkommen inntekt med fødselsnummer $fodselsnummer og uuid $uuid")

            tilkommenInntekt.fjern(
                saksbehandlerIdent = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident,
                notatTilBeslutter = notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer,
                        totrinnsvurderingRepository,
                    ).id(),
            )

            val event =
                InntektsendringerEvent(
                    inntektskilder =
                        listOf(
                            InntektsendringerEvent.Inntektskilde(
                                inntektskilde = tilkommenInntekt.organisasjonsnummer,
                                inntekter = emptyList(),
                                nullstill = tilkommenInntekt.dager.tilNullstillinger(),
                            ),
                        ),
                )

            meldingPubliserer.publiser(
                fødselsnummer = tilkommenInntekt.id().fødselsnummer,
                hendelse = event,
                årsak = "tilkommen inntekt fjernet",
            )
        }

        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    private fun byggInntektsendringerEvent(
        arbeidsgiverFør: String,
        arbeidsgiverEtter: String,
        dagerFør: Set<LocalDate>,
        dagerEtter: Set<LocalDate>,
        dagsbeløpFør: BigDecimal,
        dagsbeløpEtter: BigDecimal,
    ): InntektsendringerEvent? =
        if (arbeidsgiverFør != arbeidsgiverEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverFør,
                            inntekter = emptyList(),
                            nullstill = dagerFør.tilNullstillinger(),
                        ),
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.tilInntekter(dagsbeløpEtter),
                            nullstill = emptyList(),
                        ),
                    ),
            )
        } else if (dagsbeløpFør != dagsbeløpEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.tilInntekter(dagsbeløpEtter),
                            nullstill = dagerFør.minus(dagerEtter).tilNullstillinger(),
                        ),
                    ),
            )
        } else if (dagerFør != dagerEtter) {
            InntektsendringerEvent(
                inntektskilder =
                    listOf(
                        InntektsendringerEvent.Inntektskilde(
                            inntektskilde = arbeidsgiverEtter,
                            inntekter = dagerEtter.minus(dagerFør).tilInntekter(dagsbeløpEtter),
                            nullstill = dagerFør.minus(dagerEtter).tilNullstillinger(),
                        ),
                    ),
            )
        } else {
            null
        }

    private fun Set<LocalDate>.tilInntekter(dagsbeløp: BigDecimal) =
        tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Inntekt(
                fom = periode.fom,
                tom = periode.tom,
                dagsbeløp = dagsbeløp,
            )
        }

    private fun Set<LocalDate>.tilNullstillinger() =
        tilPerioder().map { periode ->
            InntektsendringerEvent.Inntektskilde.Nullstilling(
                fom = periode.fom,
                tom = periode.tom,
            )
        }

    private fun verifiserAtErInnenforEtSykefraværstilfelle(
        periode: Periode,
        fødselsnummer: String,
        vedtaksperiodeRepository: VedtaksperiodeRepository,
    ) {
        val sykefraværstilfellePerioder =
            finnSykefraværstillfellerSomPerioder(fødselsnummer, vedtaksperiodeRepository)
                .map { it.utenFørsteDag() }
                .filterNot { it.datoer().isEmpty() }

        if (sykefraværstilfellePerioder.none { periode erInnenfor it }) {
            error("Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle")
        }
    }

    private fun finnSykefraværstillfellerSomPerioder(
        fødselsnummer: String,
        vedtaksperiodeRepository: VedtaksperiodeRepository,
    ) = vedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer)
        .map { it.behandlinger.last() }
        .groupBy(BehandlingDto::skjæringstidspunkt, BehandlingDto::tom)
        .map { (skjæringstidspunkt, listeAvTom) -> Periode(fom = skjæringstidspunkt, tom = listeAvTom.max()) }
}

private fun Periode.utenFørsteDag() = copy(fom = fom.plusDays(1))
