package no.nav.helse

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.UnntaFraAutomatiseringDao
import no.nav.helse.mediator.BehandlingsstatistikkMediator
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.mediator.dokument.DokumentMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.AzureAdAppConfig
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.opptegnelseApi
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory
import java.net.ProxySelector
import java.net.URI
import java.util.UUID
import kotlin.random.Random.Default.nextInt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer
import no.nav.helse.spesialist.api.tildeling.TildelingDao as TildelingApiDao

private val logg = LoggerFactory.getLogger("ApplicationBuilder")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(env)
    private val dataSource = dataSourceBuilder.getDataSource()

    private val azureAdClient =
        HttpClient(Apache) {
            install(HttpRequestRetry) {
                retryOnExceptionIf(3) { request, throwable ->
                    logg.warn("Caught exception ${throwable.message}, for url ${request.url}")
                    true
                }
                retryIf(maxRetries) { request, response ->
                    if (response.status.value.let { it in 500..599 }) {
                        logg.warn(
                            "Retrying for statuscode ${response.status.value}, for url ${request.url}",
                        )
                        true
                    } else {
                        false
                    }
                }
            }
            engine {
                customizeClient {
                    setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(
                        jacksonObjectMapper()
                            .registerModule(JavaTimeModule()),
                    ),
                )
            }
        }
    private val httpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 120_000
                connectTimeout = 1_000
                connectionRequestTimeout = 40_000
            }
        }
    private val reservasjonHttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 1_000
                connectTimeout = 1_000
                connectionRequestTimeout = 2_000
            }
        }
    private val azureConfig =
        AzureConfig(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
            jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
            tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        )
    private val azureAdAppConfig =
        AzureAdAppConfig(
            azureConfig = azureConfig,
        )
    private val accessTokenClient =
        AccessTokenClient(
            httpClient = azureAdClient,
            azureConfig = azureConfig,
            privateJwk = env.getValue("AZURE_APP_JWK"),
        )
    private val snapshotClient =
        SnapshotClient(
            httpClient = httpClient,
            accessTokenClient = accessTokenClient,
            spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
            spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
        )
    private val reservasjonClient =
        ReservasjonClient(
            httpClient = reservasjonHttpClient,
            accessTokenClient = accessTokenClient,
            apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
            scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
        )

    private val msGraphClient = MsGraphClient(httpClient = httpClient, tokenClient = accessTokenClient)

    private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")

    private val personDao = PersonDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val unntaFraAutomatiseringDao = UnntaFraAutomatiseringDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingApiDao = TildelingApiDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    private val notatDao = NotatDao(dataSource)
    private val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    private val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val vergemålDao = VergemålDao(dataSource)
    private val notatMediator = NotatMediator(notatDao)
    private val overstyringDao = OverstyringDao(dataSource)
    private val apiVarselRepository = ApiVarselRepository(dataSource)
    private val meldingDao = MeldingDao(dataSource)
    private val dokumentDao = DokumentDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private val påVentApiDao = PåVentApiDao(dataSource)
    private val avviksvurderinghenter =
        object : Avviksvurderinghenter {
            override fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering? {
                return AvviksvurderingDao(dataSource).finnAvviksvurdering(vilkårsgrunnlagId)
            }
        }
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)

    private val behandlingsstatistikkMediator = BehandlingsstatistikkMediator(behandlingsstatistikkDao)

    private val tilgangsgrupper = Tilgangsgrupper(System.getenv())
    private val tilgangskontrollørForReservasjon = TilgangskontrollørForReservasjon(msGraphClient, tilgangsgrupper)

    private val oppgaveMediator: OppgaveMediator =
        OppgaveMediator(
            meldingDao = meldingDao,
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            reservasjonDao = reservasjonDao,
            opptegnelseDao = opptegnelseDao,
            totrinnsvurderingRepository = totrinnsvurderingDao,
            saksbehandlerRepository = saksbehandlerDao,
            rapidsConnection = rapidsConnection,
            tilgangskontroll = tilgangskontrollørForReservasjon,
            tilgangsgrupper = tilgangsgrupper,
        )

    private val dokumentMediator: DokumentMediator = DokumentMediator(dokumentDao, rapidsConnection)

    private val saksbehandlerMediator: SaksbehandlerMediator =
        SaksbehandlerMediator(dataSource, versjonAvKode(env), rapidsConnection, oppgaveMediator, tilgangsgrupper)

    private val godkjenningMediator =
        GodkjenningMediator(
            vedtakDao,
            opptegnelseDao,
            oppgaveDao,
            UtbetalingDao(dataSource),
            meldingDao,
            generasjonDao,
        )

    private val totrinnsvurderingMediator =
        TotrinnsvurderingMediator(TotrinnsvurderingDao(dataSource), oppgaveDao, periodehistorikkDao, notatMediator)

    private val snapshotMediator =
        SnapshotMediator(
            snapshotDao = snapshotApiDao,
            snapshotClient = snapshotClient,
        )

    private val plukkTilManuell: PlukkTilManuell<String> = (
        {
            it?.let {
                val divisor = it.toInt()
                require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
                nextInt(divisor) == 0
            } ?: false
        }
    )

    private val stikkprøver =
        object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FGB_DIVISOR"])

            override fun utsFlereArbeidsgivereForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_FLERE_AG_FORLENGELSE_DIVISOR"])

            override fun utsEnArbeidsgiverFørstegangsbehandling() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FGB_DIVISOR"])

            override fun utsEnArbeidsgiverForlengelse() = plukkTilManuell(env["STIKKPROEVER_UTS_EN_AG_FORLENGELSE_DIVISOR"])

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() =
                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FGB_DIVISOR"])

            override fun fullRefusjonFlereArbeidsgivereForlengelse() =
                plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_FLERE_AG_FORLENGELSE_DIVISOR"])

            override fun fullRefusjonEnArbeidsgiver() = plukkTilManuell(env["STIKKPROEVER_FULL_REFUSJON_EN_AG_DIVISOR"])
        }

    private lateinit var rapidsConnection: RapidsConnection

    private val godkjenningService: GodkjenningService =
        GodkjenningService(
            dataSource = dataSource,
            rapidsConnection = rapidsConnection,
            oppgaveMediator = oppgaveMediator,
            saksbehandlerRepository = saksbehandlerDao,
        )

    private val automatiseringDao = AutomatiseringDao(dataSource)
    val automatisering =
        Automatisering(
            risikovurderingDao = risikovurderingDao,
            unntaFraAutomatiseringDao = unntaFraAutomatiseringDao,
            automatiseringDao = automatiseringDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            vergemålDao = vergemålDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            overstyringDao = overstyringDao,
            stikkprøver = stikkprøver,
            meldingDao = meldingDao,
            generasjonDao = generasjonDao,
        )

    private val kommandofabrikk =
        Kommandofabrikk(
            dataSource = dataSource,
            snapshotClient = snapshotClient,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator,
            automatisering = automatisering,
        )

    private val meldingMediator: MeldingMediator =
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = rapidsConnection,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = avviksvurderingDao,
            generasjonDao = generasjonDao,
        )

    init {
        rapidsConnection =
            RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
                install(CORS) {
                    allowHeader(HttpHeaders.AccessControlAllowOrigin)
                    allowHost("spesialist.intern.dev.nav.no", listOf("https"))
                }
                install(CallId) {
                    retrieveFromHeader(HttpHeaders.XRequestId)
                    generate {
                        UUID.randomUUID().toString()
                    }
                }
                install(WebSockets)
                installErrorHandling()
                install(CallLogging) {
                    disableDefaultColors()
                    logger = httpTraceLog
                    level = Level.INFO
                    callIdMdc("callId")
                    filter { call -> call.request.path().startsWith("/api/") }
                }
                install(DoubleReceive)
                install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
                requestResponseTracing(httpTraceLog)
                azureAdAppAuthentication(azureAdAppConfig)
                graphQLApi(
                    personApiDao = personApiDao,
                    egenAnsattApiDao = egenAnsattApiDao,
                    tildelingDao = tildelingApiDao,
                    arbeidsgiverApiDao = arbeidsgiverApiDao,
                    overstyringApiDao = overstyringApiDao,
                    risikovurderingApiDao = risikovurderingApiDao,
                    varselRepository = apiVarselRepository,
                    oppgaveApiDao = oppgaveApiDao,
                    periodehistorikkDao = periodehistorikkDao,
                    notatDao = notatDao,
                    totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                    påVentApiDao = påVentApiDao,
                    reservasjonClient = reservasjonClient,
                    avviksvurderinghenter = avviksvurderinghenter,
                    skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
                    kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
                    beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
                    snapshotMediator = snapshotMediator,
                    behandlingsstatistikkMediator = behandlingsstatistikkMediator,
                    notatMediator = notatMediator,
                    saksbehandlerhåndterer = saksbehandlerMediator,
                    oppgavehåndterer = oppgaveMediator,
                    totrinnsvurderinghåndterer = totrinnsvurderingMediator,
                    godkjenninghåndterer = godkjenningService,
                    personhåndterer = meldingMediator,
                    dokumenthåndterer = dokumentMediator,
                )

                routing {
                    authenticate("oidc") {
                        opptegnelseApi()
                    }
                }
            }.build()
        rapidsConnection.register(this)
    }

    fun start() =
        rapidsConnection.start().also {
            val beans: List<GarbageCollectorMXBean> = ManagementFactory.getGarbageCollectorMXBeans()
            logg.info("Registrerte garbage collectors etter oppstart: ${beans.joinToString { it.name }}")
        }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }

    private fun versjonAvKode(env: Map<String, String>): String {
        return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
    }
}

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<Modellfeil> { call: ApplicationCall, modellfeil: Modellfeil ->
            modellfeil.logger()
            call.respond(status = modellfeil.httpkode, message = modellfeil.tilFeilDto())
        }
        exception<Throwable> { call, cause ->
            val uri = call.request.uri
            val verb = call.request.httpMethod.value
            logg.error("Unhandled: $verb", cause)
            sikkerlogg.error("Unhandled: $verb - $uri", cause)
            call.respondText(
                text = "Det skjedde en uventet feil",
                status = HttpStatusCode.InternalServerError,
            )
            call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
        }
    }
}
