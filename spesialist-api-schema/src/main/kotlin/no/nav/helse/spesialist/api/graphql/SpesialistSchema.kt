package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQLConfiguration
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutation
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutation
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutation
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutation
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutation
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutation
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.StansAutomatiskBehandlingMutation
import no.nav.helse.spesialist.api.graphql.mutation.StansAutomatiskBehandlingMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutation
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutation
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutation
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutationSchema
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutation
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationSchema
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQuery
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQuerySchema
import no.nav.helse.spesialist.api.graphql.query.OppgaverQuery
import no.nav.helse.spesialist.api.graphql.query.OppgaverQuerySchema
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQuery
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQuerySchema
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQuerySchema

class SpesialistSchema(
    val queryHandlers: QueryHandlers,
    val mutationHandlers: MutationHandlers,
) {
    class QueryHandlers(
        val person: PersonQuerySchema,
        val oppgaver: OppgaverQuerySchema,
        val behandlingsstatistikk: BehandlingsstatistikkQuerySchema,
        val opptegnelse: OpptegnelseQuerySchema,
    )

    class MutationHandlers(
        val notat: NotatMutationSchema,
        val varsel: VarselMutationSchema,
        val tildeling: TildelingMutationSchema,
        val opptegnelse: OpptegnelseMutationSchema,
        val overstyring: OverstyringMutationSchema,
        val skjonnsfastsettelse: SkjonnsfastsettelseMutationSchema,
        val minimumSykdomsgrad: MinimumSykdomsgradMutationSchema,
        val totrinnsvurdering: TotrinnsvurderingMutationSchema,
        val vedtak: VedtakMutationSchema,
        val person: PersonMutationSchema,
        val paVent: PaVentMutationSchema,
        val stansAutomatiskBehandling: StansAutomatiskBehandlingMutationSchema,
    )

    fun setup(schemaConfiguration: GraphQLConfiguration.SchemaConfiguration) {
        schemaConfiguration.packages = listOf("no.nav.helse.spesialist.api.graphql")

        schemaConfiguration.queries =
            listOf(
                PersonQuery(handler = queryHandlers.person),
                OppgaverQuery(handler = queryHandlers.oppgaver),
                BehandlingsstatistikkQuery(handler = queryHandlers.behandlingsstatistikk),
                OpptegnelseQuery(handler = queryHandlers.opptegnelse),
            )

        schemaConfiguration.mutations =
            listOf(
                NotatMutation(handler = mutationHandlers.notat),
                VarselMutation(handler = mutationHandlers.varsel),
                TildelingMutation(handler = mutationHandlers.tildeling),
                OpptegnelseMutation(handler = mutationHandlers.opptegnelse),
                OverstyringMutation(handler = mutationHandlers.overstyring),
                SkjonnsfastsettelseMutation(handler = mutationHandlers.skjonnsfastsettelse),
                MinimumSykdomsgradMutation(handler = mutationHandlers.minimumSykdomsgrad),
                TotrinnsvurderingMutation(handler = mutationHandlers.totrinnsvurdering),
                VedtakMutation(handler = mutationHandlers.vedtak),
                PersonMutation(handler = mutationHandlers.person),
                PaVentMutation(handler = mutationHandlers.paVent),
                StansAutomatiskBehandlingMutation(handler = mutationHandlers.stansAutomatiskBehandling),
            )

        schemaConfiguration.hooks = schemaGeneratorHooks
    }
}
