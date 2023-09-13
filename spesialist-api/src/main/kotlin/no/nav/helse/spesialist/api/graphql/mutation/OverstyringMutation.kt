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
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandling
import no.nav.helse.spesialist.api.saksbehandler.handlinger.SubsumsjonDto


class OverstyringMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {

    @Suppress("unused")
    suspend fun overstyrDager(
        overstyring: TidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrTidslinjeHandling(
                organisasjonsnummer = overstyring.organisasjonsnummer,
                fødselsnummer = overstyring.fodselsnummer,
                aktørId = overstyring.aktorId,
                begrunnelse = overstyring.begrunnelse,
                dager = overstyring.dager.map { it ->
                    OverstyrTidslinjeHandling.OverstyrDagDto(
                        dato = LocalDate.parse(it.dato),
                        type = it.type,
                        fraType = it.fraType,
                        grad = it.grad,
                        fraGrad = it.fraGrad,
                        subsumsjon = it.subsumsjon?.let { subsumsjon ->
                            SubsumsjonDto(
                                subsumsjon.paragraf,
                                subsumsjon.paragraf,
                                subsumsjon.bokstav
                            )
                        },
                    )
                })
            withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler) }
        } catch (e: Exception) {
            return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError("dager")).build()
        }
        DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    @Suppress("unused")
    suspend fun overstyrInntektOgRefusjon(
        overstyring: InntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrInntektOgRefusjonHandling(
                overstyring.aktorId,
                overstyring.fodselsnummer,
                LocalDate.parse(overstyring.skjaringstidspunkt),
                overstyring.arbeidsgivere.map { arbeidsgiver ->
                    OverstyrInntektOgRefusjonHandling.OverstyrArbeidsgiverDto(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        månedligInntekt = arbeidsgiver.manedligInntekt,
                        fraMånedligInntekt = arbeidsgiver.fraManedligInntekt,
                        refusjonsopplysninger = arbeidsgiver.refusjonsopplysninger?.map {
                            OverstyrInntektOgRefusjonHandling.OverstyrArbeidsgiverDto.RefusjonselementDto(
                                fom = LocalDate.parse(it.fom),
                                tom = LocalDate.parse(it.tom),
                                beløp = it.belop
                            )
                        },
                        fraRefusjonsopplysninger = arbeidsgiver.fraRefusjonsopplysninger?.map {
                            OverstyrInntektOgRefusjonHandling.OverstyrArbeidsgiverDto.RefusjonselementDto(
                                fom = LocalDate.parse(it.fom),
                                tom = LocalDate.parse(it.tom),
                                beløp = it.belop
                            )
                        },
                        begrunnelse = arbeidsgiver.begrunnelse,
                        forklaring = arbeidsgiver.forklaring,
                        subsumsjon = arbeidsgiver.subsumsjon?.let { subsumsjon ->
                            SubsumsjonDto(
                                subsumsjon.paragraf,
                                subsumsjon.ledd,
                                subsumsjon.bokstav
                            )
                        })
                })
            withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler) }
        } catch (e: Exception) {
            return@withContext DataFetcherResult.newResult<Boolean>()
                .error(kunneIkkeOverstyreError("inntekt og refusjon")).build()
        }
        DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    @Suppress("unused")
    suspend fun overstyrArbeidsforhold(
        overstyring: ArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
        try {
            val handling = OverstyrArbeidsforholdHandling(
                overstyring.fodselsnummer,
                aktørId = overstyring.aktorId,
                skjæringstidspunkt = LocalDate.parse(overstyring.skjaringstidspunkt),
                overstyrteArbeidsforhold = overstyring.overstyrteArbeidsforhold.map { arbeidsforhold ->
                    OverstyrArbeidsforholdHandling.ArbeidsforholdDto(
                        orgnummer = arbeidsforhold.orgnummer,
                        deaktivert = arbeidsforhold.deaktivert,
                        begrunnelse = arbeidsforhold.begrunnelse,
                        forklaring = arbeidsforhold.forklaring
                    )
                })
            withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(handling, saksbehandler) }
        } catch (e: Exception) {
            return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError("arbeidsforhold"))
                .build()
        }
        DataFetcherResult.newResult<Boolean>().data(true).build()
    }

    private fun kunneIkkeOverstyreError(overstyring: String): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke overstyre $overstyring")
            .extensions(mapOf("code" to 500)).build()
}
