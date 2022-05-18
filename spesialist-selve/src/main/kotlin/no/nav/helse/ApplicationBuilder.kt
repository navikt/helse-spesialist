package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
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
import io.ktor.server.plugins.cors.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import java.net.ProxySelector
import java.net.URI
import java.net.URL
import java.util.UUID
import no.nav.helse.abonnement.AbonnementDao
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.abonnement.OpptegnelseMediator
import no.nav.helse.abonnement.opptegnelseApi
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.behandlingsstatistikk.behandlingsstatistikkApi
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.api.annulleringApi
import no.nav.helse.mediator.api.graphQLApi
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.mediator.api.graphql.SnapshotMediator
import no.nav.helse.mediator.api.leggPåVentApi
import no.nav.helse.mediator.api.notaterApi
import no.nav.helse.mediator.api.oppgaveApi
import no.nav.helse.mediator.api.overstyringApi
import no.nav.helse.mediator.api.personApi
import no.nav.helse.mediator.api.tildelingApi
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.notat.NotatDao
import no.nav.helse.notat.NotatMediator
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.person.PersonApiDao
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
import kotlin.random.Random.Default.nextInt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer
import no.nav.helse.abonnement.OpptegnelseDao as OpptegnelseApiDao

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
    private val spleisClient = HttpClient(Apache) {
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
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
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
    private val oppgaveDao = OppgaveDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
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
    private val snapshotDao = SnapshotDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)

    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao,
        tildelingDao,
        reservasjonDao,
        opptegnelseDao
    )

    private val snapshotMediator = SnapshotMediator(snapshotDao, snapshotClient)

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
        snapshotDao = snapshotDao,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao, opptegnelseDao),
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
            vergemålDao = vergemålDao,
            snapshotDao = snapshotDao,
        ),
        utbetalingDao = utbetalingDao,
        opptegnelseDao = opptegnelseDao,
        vergemålDao = vergemålDao
    )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            install(CORS) {
                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                allowHost("speil.nais.adeo.no", listOf("https"))
                allowHost("spesialist.dev.intern.nav.no", listOf("https"))
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
            azureAdAppAuthentication(azureConfig)
            graphQLApi(
                personApiDao = personApiDao,
                egenAnsattDao = egenAnsattDao,
                tildelingDao = tildelingDao,
                arbeidsgiverApiDao = arbeidsgiverApiDao,
                overstyringApiDao = overstyringApiDao,
                risikovurderingApiDao = risikovurderingApiDao,
                varselDao = varselDao,
                utbetalingDao = utbetalingDao,
                oppgaveDao = oppgaveDao,
                periodehistorikkDao = periodehistorikkDao,
                skjermedePersonerGruppeId = env.skjermedePersonerGruppeId(),
                kode7Saksbehandlergruppe = env.kode7GruppeId(),
                snapshotMediator = snapshotMediator,
            )
            routing {
                authenticate("oidc") {
                    oppgaveApi(
                        oppgaveMediator = oppgaveMediator,
                        riskSupersaksbehandlergruppe = env.riskGruppeId(),
                        kode7Saksbehandlergruppe = env.kode7GruppeId(),
                        beslutterSaksbehandlergruppe = env.beslutterGruppeId(),
                    )
                    personApi(
                        hendelseMediator = hendelseMediator,
                        oppgaveMediator = oppgaveMediator
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
        }.build()

    init {
        rapidsConnection.register(this)
        hendelseMediator = HendelseMediator(
            rapidsConnection = rapidsConnection,
            dataSource = dataSource,
            oppgaveMediator = oppgaveMediator,
            hendelsefabrikk = hendelsefabrikk,
            opptegnelseDao = opptegnelseDao
        )
        notatMediator = NotatMediator(notatDao)
    }

    fun start() = rapidsConnection.start()

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

internal fun PipelineContext<Unit, ApplicationCall>.getGrupper(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}

internal fun PipelineContext<Unit, ApplicationCall>.getNAVident(): String {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("NAVident").asString()
}

private fun Map<String, String>.kode7GruppeId() = UUID.fromString(this.getValue("KODE7_SAKSBEHANDLER_GROUP"))
private fun Map<String, String>.riskGruppeId() = UUID.fromString(this.getValue("RISK_SUPERSAKSBEHANDLER_GROUP"))
private fun Map<String, String>.beslutterGruppeId() = UUID.fromString(this.getValue("BESLUTTER_SAKSBEHANDLER_GROUP"))
private fun Map<String, String>.skjermedePersonerGruppeId() = UUID.fromString(this.getValue("SKJERMEDE_PERSONER_GROUP"))
