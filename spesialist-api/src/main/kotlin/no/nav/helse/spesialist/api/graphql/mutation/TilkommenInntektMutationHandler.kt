package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator
import java.util.UUID

class TilkommenInntektMutationHandler(
    private val sessionFactory: SessionFactory,
    val meldingPubliserer: MeldingPubliserer,
) : TilkommenInntektMutationSchema {
    override fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<LeggTilTilkommenInntektResponse> =
        sessionFactory.transactionalSessionScope { session ->
            byggRespons(
                leggTilTilkommenInntekt(
                    fodselsnummer = fodselsnummer,
                    verdier = verdier,
                    notatTilBeslutter = notatTilBeslutter,
                    saksbehandler = env.graphQlContext.get(SAKSBEHANDLER),
                    session = session,
                ),
            )
        }

    fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ): LeggTilTilkommenInntektResponse {
        val periode = verdier.periode.fom tilOgMed verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = verdier.organisasjonsnummer,
            andreTilkomneInntekter = session.tilkommenInntektRepository.finnAlleForFødselsnummer(fodselsnummer),
            vedtaksperioder = session.vedtaksperiodeRepository.finnVedtaksperioder(fodselsnummer),
        )

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = fodselsnummer,
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer = fodselsnummer,
                        totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = verdier.periodebelop,
                ekskluderteUkedager = verdier.ekskluderteUkedager.toSet(),
            )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingPubliserer.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt lagt til",
        )

        return LeggTilTilkommenInntektResponse(tilkommenInntekt.id().value)
    }

    override fun endreTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val tilkommenInntekt =
                session.tilkommenInntektRepository.finn(TilkommenInntektId(tilkommenInntektId))
                    ?: error("Fant ikke tilkommen inntekt med tilkommentInntektId $tilkommenInntektId")

            endreTilkommenInntekt(
                tilkommenInntekt = tilkommenInntekt,
                endretTil = endretTil,
                notatTilBeslutter = notatTilBeslutter,
                saksbehandler = env.graphQlContext.get(SAKSBEHANDLER),
                session = session,
            )
        }

        return byggRespons(true)
    }

    fun endreTilkommenInntekt(
        tilkommenInntekt: TilkommenInntekt,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ) {
        val endretTilPeriode = endretTil.periode.fom tilOgMed endretTil.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = endretTilPeriode,
            organisasjonsnummer = endretTil.organisasjonsnummer,
            andreTilkomneInntekter =
                session.tilkommenInntektRepository
                    .finnAlleForFødselsnummer(tilkommenInntekt.fødselsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder = session.vedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntekt.fødselsnummer),
        )

        val arbeidsgiverFør = tilkommenInntekt.organisasjonsnummer
        val dagerFør = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpFør = tilkommenInntekt.dagbeløp()

        tilkommenInntekt.endreTil(
            organisasjonsnummer = endretTil.organisasjonsnummer,
            periode = endretTil.periode.fom tilOgMed endretTil.periode.tom,
            periodebeløp = endretTil.periodebelop,
            ekskluderteUkedager = endretTil.ekskluderteUkedager.toSet(),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                ).id(),
        )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        val arbeidsgiverEtter = tilkommenInntekt.organisasjonsnummer
        val dagerEtter = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpEtter = tilkommenInntekt.dagbeløp()

        val event =
            InntektsendringerEventBygger.forEndring(
                arbeidsgiverFør = arbeidsgiverFør,
                arbeidsgiverEtter = arbeidsgiverEtter,
                dagerFør = dagerFør,
                dagerEtter = dagerEtter,
                dagsbeløpFør = dagsbeløpFør,
                dagsbeløpEtter = dagsbeløpEtter,
            )

        event?.let {
            meldingPubliserer.publiser(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                hendelse = it,
                årsak = "tilkommen inntekt endret",
            )
        }
    }

    override fun fjernTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val tilkommenInntekt =
                session.tilkommenInntektRepository.finn(TilkommenInntektId(tilkommenInntektId))
                    ?: error("Fant ikke tilkommen inntekt med tilkommenInntektId $tilkommenInntektId")

            fjernTilkommenInntekt(
                tilkommenInntekt = tilkommenInntekt,
                notatTilBeslutter = notatTilBeslutter,
                saksbehandler = env.graphQlContext.get(SAKSBEHANDLER),
                session = session,
            )
        }

        return byggRespons(true)
    }

    fun fjernTilkommenInntekt(
        tilkommenInntekt: TilkommenInntekt,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ) {
        tilkommenInntekt.fjern(
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                ).id(),
        )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingPubliserer.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forFjernet(tilkommenInntekt),
            årsak = "tilkommen inntekt fjernet",
        )
    }

    override fun gjenopprettTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        sessionFactory.transactionalSessionScope { session ->
            val tilkommenInntekt =
                session.tilkommenInntektRepository.finn(TilkommenInntektId(tilkommenInntektId))
                    ?: error("Fant ikke tilkommen inntekt med tilkommenInntektId $tilkommenInntektId")

            gjenopprettTilkommenInntekt(
                tilkommenInntekt = tilkommenInntekt,
                endretTil = endretTil,
                notatTilBeslutter = notatTilBeslutter,
                saksbehandler = env.graphQlContext.get(SAKSBEHANDLER),
                session = session,
            )
        }

        return byggRespons(true)
    }

    fun gjenopprettTilkommenInntekt(
        tilkommenInntekt: TilkommenInntekt,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ) {
        val endretTilPeriode = endretTil.periode.fom tilOgMed endretTil.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = endretTilPeriode,
            organisasjonsnummer = endretTil.organisasjonsnummer,
            andreTilkomneInntekter =
                session.tilkommenInntektRepository
                    .finnAlleForFødselsnummer(tilkommenInntekt.fødselsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder = session.vedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntekt.fødselsnummer),
        )

        tilkommenInntekt.gjenopprett(
            organisasjonsnummer = endretTil.organisasjonsnummer,
            periode = endretTilPeriode,
            periodebeløp = endretTil.periodebelop,
            ekskluderteUkedager = endretTil.ekskluderteUkedager.toSet(),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                ).id(),
        )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingPubliserer.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt gjenopprettet",
        )
    }

    private fun finnEllerOpprettTotrinnsvurdering(
        fodselsnummer: String,
        totrinnsvurderingRepository: TotrinnsvurderingRepository,
    ): Totrinnsvurdering =
        totrinnsvurderingRepository.finnAktivForPerson(fodselsnummer)
            ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)
}
