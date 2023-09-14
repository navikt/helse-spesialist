package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.ANNET
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.OMREGNET_ARSINNTEKT
import no.nav.helse.spesialist.api.graphql.schema.SkjonnsfastsettelseArbeidsgiver.SkjonnsfastsettelseType.RAPPORTERT_ARSINNTEKT
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto.SkjønnsfastsettingstypeDto.RAPPORTERT_ÅRSINNTEKT
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SubsumsjonDto

class SkjonnsfastsettelseMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {
    @Suppress("unused")
    suspend fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: Skjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
                skjonnsfastsettelse.aktorId,
                skjonnsfastsettelse.fodselsnummer,
                LocalDate.parse(skjonnsfastsettelse.skjaringstidspunkt),
                skjonnsfastsettelse.arbeidsgivere.map { arbeidsgiver ->
                    SkjønnsfastsettSykepengegrunnlagHandlingFraApi.SkjønnsfastsattArbeidsgiverDto(
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
                        arbeidsgiver.subsumsjon?.let { subsumsjon ->
                            SubsumsjonDto(
                                subsumsjon.paragraf,
                                subsumsjon.ledd,
                                subsumsjon.bokstav
                            )
                        },
                        arbeidsgiver.initierendeVedtaksperiodeId
                    )
                }
            )
            withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler.value) }
        } catch (e: Exception) {
            return@withContext DataFetcherResult.newResult<Boolean>()
                .error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError())
                .build()
        }
        DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    private fun kunneIkkeSkjønnsfastsetteSykepengegrunnlagError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke skjønnsfastsette sykepengegrunnlag")
            .extensions(mapOf("code" to 500)).build()
}
