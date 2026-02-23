package no.nav.helse.spesialist.api

import com.auth0.jwk.JwkProviderBuilder
import com.expediagroup.graphql.server.ktor.GraphQL
import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.auth.configureJwtAuthentication
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.configureGraphQLPlugin
import no.nav.helse.spesialist.api.graphql.graphQLRoute
import no.nav.helse.spesialist.api.plugins.configureCallIdPlugin
import no.nav.helse.spesialist.api.plugins.configureCallLoggingPlugin
import no.nav.helse.spesialist.api.plugins.configureContentNegotiationPlugin
import no.nav.helse.spesialist.api.plugins.configureOpenApiPlugin
import no.nav.helse.spesialist.api.plugins.configureResourcesPlugin
import no.nav.helse.spesialist.api.plugins.configureStatusPagesPlugin
import no.nav.helse.spesialist.api.rest.DokumentMediator
import no.nav.helse.spesialist.api.rest.RestAdapter
import no.nav.helse.spesialist.api.rest.restRoutes
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import java.net.URI

fun configureKtorApplication(
    ktorApplication: Application,
    apiModuleConfiguration: ApiModule.Configuration,
    spesialistSchema: SpesialistSchema,
    sessionFactory: SessionFactory,
    meldingPubliserer: MeldingPubliserer,
    dokumentMediator: DokumentMediator,
    forsikringHenter: ForsikringHenter,
    inngangsvilkårHenter: InngangsvilkårHenter,
    inngangsvilkårInnsender: InngangsvilkårInnsender,
    environmentToggles: EnvironmentToggles,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
) {
    with(ktorApplication) {
        install(CallId) { configureCallIdPlugin() }
        install(StatusPages) { configureStatusPagesPlugin() }
        install(CallLogging) { configureCallLoggingPlugin() }
        install(DoubleReceive)
        install(ContentNegotiation) { configureContentNegotiationPlugin() }
        if (apiModuleConfiguration.eksponerOpenApi) {
            install(OpenApi) { configureOpenApiPlugin() }
        }
        install(SSE)
        install(Resources) { configureResourcesPlugin() }
        val graphQLPlugin = install(GraphQL.Plugin) { configureGraphQLPlugin(spesialistSchema) }

        authentication {
            jwt("oidc") {
                configureJwtAuthentication(
                    jwkProvider = JwkProviderBuilder(URI(apiModuleConfiguration.jwkProviderUri).toURL()).build(),
                    issuerUrl = apiModuleConfiguration.issuerUrl,
                    clientId = apiModuleConfiguration.clientId,
                    tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
                    tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
                )
            }
        }

        routing {
            graphQLRoute(graphQLPlugin)
            restRoutes(
                restAdapter =
                    RestAdapter(
                        sessionFactory = sessionFactory,
                        meldingPubliserer = meldingPubliserer,
                        versjonAvKode = apiModuleConfiguration.versjonAvKode,
                    ),
                sessionFactory = sessionFactory,
                configuration = apiModuleConfiguration,
                dokumentMediator = dokumentMediator,
                environmentToggles = environmentToggles,
                krrRegistrertStatusHenter = krrRegistrertStatusHenter,
                forsikringHenter = forsikringHenter,
                inngangsvilkårHenter = inngangsvilkårHenter,
                inngangsvilkårInnsender = inngangsvilkårInnsender,
            )
        }
    }
}
