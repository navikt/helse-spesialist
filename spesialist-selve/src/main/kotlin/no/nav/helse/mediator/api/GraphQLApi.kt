package no.nav.helse.mediator.api

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import graphql.GraphQL
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.api.graphql.ContextFactory
import no.nav.helse.mediator.api.graphql.RequestParser
import no.nav.helse.mediator.api.graphql.SchemaBuilder
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.objectMapper
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao
import java.util.*

internal fun Application.graphQLApi(
    snapshotDao: SnapshotDao,
    personApiDao: PersonApiDao,
    tildelingDao: TildelingDao,
    arbeidsgiverApiDao: ArbeidsgiverApiDao,
    overstyringApiDao: OverstyringApiDao,
    risikovurderingApiDao: RisikovurderingApiDao,
    varselDao: VarselDao,
    kode7Saksbehandlergruppe: UUID,
    snapshotGraphQLClient: SpeilSnapshotGraphQLClient
) {
    val schema = SchemaBuilder(
        snapshotDao = snapshotDao,
        personApiDao = personApiDao,
        tildelingDao = tildelingDao,
        arbeidsgiverApiDao = arbeidsgiverApiDao,
        overstyringApiDao = overstyringApiDao,
        risikovurderingApiDao = risikovurderingApiDao,
        varselDao = varselDao,
        snapshotGraphQLClient = snapshotGraphQLClient
    ).build()

    val server = GraphQLServer(
        requestParser = RequestParser(),
        contextFactory = ContextFactory(kode7Saksbehandlergruppe),
        requestHandler = GraphQLRequestHandler(
            GraphQL.newGraphQL(schema).build()
        )
    )

    routing {
        post("graphql") {
            val result = server.execute(call.request)

            if (result != null) {
                val json = objectMapper.writeValueAsString(result)
                call.respond(json)
            }
        }

        get("playground") {
            call.respondText(buildPlaygroundHtml("graphql", "subscriptions"), ContentType.Text.Html)
        }
    }
}

private fun buildPlaygroundHtml(graphQLEndpoint: String, subscriptionsEndpoint: String) =
    Application::class.java.classLoader.getResource("graphql-playground.html")?.readText()
        ?.replace("\${graphQLEndpoint}", graphQLEndpoint)?.replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
        ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")
