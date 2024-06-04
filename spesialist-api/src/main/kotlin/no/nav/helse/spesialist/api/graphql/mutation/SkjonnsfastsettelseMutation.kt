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
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi.SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SkjonnsfastsettelseMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(SkjonnsfastsettelseMutation::class.java)
    }

    @Suppress("unused")
    suspend fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: Skjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
            try {
                val handling =
                    SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
                        skjonnsfastsettelse.aktorId,
                        skjonnsfastsettelse.fodselsnummer,
                        skjonnsfastsettelse.skjaringstidspunkt,
                        skjonnsfastsettelse.arbeidsgivere.map { arbeidsgiver ->
                            SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverFraApi(
                                arbeidsgiver.organisasjonsnummer,
                                arbeidsgiver.arlig,
                                arbeidsgiver.fraArlig,
                                arbeidsgiver.arsak,
                                arbeidsgiver.type.let {
                                    when (it) {
                                        OMREGNET_ARSINNTEKT -> OMREGNET_ÅRSINNTEKT
                                        RAPPORTERT_ARSINNTEKT -> RAPPORTERT_ÅRSINNTEKT
                                        ANNET -> SkjønnsfastsettingstypeDto.ANNET
                                    }
                                },
                                arbeidsgiver.begrunnelseMal,
                                arbeidsgiver.begrunnelseFritekst,
                                arbeidsgiver.begrunnelseKonklusjon,
                                arbeidsgiver.lovhjemmel?.let { lovhjemmel ->
                                    LovhjemmelFraApi(
                                        paragraf = lovhjemmel.paragraf,
                                        ledd = lovhjemmel.ledd,
                                        bokstav = lovhjemmel.bokstav,
                                        lovverk = lovhjemmel.lovverk,
                                        lovverksversjon = lovhjemmel.lovverksversjon,
                                    )
                                },
                                arbeidsgiver.initierendeVedtaksperiodeId,
                            )
                        },
                    )
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler.value) }
            } catch (e: Exception) {
                val kunneIkkeSkjønnsfastsetteSykepengegrunnlagError = kunneIkkeSkjønnsfastsetteSykepengegrunnlagError()
                logg.error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>()
                    .error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError)
                    .build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    private fun kunneIkkeSkjønnsfastsetteSykepengegrunnlagError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke skjønnsfastsette sykepengegrunnlag")
            .extensions(mapOf("code" to 500)).build()
}
