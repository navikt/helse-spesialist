package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQLConfiguration
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutation
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutation
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationSchema
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQuerySchema

class SpesialistSchema(
    val queryHandlers: QueryHandlers,
    val mutationHandlers: MutationHandlers,
) {
    class QueryHandlers(
        val person: PersonQuerySchema,
    )

    class MutationHandlers(
        val overstyring: OverstyringMutationSchema,
        val totrinnsvurdering: TotrinnsvurderingMutationSchema,
    )

    fun setup(schemaConfiguration: GraphQLConfiguration.SchemaConfiguration) {
        schemaConfiguration.packages = listOf("no.nav.helse.spesialist.api.graphql")

        schemaConfiguration.queries =
            listOf(
                PersonQuery(handler = queryHandlers.person),
            )

        schemaConfiguration.mutations =
            listOf(
                OverstyringMutation(handler = mutationHandlers.overstyring),
                TotrinnsvurderingMutation(handler = mutationHandlers.totrinnsvurdering),
            )

        schemaConfiguration.hooks = schemaGeneratorHooks
    }
}
