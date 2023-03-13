package no.nav.helse

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.net.ProxySelector
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.api.erDev
import no.nav.helse.mediator.api.leggPåVentApi
import no.nav.helse.mediator.api.notaterApi
import no.nav.helse.mediator.api.overstyringApi
import no.nav.helse.mediator.api.personApi
import no.nav.helse.mediator.api.tildelingApi
import no.nav.helse.mediator.api.totrinnsvurderingApi
import no.nav.helse.modell.TotrinnsvurderingDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.leggpåvent.LeggPåVentService
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.TildelingService
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.AzureAdAppConfig
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseMediator
import no.nav.helse.spesialist.api.abonnement.opptegnelseApi
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.behandlingsstatistikk.behandlingsstatistikkApi
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.experimental.OppgavePagineringDao
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao
import no.nav.helse.spesialist.api.utbetaling.annulleringApi
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.random.Random.Default.nextInt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao as OpptegnelseApiDao

private val auditLog = LoggerFactory.getLogger("auditLogger")
private val logg = LoggerFactory.getLogger("ApplicationBuilder")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
private val personIdRegex = "\\d{11,13}".toRegex()

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()

    private val azureAdClient = HttpClient(Apache) {
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json, JacksonConverter(
                    jacksonObjectMapper()
                        .registerModule(JavaTimeModule())
                )
            )
        }
    }
    private val httpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter())
        }
        engine {
            socketTimeout = 120_000
            connectTimeout = 1_000
            connectionRequestTimeout = 40_000
        }
    }
    private val accessTokenClient = AccessTokenClient(
        aadAccessTokenUrl = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        clientSecret = env.getValue("AZURE_APP_CLIENT_SECRET"),
        httpClient = azureAdClient
    )
    private val snapshotClient = SnapshotClient(
        httpClient = httpClient,
        accessTokenClient = accessTokenClient,
        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
        spleisClientId = env.getValue("SPLEIS_CLIENT_ID")
    )
    private val reservasjonClient = ReservasjonClient(
        httpClient = httpClient,
        accessTokenClient = accessTokenClient,
        apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
        scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
    )
    private val azureConfig = AzureConfig(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    )
    private val azureAdAppConfig = AzureAdAppConfig(
        azureConfig = azureConfig,
    )
    private val graphAccessTokenClient = GraphAccessTokenClient(
        httpClient = httpClient,
        azureConfig = azureConfig,
        privateJwk = env.getValue("AZURE_APP_JWK")
    )

    private val msGraphClient = MsGraphClient(httpClient = httpClient, tokenClient = graphAccessTokenClient).also {
        if (erDev()) {
            runBlocking { it.hentGrupper() }
        }
    }
    private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")
    private lateinit var hendelseMediator: HendelseMediator
    private lateinit var saksbehandlerMediator: SaksbehandlerMediator
    private lateinit var tildelingService: TildelingService

    private val personDao = PersonDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val varselDao = VarselDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val oppgavePagineringDao = OppgavePagineringDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val warningDao = WarningDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)
    private val utbetalingApiDao = UtbetalingApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val opptegnelseApiDao = OpptegnelseApiDao(dataSource)
    private val abonnementDao = no.nav.helse.spesialist.api.abonnement.AbonnementDao(dataSource)
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)
    private val notatMediator = NotatMediator(notatDao)
    private val overstyringDao = OverstyringDao(dataSource)
    private val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    private val varselRepository = ActualVarselRepository(dataSource)
    private val apiVarselRepository = ApiVarselRepository(dataSource)
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)

    private val behandlingsstatistikkMediator = BehandlingsstatistikkMediator(behandlingsstatistikkDao)

    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        reservasjonDao = reservasjonDao,
        opptegnelseDao = opptegnelseDao
    )

    private val snapshotMediator = SnapshotMediator(
        snapshotDao = snapshotApiDao,
        snapshotClient = snapshotClient,
    )

    private val plukkTilManuell: PlukkTilManuell = ({
        env["STIKKPROEVER_DIVISOR"]?.let {
            val divisor = it.toInt()
            require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
            nextInt(divisor) == 0
        } ?: false
    })

    private val tilgangsgrupper = Tilgangsgrupper(System.getenv())

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            install(CORS) {
                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                allowHost("spesialist.dev.intern.nav.no", listOf("https"))
            }
            install(CallId) {
                retrieveFromHeader(HttpHeaders.XRequestId)
                generate {
                    UUID.randomUUID().toString()
                }
            }
            installErrorHandling()
            install(CallLogging) {
                disableDefaultColors()
                logger = httpTraceLog
                level = Level.INFO
                callIdMdc("callId")
                filter { call -> call.request.path().startsWith("/api/") }
            }
            intercept(ApplicationCallPipeline.Call) {
                call.principal<JWTPrincipal>()?.let { principal ->
                    val uri = call.request.uri
                    val navIdent = principal.payload.getClaim("NAVident").asString()
                    (personIdRegex.find(uri)?.value ?: call.request.header("fodselsnummer"))?.also { personId ->
                        auditLog.info(
                            "end=${System.currentTimeMillis()} suid=$navIdent duid=$personId request=${
                                uri.substring(
                                    0,
                                    uri.length.coerceAtMost(70)
                                )
                            }"
                        )
                    }
                }
            }
            install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            requestResponseTracing(httpTraceLog)
            azureAdAppAuthentication(azureAdAppConfig)
            graphQLApi(
                personApiDao = personApiDao,
                egenAnsattApiDao = egenAnsattApiDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselDao = varselDao,
                varselRepository = apiVarselRepository,
                utbetalingApiDao = utbetalingApiDao,
                oppgaveApiDao = oppgaveApiDao,
                periodehistorikkDao = periodehistorikkDao,
                notatDao = notatDao,
                reservasjonClient = reservasjonClient,
                skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
                kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
                beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
                riskGruppeId = tilgangsgrupper.riskQaGruppeId,
                snapshotMediator = snapshotMediator,
                oppgaveService = OppgaveService(oppgavePagineringDao),
                behandlingsstatistikkMediator = behandlingsstatistikkMediator,
            )
            routing {
                authenticate("oidc") {
                    personApi(
                        varselRepository = apiVarselRepository,
                        totrinnsvurderingDao = totrinnsvurderingDao,
                        hendelseMediator = hendelseMediator,
                        oppgaveMediator = oppgaveMediator,
                        tilgangsgrupper = tilgangsgrupper,
                    )
                    overstyringApi(hendelseMediator)
                    tildelingApi(tildelingService)
                    annulleringApi(saksbehandlerMediator)
                    opptegnelseApi(OpptegnelseMediator(opptegnelseApiDao, abonnementDao))
                    leggPåVentApi(LeggPåVentService(tildelingDao, hendelseMediator), notatMediator)
                    behandlingsstatistikkApi(BehandlingsstatistikkMediator(behandlingsstatistikkDao))
                    notaterApi(notatMediator)
                    totrinnsvurderingApi(
                        apiVarselRepository,
                        oppgaveMediator,
                        periodehistorikkDao,
                        notatMediator,
                        tildelingService,
                        hendelseMediator,
                        totrinnsvurderingDao
                    )
                }
            }
        }.build()

    private val automatiseringDao = AutomatiseringDao(dataSource)
    val automatisering = Automatisering(
        warningDao = warningDao,
        risikovurderingDao = risikovurderingDao,
        automatiseringDao = automatiseringDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        vergemålDao = vergemålDao,
        personDao = personDao,
        vedtakDao = vedtakDao,
        overstyringDao = overstyringDao,
        generasjonRepository = generasjonRepository,
        snapshotMediator = snapshotMediator,
        plukkTilManuell = plukkTilManuell
    )

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao, opptegnelseDao, varselRepository, generasjonRepository),
        automatisering = automatisering,
        overstyringMediator = OverstyringMediator(rapidsConnection),
        snapshotMediator = snapshotMediator,
    )

    init {
        rapidsConnection.register(this)
        hendelseMediator = HendelseMediator(
            dataSource = dataSource,
            rapidsConnection = rapidsConnection,
            opptegnelseDao = opptegnelseDao,
            oppgaveMediator = oppgaveMediator,
            hendelsefabrikk = hendelsefabrikk
        )
        saksbehandlerMediator = SaksbehandlerMediator(dataSource, rapidsConnection)
        tildelingService = TildelingService(
            saksbehandlerDao,
            tildelingDao,
            hendelseMediator,
            riskSaksbehandlergruppe = tilgangsgrupper.riskQaGruppeId,
            kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
            beslutterSaksbehandlergruppe = tilgangsgrupper.beslutterGruppeId,
            skjermedePersonerSaksbehandlergruppe = tilgangsgrupper.skjermedePersonerGruppeId,
        )
    }

    fun start() = rapidsConnection.start().also {
        val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
        logg.info("Registrerte garbage collectors etter oppstart: ${beans.joinToString { it.name }}")
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }
}

fun Application.installErrorHandling() {

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val uri = call.request.uri
            val verb = call.request.httpMethod.value
            logg.error("Unhandled: $verb", cause)
            sikkerlogg.error("Unhandled: $verb - $uri", cause)
            call.respondText(
                text = "Det skjedde en uventet feil",
                status = HttpStatusCode.InternalServerError
            )
            call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")

        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.gruppemedlemskap(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}

class Tilgangskontroll(private val tilgangsgrupper: Tilgangsgrupper, private val gruppemedlemskap: List<UUID>) {
    val harTilgangTilRisksaker by lazy { gruppemedlemskap.contains(tilgangsgrupper.riskQaGruppeId) }
    val harTilgangTilBeslutterOppgaver by lazy { gruppemedlemskap.contains(tilgangsgrupper.beslutterGruppeId) }
}

class Tilgangsgrupper(private val env: Map<String, String>) {
    private fun fromEnv(key: String) = UUID.fromString(env.getValue(key))

    val kode7GruppeId: UUID by lazy { fromEnv(kode7Key) }
    val riskQaGruppeId: UUID by lazy { fromEnv(riskQaKey) }
    val beslutterGruppeId: UUID by lazy { fromEnv(beslutterKey) }
    val skjermedePersonerGruppeId: UUID by lazy { fromEnv(skjermedeKey) }

    companion object {
        const val kode7Key = "KODE7_SAKSBEHANDLER_GROUP"
        const val riskQaKey = "RISK_SUPERSAKSBEHANDLER_GROUP"
        const val beslutterKey = "BESLUTTER_SAKSBEHANDLER_GROUP"
        const val skjermedeKey = "SKJERMEDE_PERSONER_GROUP"
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.tilganger(tilgangsgrupper: Tilgangsgrupper) =
    Tilgangskontroll(tilgangsgrupper, gruppemedlemskap())
