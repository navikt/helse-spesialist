package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SubsumsjonDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class OverstyringMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {

    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutation::class.java)
    }

    @Suppress("unused")
    suspend fun overstyrDager(
        overstyring: TidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrTidslinjeHandlingFraApi(
                vedtaksperiodeId = UUID.fromString(overstyring.vedtaksperiodeId),
                organisasjonsnummer = overstyring.organisasjonsnummer,
                fødselsnummer = overstyring.fodselsnummer,
                aktørId = overstyring.aktorId,
                begrunnelse = overstyring.begrunnelse,
                dager = overstyring.dager.map { it ->
                    OverstyrTidslinjeHandlingFraApi.OverstyrDagDto(
                        dato = LocalDate.parse(it.dato),
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        subsumsjon = it.subsumsjon?.let { subsumsjon ->
                            SubsumsjonDto(
                                subsumsjon.paragraf,
                                subsumsjon.ledd,
                                subsumsjon.bokstav
                            )
                        },
                    )
                })
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
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrInntektOgRefusjonHandlingFraApi(
                aktørId = overstyring.aktorId,
                fødselsnummer = overstyring.fodselsnummer,
                skjæringstidspunkt = LocalDate.parse(overstyring.skjaringstidspunkt),
                arbeidsgivere = overstyring.arbeidsgivere.map { arbeidsgiver ->
                    OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverDto(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        månedligInntekt = arbeidsgiver.manedligInntekt,
                        fraMånedligInntekt = arbeidsgiver.fraManedligInntekt,
                        refusjonsopplysninger = arbeidsgiver.refusjonsopplysninger?.map {
                            OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverDto.RefusjonselementDto(
                                fom = LocalDate.parse(it.fom),
                                tom = LocalDate.parse(it.tom),
                                beløp = it.belop
                            )
                        },
                        fraRefusjonsopplysninger = arbeidsgiver.fraRefusjonsopplysninger?.map {
                            OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverDto.RefusjonselementDto(
                                fom = LocalDate.parse(it.fom),
                                tom = LocalDate.parse(it.tom),
                                beløp = it.belop
                            )
                        },
                        begrunnelse = arbeidsgiver.begrunnelse,
                        forklaring = arbeidsgiver.forklaring,
                        subsumsjon = arbeidsgiver.subsumsjon?.let { subsumsjon ->
                            SubsumsjonDto(
                                paragraf = subsumsjon.paragraf,
                                ledd = subsumsjon.ledd,
                                bokstav = subsumsjon.bokstav
                            )
                        })
                })
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
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrArbeidsforholdHandlingFraApi(
                overstyring.fodselsnummer,
                aktørId = overstyring.aktorId,
                skjæringstidspunkt = LocalDate.parse(overstyring.skjaringstidspunkt),
                overstyrteArbeidsforhold = overstyring.overstyrteArbeidsforhold.map { arbeidsforhold ->
                    OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdDto(
                        orgnummer = arbeidsforhold.orgnummer,
                        deaktivert = arbeidsforhold.deaktivert,
                        begrunnelse = arbeidsforhold.begrunnelse,
                        forklaring = arbeidsforhold.forklaring
                    )
                })
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
