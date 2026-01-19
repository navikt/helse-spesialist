package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLServer
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.KtorGraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.GraphQLCallLogging
import no.nav.helse.spesialist.api.GraphQLMetrikker
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.bootstrap.debugMinneApi
import no.nav.helse.spesialist.api.bootstrap.installPlugins
import no.nav.helse.spesialist.api.feilhåndtering.SpesialistDataFetcherExceptionHandler
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.StansAutomatiskBehandlingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.rest.RestAdapter
import no.nav.helse.spesialist.api.rest.restRoutes
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import java.time.Duration

fun kobleOppApi(
    ktorApplication: Application,
    apiModuleConfiguration: ApiModule.Configuration,
    tilgangsgruppeUuider: TilgangsgruppeUuider,
    spesialistSchema: SpesialistSchema,
    sessionFactory: SessionFactory,
    meldingPubliserer: MeldingPubliserer,
    dokumentMediator: DokumentMediator,
    environmentToggles: EnvironmentToggles,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
) {
    ktorApplication.installPlugins(apiModuleConfiguration.eksponerOpenApi)
    ktorApplication.azureAdAppAuthentication(apiModuleConfiguration)
    val graphQLPlugin =
        ktorApplication.install(GraphQL) {
            engine {
                exceptionHandler = SpesialistDataFetcherExceptionHandler()
            }
            server {
                requestParser = KtorGraphQLRequestParser(objectMapper)
                contextFactory = ContextFactory(tilgangsgruppeUuider = tilgangsgruppeUuider)
            }
            schema(spesialistSchema::setup)
        }
    ktorApplication.routing {
        webSocketsApi()
        debugMinneApi()
        route("graphql") {
            authenticate("oidc") {
                install(GraphQLMetrikker)
                install(GraphQLCallLogging)
                queryHandler(graphQLPlugin.server)
            }
        }
        val restAdapter =
            RestAdapter(
                sessionFactory = sessionFactory,
                tilgangsgruppeUuider = tilgangsgruppeUuider,
                meldingPubliserer = meldingPubliserer,
                versjonAvKode = apiModuleConfiguration.versjonAvKode,
            )
        restRoutes(restAdapter, apiModuleConfiguration, dokumentMediator, environmentToggles, krrRegistrertStatusHenter)
    }
}

fun lagSchemaMedResolversOgHandlers(
    daos: Daos,
    apiOppgaveService: ApiOppgaveService,
    saksbehandlerMediator: SaksbehandlerMediator,
    stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    personhåndterer: Personhåndterer,
    snapshothenter: Snapshothenter,
    krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    sessionFactory: SessionFactory,
    behandlingstatistikk: IBehandlingsstatistikkService,
): SpesialistSchema =
    SpesialistSchema(
        queryHandlers =
            SpesialistSchema.QueryHandlers(
                person =
                    PersonQueryHandler(
                        personoppslagService =
                            PersonService(
                                personApiDao = daos.personApiDao,
                                vergemålApiDao = daos.vergemålApiDao,
                                tildelingApiDao = daos.tildelingApiDao,
                                arbeidsgiverApiDao = daos.arbeidsgiverApiDao,
                                overstyringApiDao = daos.overstyringApiDao,
                                risikovurderingApiDao = daos.risikovurderingApiDao,
                                varselRepository = daos.varselApiRepository,
                                oppgaveApiDao = daos.oppgaveApiDao,
                                periodehistorikkApiDao = daos.periodehistorikkApiDao,
                                notatDao = daos.notatApiDao,
                                påVentApiDao = daos.påVentApiDao,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerMediator = saksbehandlerMediator,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                personhåndterer = personhåndterer,
                                snapshotService = SnapshotService(daos.personinfoDao, snapshothenter),
                                krrRegistrertStatusHenter = krrRegistrertStatusHenter,
                                sessionFactory = sessionFactory,
                                vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
                                stansAutomatiskBehandlingSaksbehandlerDao = daos.stansAutomatiskBehandlingSaksbehandlerDao,
                                annulleringRepository = daos.annulleringRepository,
                                saksbehandlerRepository = daos.saksbehandlerRepository,
                            ),
                    ),
                oppgaver =
                    OppgaverQueryHandler(
                        apiOppgaveService = apiOppgaveService,
                    ),
                behandlingsstatistikk =
                    BehandlingsstatistikkQueryHandler(
                        behandlingsstatistikkMediator = behandlingstatistikk,
                    ),
            ),
        mutationHandlers =
            SpesialistSchema.MutationHandlers(
                notat = NotatMutationHandler(sessionFactory = sessionFactory),
                tildeling = TildelingMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                overstyring =
                    OverstyringMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                totrinnsvurdering =
                    TotrinnsvurderingMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                person = PersonMutationHandler(personhåndterer = personhåndterer),
                paVent = PaVentMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                stansAutomatiskBehandling = StansAutomatiskBehandlingMutationHandler(sessionFactory = sessionFactory),
            ),
    )

// TODO Erstatt denne med å bruke graphQLPostRoute() i routingen
//  Per nå feiler det med ClassNotFoundException: io.ktor.server.routing.RoutingKt
fun Route.queryHandler(server: GraphQLServer<ApplicationRequest>) {
    post {
        val start = System.nanoTime()

        val result = checkNotNull(server.execute(call.request)) { "Kall mot GraphQL server feilet" }

        loggFeil(result)

        val tidBrukt = Duration.ofNanos(System.nanoTime() - start)
        sikkerlogg.trace("Kall behandlet etter ${tidBrukt.toMillis()} ms")
        call.respond(result)
    }
}

private fun loggFeil(result: GraphQLServerResponse) {
    if (result is GraphQLResponse<*>) {
        result.errors.takeUnless { it.isNullOrEmpty() }?.let {
            sikkerlogg.warn("GraphQL-respons inneholder feil: ${it.joinToString()}")
        }
    }
}
