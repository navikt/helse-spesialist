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
import no.nav.helse.FeatureToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.GraphQLCallLogging
import no.nav.helse.spesialist.api.GraphQLMetrikker
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.graphql.mutation.AnnulleringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.MinimumSykdomsgradMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.NotatMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpphevStansMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OpptegnelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.OverstyringMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PaVentMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.PersonMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.SkjonnsfastsettelseMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TildelingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.TotrinnsvurderingMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.VarselMutationHandler
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler
import no.nav.helse.spesialist.api.graphql.query.BehandlingsstatistikkQueryHandler
import no.nav.helse.spesialist.api.graphql.query.DokumentQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OppgaverQueryHandler
import no.nav.helse.spesialist.api.graphql.query.OpptegnelseQueryHandler
import no.nav.helse.spesialist.api.graphql.query.PersonQueryHandler
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

fun Application.settOppGraphQLApi(
    daos: Daos,
    sessionFactory: SessionFactory,
    saksbehandlerMediator: SaksbehandlerMediator,
    apiOppgaveService: ApiOppgaveService,
    godkjenninghåndterer: Godkjenninghåndterer,
    personhåndterer: Personhåndterer,
    dokumenthåndterer: Dokumenthåndterer,
    stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    behandlingstatistikk: IBehandlingsstatistikkService,
    snapshothenter: Snapshothenter,
    reservasjonshenter: Reservasjonshenter,
    tilgangsgrupper: Tilgangsgrupper,
    featureToggles: FeatureToggles,
) {
    val spesialistSchema =
        lagSchemaMedResolversOgHandlers(
            daos = daos,
            apiOppgaveService = apiOppgaveService,
            saksbehandlerMediator = saksbehandlerMediator,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            personhåndterer = personhåndterer,
            snapshothenter = snapshothenter,
            reservasjonshenter = reservasjonshenter,
            sessionFactory = sessionFactory,
            behandlingstatistikk = behandlingstatistikk,
            dokumenthåndterer = dokumenthåndterer,
            godkjenninghåndterer = godkjenninghåndterer,
            featureToggles = featureToggles,
        )
    val graphQLPlugin =
        install(GraphQL) {
            server {
                requestParser = KtorGraphQLRequestParser(objectMapper)
                contextFactory =
                    ContextFactory(
                        kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
                        skjermedePersonerSaksbehandlergruppe = tilgangsgrupper.skjermedePersonerGruppeId,
                        beslutterSaksbehandlergruppe = tilgangsgrupper.beslutterGruppeId,
                    )
            }
            schema(spesialistSchema::setup)
        }
    routing {
        route("graphql") {
            authenticate("oidc") {
                install(GraphQLMetrikker)
                install(GraphQLCallLogging)
                queryHandler(graphQLPlugin.server)
            }
        }
    }
}

private fun lagSchemaMedResolversOgHandlers(
    daos: Daos,
    apiOppgaveService: ApiOppgaveService,
    saksbehandlerMediator: SaksbehandlerMediator,
    stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    personhåndterer: Personhåndterer,
    snapshothenter: Snapshothenter,
    reservasjonshenter: Reservasjonshenter,
    sessionFactory: SessionFactory,
    behandlingstatistikk: IBehandlingsstatistikkService,
    dokumenthåndterer: Dokumenthåndterer,
    godkjenninghåndterer: Godkjenninghåndterer,
    featureToggles: FeatureToggles,
): SpesialistSchema =
    SpesialistSchema(
        queryHandlers =
            SpesialistSchema.QueryHandlers(
                person =
                    PersonQueryHandler(
                        personoppslagService =
                            PersonService(
                                personApiDao = daos.personApiDao,
                                egenAnsattApiDao = daos.egenAnsattApiDao,
                                vergemålApiDao = daos.vergemålApiDao,
                                tildelingApiDao = daos.tildelingApiDao,
                                arbeidsgiverApiDao = daos.arbeidsgiverApiDao,
                                overstyringApiDao = daos.overstyringApiDao,
                                risikovurderingApiDao = daos.risikovurderingApiDao,
                                varselRepository = daos.varselApiRepository,
                                oppgaveApiDao = daos.oppgaveApiDao,
                                periodehistorikkApiDao = daos.periodehistorikkApiDao,
                                notatDao = daos.notatApiDao,
                                totrinnsvurderingApiDao = daos.totrinnsvurderingApiDao,
                                påVentApiDao = daos.påVentApiDao,
                                apiOppgaveService = apiOppgaveService,
                                saksbehandlerMediator = saksbehandlerMediator,
                                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                personhåndterer = personhåndterer,
                                snapshotService = SnapshotService(daos.personinfoDao, snapshothenter),
                                reservasjonshenter = reservasjonshenter,
                                sessionFactory = sessionFactory,
                                vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
                                featureToggles = featureToggles,
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
                opptegnelse =
                    OpptegnelseQueryHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                dokument =
                    DokumentQueryHandler(
                        personApiDao = daos.personApiDao,
                        egenAnsattApiDao = daos.egenAnsattApiDao,
                        dokumenthåndterer = dokumenthåndterer,
                    ),
            ),
        mutationHandlers =
            SpesialistSchema.MutationHandlers(
                notat = NotatMutationHandler(sessionFactory = sessionFactory),
                varsel = VarselMutationHandler(varselRepository = daos.varselApiRepository),
                tildeling = TildelingMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                opptegnelse = OpptegnelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                overstyring = OverstyringMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                minimumSykdomsgrad = MinimumSykdomsgradMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                totrinnsvurdering =
                    TotrinnsvurderingMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                vedtak =
                    VedtakMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                        godkjenninghåndterer = godkjenninghåndterer,
                    ),
                person = PersonMutationHandler(personhåndterer = personhåndterer),
                annullering =
                    AnnulleringMutationHandler(
                        saksbehandlerMediator = saksbehandlerMediator,
                    ),
                paVent = PaVentMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
                opphevStans = OpphevStansMutationHandler(saksbehandlerMediator = saksbehandlerMediator),
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
        sikkerLogg.trace("Kall behandlet etter ${tidBrukt.toMillis()} ms")
        call.respond(result)
    }
}

private fun loggFeil(result: GraphQLServerResponse) {
    if (result is GraphQLResponse<*>) {
        result.errors.takeUnless { it.isNullOrEmpty() }?.let {
            sikkerLogg.warn("GraphQL-respons inneholder feil: ${it.joinToString()}")
        }
    }
}

private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
