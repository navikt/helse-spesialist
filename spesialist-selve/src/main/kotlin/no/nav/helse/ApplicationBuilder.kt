package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.helse.abonnement.AbonnementDao
import no.nav.helse.abonnement.OpptegnelseMediator
import no.nav.helse.abonnement.opptegnelseApi
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.behandlingsstatistikk.behandlingsstatistikkApi
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.api.*
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.notat.NotatDao
import no.nav.helse.notat.NotatMediator
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.person.PersonApiDao
import no.nav.helse.person.PersonsnapshotDao
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.ProxySelector
import java.net.URL
import java.util.*
import kotlin.random.Random.Default.nextInt
import no.nav.helse.abonnement.OpptegnelseDao as OpptegnelseApiDao

const val azureMountPath: String = "/var/run/secrets/nais.io/azure"
private val auditLog = LoggerFactory.getLogger("auditLogger")
private val logg = LoggerFactory.getLogger("ApplicationBuilder")
private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
private val personIdRegex = "\\d{11,13}".toRegex()

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(System.getenv())
    private val dataSource = dataSourceBuilder.getDataSource()

    private val azureAdClient = HttpClient(Apache) {
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
    }
    private val spleisClient = HttpClient(Apache) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        engine {
            socketTimeout = 30_000
            connectTimeout = 30_000
            connectionRequestTimeout = 40_000
        }
    }
    private val accessTokenClient = AccessTokenClient(
        aadAccessTokenUrl = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        clientSecret = env.getValue("AZURE_APP_CLIENT_SECRET"),
        httpClient = azureAdClient
    )
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisClientId = env.getValue("SPLEIS_CLIENT_ID")
    )

    private val azureConfig = AzureAdAppConfig(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        jwkProvider = JwkProviderBuilder(URL(env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"))).build()
    )
    private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")
    private lateinit var hendelseMediator: HendelseMediator
    private lateinit var notatMediator: NotatMediator

    private val personDao = PersonDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val varselDao = VarselDao(dataSource)
    private val personsnapshotDao = PersonsnapshotDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val warningDao = WarningDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val commandContextDao = CommandContextDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)
    private val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val opptegnelseApiDao = OpptegnelseApiDao(dataSource)
    private val abonnementDao = AbonnementDao(dataSource)
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)

    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao,
        tildelingDao,
        reservasjonDao
    )

    private val plukkTilManuell: PlukkTilManuell = ({
        env["STIKKPROEVER_DIVISOR"]?.let {
            val divisor = it.toInt()
            require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
            nextInt(divisor) == 0
        } ?: false
    })


    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        commandContextDao = commandContextDao,
        snapshotDao = snapshotDao,
        oppgaveDao = oppgaveDao,
        reservasjonDao = reservasjonDao,
        tildelingDao = tildelingDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        arbeidsforholdDao = arbeidsforholdDao,
        speilSnapshotRestClient = speilSnapshotRestClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao),
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = AutomatiseringDao(dataSource),
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            plukkTilManuell = plukkTilManuell,
            vergemålDao = vergemålDao
        ),
        utbetalingDao = utbetalingDao,
        opptegnelseDao = opptegnelseDao,
        vergemålDao = vergemålDao
    )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            install(CORS) {
                header(HttpHeaders.AccessControlAllowOrigin)
                host("speil.nais.adeo.no", listOf("https"))
            }
            install(CallId) {
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
                        auditLog.info("end=${System.currentTimeMillis()} suid=$navIdent duid=$personId request=${uri.substring(0, uri.length.coerceAtMost(70))}")
                    }
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            requestResponseTracing(httpTraceLog)
            azureAdAppAuthentication(azureConfig)
            basicAuthentication(env.getValue("ADMIN_SECRET"))
            installGraphQLApi()
            routing {
                authenticate("oidc") {
                    oppgaveApi(
                        oppgaveMediator = oppgaveMediator,
                        riskSupersaksbehandlergruppe = env.riskGruppeId(),
                        kode7Saksbehandlergruppe = env.kode7GruppeId()
                    )
                    personApi(
                        personMediator = PersonMediator(
                            personsnapshotDao = personsnapshotDao,
                            snapshotDao = snapshotDao,
                            varselDao = varselDao,
                            personDao = personApiDao,
                            arbeidsgiverDao = arbeidsgiverApiDao,
                            overstyringDao = overstyringApiDao,
                            oppgaveDao = oppgaveDao,
                            tildelingDao = tildelingDao,
                            risikovurderingApiDao = risikovurderingApiDao,
                            utbetalingDao = utbetalingDao,
                            speilSnapshotRestClient = speilSnapshotRestClient
                        ),
                        hendelseMediator = hendelseMediator,
                        kode7Saksbehandlergruppe = env.kode7GruppeId()
                    )
                    overstyringApi(hendelseMediator)
                    tildelingApi(TildelingMediator(saksbehandlerDao, tildelingDao, hendelseMediator))
                    annulleringApi(hendelseMediator)
                    opptegnelseApi(OpptegnelseMediator(opptegnelseApiDao, abonnementDao))
                    leggPåVentApi(LeggPåVentMediator(tildelingDao, hendelseMediator))
                    behandlingsstatistikkApi(BehandlingsstatistikkMediator(behandlingsstatistikkDao))
                    notaterApi(notatMediator)
                }
            }
            adminApi(hendelseMediator)
        }.build()

    init {
        rapidsConnection.register(this)
        hendelseMediator = HendelseMediator(
            rapidsConnection = rapidsConnection,
            dataSource = dataSource,
            oppgaveMediator = oppgaveMediator,
            hendelsefabrikk = hendelsefabrikk,
        )
        notatMediator = NotatMediator(notatDao)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }
}

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { cause ->
            val uri = call.request.uri
            val verb = call.request.httpMethod.value
            logg.error("Unhandled: $verb", cause)
            sikkerLog.error("Unhandled: $verb - $uri", cause)
            call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.getGrupper(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}

private fun Map<String, String>.kode7GruppeId() = UUID.fromString(this.getValue("KODE7_SAKSBEHANDLER_GROUP"))
private fun Map<String, String>.riskGruppeId() = UUID.fromString(this.getValue("RISK_SUPERSAKSBEHANDLER_GROUP"))
