package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.time.LocalDate

class OverstyringMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutation::class.java)
    }

    @Suppress("unused")
    suspend fun overstyrDager(
        overstyring: TidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
            try {
                val handling =
                    OverstyrTidslinjeHandlingFraApi(
                        vedtaksperiodeId = overstyring.vedtaksperiodeId,
                        organisasjonsnummer = overstyring.organisasjonsnummer,
                        fødselsnummer = overstyring.fodselsnummer,
                        aktørId = overstyring.aktorId,
                        begrunnelse = overstyring.begrunnelse,
                        dager =
                            overstyring.dager.map { it ->
                                OverstyrTidslinjeHandlingFraApi.OverstyrDagFraApi(
                                    dato = it.dato,
                                    type = it.type,
                                    fraType = it.fraType,
                                    grad = it.grad,
                                    fraGrad = it.fraGrad,
                                    lovhjemmel =
                                        it.lovhjemmel?.let { lovhjemmel ->
                                            LovhjemmelFraApi(
                                                lovhjemmel.paragraf,
                                                lovhjemmel.ledd,
                                                lovhjemmel.bokstav,
                                                lovhjemmel.lovverk,
                                                lovhjemmel.lovverksversjon,
                                            )
                                        },
                                )
                            },
                    )
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler.value) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("dager")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError).build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    @Suppress("unused")
    suspend fun overstyrInntektOgRefusjon(
        overstyring: InntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
            try {
                val handling =
                    OverstyrInntektOgRefusjonHandlingFraApi(
                        aktørId = overstyring.aktorId,
                        fødselsnummer = overstyring.fodselsnummer,
                        skjæringstidspunkt = overstyring.skjaringstidspunkt,
                        arbeidsgivere =
                            overstyring.arbeidsgivere.map { arbeidsgiver ->
                                OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi(
                                    organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                                    månedligInntekt = arbeidsgiver.manedligInntekt,
                                    fraMånedligInntekt = arbeidsgiver.fraManedligInntekt,
                                    refusjonsopplysninger =
                                        arbeidsgiver.refusjonsopplysninger?.map { refusjonselement ->
                                            OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(
                                                fom = refusjonselement.fom,
                                                tom = refusjonselement.tom,
                                                beløp = refusjonselement.belop,
                                            )
                                        },
                                    fraRefusjonsopplysninger =
                                        arbeidsgiver.fraRefusjonsopplysninger?.map { refusjonselement ->
                                            OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi.RefusjonselementFraApi(
                                                fom = refusjonselement.fom,
                                                tom = refusjonselement.tom,
                                                beløp = refusjonselement.belop,
                                            )
                                        },
                                    begrunnelse = arbeidsgiver.begrunnelse,
                                    forklaring = arbeidsgiver.forklaring,
                                    lovhjemmel =
                                        arbeidsgiver.lovhjemmel?.let { lovhjemmel ->
                                            LovhjemmelFraApi(
                                                paragraf = lovhjemmel.paragraf,
                                                ledd = lovhjemmel.ledd,
                                                bokstav = lovhjemmel.bokstav,
                                                lovverk = lovhjemmel.lovverk,
                                                lovverksversjon = lovhjemmel.lovverksversjon,
                                            )
                                        },
                                )
                            },
                    )
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler.value) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("inntekt og refusjon")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>()
                    .error(kunneIkkeOverstyreError).build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    @Suppress("unused")
    suspend fun overstyrArbeidsforhold(
        overstyring: ArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
            try {
                val handling =
                    OverstyrArbeidsforholdHandlingFraApi(
                        overstyring.fodselsnummer,
                        aktørId = overstyring.aktorId,
                        skjæringstidspunkt = overstyring.skjaringstidspunkt,
                        overstyrteArbeidsforhold =
                            overstyring.overstyrteArbeidsforhold.map { arbeidsforhold ->
                                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi(
                                    orgnummer = arbeidsforhold.orgnummer,
                                    deaktivert = arbeidsforhold.deaktivert,
                                    begrunnelse = arbeidsforhold.begrunnelse,
                                    forklaring = arbeidsforhold.forklaring,
                                    lovhjemmel =
                                        arbeidsforhold.lovhjemmel?.let { lovhjemmel ->
                                            LovhjemmelFraApi(
                                                paragraf = lovhjemmel.paragraf,
                                                ledd = lovhjemmel.ledd,
                                                bokstav = lovhjemmel.bokstav,
                                                lovverk = lovhjemmel.lovverk,
                                                lovverksversjon = lovhjemmel.lovverksversjon,
                                            )
                                        },
                                )
                            },
                    )
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler.value) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("arbeidsforhold")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError)
                    .build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    private fun kunneIkkeOverstyreError(overstyring: String): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke overstyre $overstyring")
            .extensions(mapOf("code" to 500)).build()
}
