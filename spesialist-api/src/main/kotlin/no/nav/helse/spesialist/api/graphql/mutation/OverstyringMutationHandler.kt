package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.overstyringUnitOfWork
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt
import no.nav.helse.spesialist.api.graphql.saksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OverstyringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val sessionFactory: SessionFactory,
    private val meldingPubliserer: MeldingPubliserer,
) : OverstyringMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutationHandler::class.java)
    }

    override fun overstyrDager(
        overstyring: ApiTidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        } catch (e: Exception) {
            val feilmelding = "Kunne ikke overstyre dager"
            logg.error(feilmelding, e)
            lagFeilrespons(feilmelding)
        }
    }

    override fun overstyrInntektOgRefusjon(
        overstyring: ApiInntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler: SaksbehandlerFraApi = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        } catch (e: Exception) {
            val feilmelding = "Kunne ikke overstyre inntekt og refusjon"
            logg.error(feilmelding, e)
            lagFeilrespons(feilmelding)
        }
    }

    override fun overstyrArbeidsforhold(
        overstyring: ApiArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler: SaksbehandlerFraApi = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        } catch (e: Exception) {
            val feilmelding = "Kunne ikke overstyre arbeidsforhold"
            logg.error(feilmelding, e)
            lagFeilrespons(feilmelding)
        }
    }

    override fun overstyrTilkommenInntekt(
        apiOverstyring: ApiTilkommenInntektOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.saksbehandler().toDomain()
        val overstyring = apiOverstyring.toDomain(saksbehandler.id())
        overstyringUnitOfWork(
            overstyring = overstyring,
            saksbehandler = saksbehandler,
            sessionFactory = sessionFactory,
        ) {
            meldingPubliserer.publiser(
                fødselsnummer = overstyring.fødselsnummer,
                hendelse = overstyring.byggEvent(),
                årsak = "overstyring av tilkommen inntekt",
            )
        }
        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    private fun SaksbehandlerFraApi.toDomain() =
        Saksbehandler(
            id = SaksbehandlerOid(value = this.oid),
            navn = navn,
            epost = epost,
            ident = ident,
        )

    private fun lagFeilrespons(feilmelding: String): DataFetcherResult<Boolean> =
        DataFetcherResult.newResult<Boolean>().error(
            GraphqlErrorException.newErrorException().message(feilmelding).extensions(mapOf("code" to 500)).build(),
        ).data(false).build()

    private fun ApiTilkommenInntektOverstyring.toDomain(saksbehandlerOid: SaksbehandlerOid): OverstyrTilkommenInntekt {
        return OverstyrTilkommenInntekt.ny(
            fødselsnummer = this.fodselsnummer,
            saksbehandlerOid = saksbehandlerOid,
            aktørId = this.aktorId,
            vedtaksperiodeId = this.vedtaksperiodeId,
            nyEllerEndredeInntekter =
                this.lagtTilEllerEndret.map { lagtTilEllerEndret ->
                    OverstyrTilkommenInntekt.NyEllerEndretInntekt(
                        lagtTilEllerEndret.organisasjonsnummer,
                        lagtTilEllerEndret.perioder.map {
                            OverstyrTilkommenInntekt.NyEllerEndretInntekt.PeriodeMedBeløp(
                                it.fom,
                                it.tom,
                                it.periodeBelop,
                            )
                        },
                    )
                },
            fjernedeInntekter =
                this.fjernet.map { fjernet ->
                    OverstyrTilkommenInntekt.FjernetInntekt(
                        fjernet.organisasjonsnummer,
                        fjernet.perioder.map {
                            OverstyrTilkommenInntekt.FjernetInntekt.PeriodeUtenBeløp(
                                it.fom,
                                it.tom,
                            )
                        },
                    )
                },
        )
    }
}
