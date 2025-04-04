package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.VedtaksperiodeRepository
import no.nav.helse.modell.melding.InntektsendringerEvent
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.gradering.TilkommenInntekt
import no.nav.helse.spesialist.domain.gradering.TilkommenInntektId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TilkommenInntektMutationHandler(
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val tilkommenInntektRepository: TilkommenInntektRepository,
    private val meldingPubliserer: MeldingPubliserer,
    // tidligste fom må være skjæringstidspunkt +1 (fiksa se under)
    // perioden må være innenfor vedtaksperiodene (fiksa se under)
    // kan ikke legge inn perioder som overlapper med eksisterende perioder, selvom de er fjernet (fiksa se under)
) : TilkommenInntektMutationSchema {
    override fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Unit> {
        sjekkAtErInnenforEtSykefraværstilfelle(verdier)
        sjekkAtIkkeOverlapperMedEksisterende(verdier)
        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = fodselsnummer,
                saksbehandlerOid = SaksbehandlerOid(env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).oid),
                notatTilBeslutter = notatTilBeslutter,
                organisasjonsnummer = verdier.organisasjonsnummer,
                fom = verdier.fom,
                tom = verdier.tom,
                periodebeløp = verdier.periodebelop,
                dager = verdier.dager,
            )
        tilkommenInntektRepository.lagre(tilkommenInntekt)
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

        return DataFetcherResult.newResult<Unit>().build()
    }

    override fun endreTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Unit> {
        val tilkommenInntekt = tilkommenInntektRepository.finn(TilkommenInntektId.fra(fodselsnummer, uuid))

        sjekkAtErInnenforEtSykefraværstilfelle(endretTil)
        sjekkAtIkkeOverlapperMedEksisterende(endretTil)

        val dagerFør: Set<LocalDate> = tilkommenInntekt.dager
        val dagsbeløpFør: BigDecimal = tilkommenInntekt.dagbeløp()
        val arbeidsgiverFør: String = tilkommenInntekt.organisasjonsnummer

        tilkommenInntekt.endreTil(
            organisasjonsnummer = endretTil.organisasjonsnummer,
            fom = endretTil.fom,
            tom = endretTil.tom,
            periodebeløp = endretTil.periodebelop,
            dager = endretTil.dager,
            saksbehandlerOid = SaksbehandlerOid(env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).oid),
            notatTilBeslutter = notatTilBeslutter,
        )

        val dagerEtter = tilkommenInntekt.dager
        val dagsbeløpEtter = tilkommenInntekt.dagbeløp()
        val arbeidsgiverEtter = tilkommenInntekt.organisasjonsnummer

        // Her må vi finne frem til liste av det som skal sendes som inntekt til spesidaler, og det som skal nullstilles
        // Og så mappe det til en melding til Spesidaler og publisere den

        return DataFetcherResult.newResult<Unit>().build()
    }

    private fun sjekkAtErInnenforEtSykefraværstilfelle(tilkommenInntektOverstyring: ApiTilkommenInntektOverstyring) {
        val fødselsnummer = tilkommenInntektOverstyring.fodselsnummer
        val periode = Periode(fom = tilkommenInntektOverstyring.fom, tom = tilkommenInntektOverstyring.tom)
        val sykefraværstilfellePerioder =
            vedtaksperiodeRepository.finnVedtaksperioder(fødselsnummer).map {
                it.behandlinger.last()
            }.groupBy({ it.skjæringstidspunkt }, { it.tom }).map { (skjæringstidspunkt, listeAvTom) ->
                Periode(fom = skjæringstidspunkt, tom = listeAvTom.max())
            }
        if (sykefraværstilfellePerioder.none { (fom, tom) ->
                tilkommenInntektOverstyring.fom > fom && tilkommenInntektOverstyring.tom <= tom
            }
        ) {
            error("Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle")
        }
    }

    private fun sjekkAtIkkeOverlapperMedEksisterende(tilkommenInntektOverstyring: ApiTilkommenInntektOverstyring) {
        val periode = Periode(fom = tilkommenInntektOverstyring.fom, tom = tilkommenInntektOverstyring.tom)
        val eksisterendePerioder =
            tilkommenInntektRepository.finnAlleForFødselsnummerOgOrganisasjonsnummer(
                fødselsnummer = tilkommenInntektOverstyring.fodselsnummer,
                organisasjonsnummer = tilkommenInntektOverstyring.organisasjonsnummer,
            )
        if (eksisterendePerioder.any { it.periode overlapperMed periode }) {
            error("Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt")
        }
    }
}
