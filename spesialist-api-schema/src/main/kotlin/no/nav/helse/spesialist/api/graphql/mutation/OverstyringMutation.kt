package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring

interface OverstyringMutationSchema : Mutation {
    fun overstyrDager(
        overstyring: ApiTidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun overstyrInntektOgRefusjon(
        overstyring: ApiInntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun overstyrArbeidsforhold(
        overstyring: ApiArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun overstyrTilkommenInntekt(
        @GraphQLName("overstyring")
        apiOverstyring: ApiTilkommenInntektOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class OverstyringMutation(private val handler: OverstyringMutationSchema) : OverstyringMutationSchema by handler
