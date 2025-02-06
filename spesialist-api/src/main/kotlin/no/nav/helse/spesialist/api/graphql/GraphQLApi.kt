package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLServer
import com.expediagroup.graphql.server.ktor.GraphQL
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.GraphQLCallLogging
import no.nav.helse.spesialist.api.GraphQLMetrikker
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
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
import no.nav.helse.spesialist.api.person.PersonService
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

fun Application.graphQLApi(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    tildelingApiDao: TildelingApiDao,
    arbeidsgiverApiDao: ArbeidsgiverApiDao,
    overstyringApiDao: OverstyringApiDao,
    risikovurderingApiDao: RisikovurderingApiDao,
    varselRepository: VarselApiRepository,
    oppgaveApiDao: OppgaveApiDao,
    periodehistorikkApiDao: PeriodehistorikkApiDao,
    notatDao: NotatApiDao,
    totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    påVentApiDao: PåVentApiDao,
    vergemålApiDao: VergemålApiDao,
    reservasjonClient: ReservasjonClient,
    avviksvurderinghenter: Avviksvurderinghenter,
    skjermedePersonerGruppeId: UUID,
    kode7Saksbehandlergruppe: UUID,
    beslutterGruppeId: UUID,
    snapshotService: SnapshotService,
    behandlingsstatistikkMediator: IBehandlingsstatistikkService,
    saksbehandlerhåndterer: Saksbehandlerhåndterer,
    apiOppgaveService: ApiOppgaveService,
    totrinnsvurderinghåndterer: Totrinnsvurderinghåndterer,
    godkjenninghåndterer: Godkjenninghåndterer,
    personhåndterer: Personhåndterer,
    dokumenthåndterer: Dokumenthåndterer,
    stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    env: Environment,
) {
    val spesialistSchema =
        SpesialistSchema(
            queryHandlers =
                SpesialistSchema.QueryHandlers(
                    person =
                        PersonQueryHandler(
                            personoppslagService =
                                PersonService(
                                    personApiDao = personApiDao,
                                    egenAnsattApiDao = egenAnsattApiDao,
                                    tildelingApiDao = tildelingApiDao,
                                    arbeidsgiverApiDao = arbeidsgiverApiDao,
                                    overstyringApiDao = overstyringApiDao,
                                    risikovurderingApiDao = risikovurderingApiDao,
                                    varselRepository = varselRepository,
                                    oppgaveApiDao = oppgaveApiDao,
                                    periodehistorikkApiDao = periodehistorikkApiDao,
                                    notatDao = notatDao,
                                    totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                                    påVentApiDao = påVentApiDao,
                                    vergemålApiDao = vergemålApiDao,
                                    snapshotService = snapshotService,
                                    reservasjonClient = reservasjonClient,
                                    apiOppgaveService = apiOppgaveService,
                                    saksbehandlerhåndterer = saksbehandlerhåndterer,
                                    avviksvurderinghenter = avviksvurderinghenter,
                                    personhåndterer = personhåndterer,
                                    stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                                    env = env,
                                ),
                        ),
                    oppgaver =
                        OppgaverQueryHandler(
                            apiOppgaveService = apiOppgaveService,
                        ),
                    behandlingsstatistikk =
                        BehandlingsstatistikkQueryHandler(
                            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                        ),
                    opptegnelse =
                        OpptegnelseQueryHandler(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                        ),
                    dokument =
                        DokumentQueryHandler(
                            personApiDao = personApiDao,
                            egenAnsattApiDao = egenAnsattApiDao,
                            dokumenthåndterer = dokumenthåndterer,
                        ),
                ),
            mutationHandlers =
                SpesialistSchema.MutationHandlers(
                    notat = NotatMutationHandler(notatDao = notatDao),
                    varsel = VarselMutationHandler(varselRepository = varselRepository),
                    tildeling = TildelingMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    opptegnelse = OpptegnelseMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    overstyring = OverstyringMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    minimumSykdomsgrad = MinimumSykdomsgradMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    totrinnsvurdering =
                        TotrinnsvurderingMutationHandler(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            apiOppgaveService = apiOppgaveService,
                            totrinnsvurderinghåndterer = totrinnsvurderinghåndterer,
                        ),
                    vedtak =
                        VedtakMutationHandler(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                            godkjenninghåndterer = godkjenninghåndterer,
                        ),
                    person = PersonMutationHandler(personhåndterer = personhåndterer),
                    annullering =
                        AnnulleringMutationHandler(
                            saksbehandlerhåndterer = saksbehandlerhåndterer,
                        ),
                    paVent = PaVentMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                    opphevStans = OpphevStansMutationHandler(saksbehandlerhåndterer = saksbehandlerhåndterer),
                ),
        )
    val graphQL =
        install(GraphQL) {
            server {
                requestParser = RequestParser()
                contextFactory =
                    ContextFactory(
                        kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                        skjermedePersonerSaksbehandlergruppe = skjermedePersonerGruppeId,
                        beslutterSaksbehandlergruppe = beslutterGruppeId,
                    )
            }
            schema(spesialistSchema::setup)
        }

    routing {
        route("graphql") {
            authenticate("oidc") {
                install(GraphQLMetrikker)
                install(GraphQLCallLogging)
                queryHandler(graphQL.server)
            }
        }
    }
}

// TODO Erstatt denne med å bruke graphQLPostRoute() i routingen
//  Per nå feiler det med ClassNotFoundException: io.ktor.server.routing.RoutingKt
fun Route.queryHandler(server: GraphQLServer<ApplicationRequest>) {
    post {
        val start = System.nanoTime()

        val result = server.execute(call.request)

        val tidBrukt = Duration.ofNanos(System.nanoTime() - start)
        sikkerLogg.trace("Kall behandlet etter ${tidBrukt.toMillis()} ms")
        call.respond(result ?: throw RuntimeException("Kall mot GraphQL server feilet"))
    }
}

private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
