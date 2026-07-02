package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.helse.spesialist.client.krr.ClientKrrModule
import no.nav.helse.spesialist.client.personpseudoid.ClientPersonPseudoIdModule
import no.nav.helse.spesialist.client.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.spesialist.client.sparkel.norg.ClientSparkelNorgModule
import no.nav.helse.spesialist.client.sparkel.sykepengeperioder.ClientSparkelSykepengeperioderModule
import no.nav.helse.spesialist.client.speed.ClientSpeedModule
import no.nav.helse.spesialist.client.spforsikring.ClientSpForsikringModule
import no.nav.helse.spesialist.client.spillkar.ClientSpillkarModule
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import no.nav.helse.spesialist.client.tilgangsmaskinen.ClientTilgangsmaskinenModule
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.kafka.KafkaModule
import no.nav.helse.spesialist.valkey.ValkeyModule
import java.net.URI
import java.util.UUID

fun main() {
    val env = System.getenv()
    val versjonAvKode = env.getValue("NAIS_APP_IMAGE")
    val rapidApp = RapidApp()
    rapidApp.start(
        configuration =
            Configuration(
                api =
                    ApiModule.Configuration(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                        eksponerOpenApi = env.getBoolean("EKSPONER_OPENAPI"),
                        versjonAvKode = versjonAvKode,
                    ),
                clientEntraID =
                    ClientEntraIDModule.Configuration(
                        tokenEndpoint = env.getValue("NAIS_TOKEN_ENDPOINT"),
                        tokenExchangeEndpoint = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
                        msGraphUrl = "https://graph.microsoft.com",
                    ),
                clientKrr =
                    ClientKrrModule.Configuration(
                        apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
                        scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
                    ),
                clientSparkelNorg =
                    ClientSparkelNorgModule.Configuration(
                        apiUrl = env.getValue("SPARKEL_NORG_API_URL"),
                        scope = env.getValue("SPARKEL_NORG_SCOPE"),
                    ),
                clientSparkelSykepengeperioder =
                    ClientSparkelSykepengeperioderModule.Configuration(
                        apiUrl = env.getValue("SPARKEL_SYKEPENGEPERIODER_API_URL"),
                        scope = env.getValue("SPARKEL_SYKEPENGEPERIODER_SCOPE"),
                    ),
                clientSpeed =
                    ClientSpeedModule.Configuration(
                        apiUrl = env.getValue("SPEED_API_URL"),
                        scope = env.getValue("SPEED_SCOPE"),
                    ),
                clientSpillkar =
                    ClientSpillkarModule.Configuration(
                        apiUrl = env.getValue("SPILLKAR_API_URL"),
                        scope = env.getValue("SPILLKAR_SCOPE"),
                    ),
                clientSpForsikring =
                    ClientSpForsikringModule.Configuration(
                        apiUrl = env.getValue("SP_FORSIKRING_API_URL"),
                        scope = env.getValue("SP_FORSIKRING_SCOPE"),
                    ),
                clientSpleis =
                    ClientSpleisModule.Configuration(
                        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                        spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
                        loggRespons = env.getBoolean("SPLEIS_CLIENT_LOGG_RESPONS"),
                    ),
                clientPersonPseudoId =
                    ClientPersonPseudoIdModule.Configuration(
                        connectionString = env.getValue("VALKEY_URI_PERSONPSEUDOID"),
                        brukernavn = env.getValue("VALKEY_USERNAME_PERSONPSEUDOID"),
                        passord = env.getValue("VALKEY_PASSWORD_PERSONPSEUDOID"),
                    ),
                clientTilgangsmaskinen =
                    ClientTilgangsmaskinenModule.Configuration(
                        baseUrl = env.getValue("TILGANGSMASKINEN_BASE_URL"),
                        scope = env.getValue("TILGANGSMASKINEN_SCOPE"),
                    ),
                db =
                    DBModule.Configuration(
                        jdbcUrl =
                            "jdbc:postgresql://" +
                                env.getValue("DATABASE_HOST") +
                                ":" +
                                env.getValue("DATABASE_PORT") +
                                "/" +
                                env.getValue("DATABASE_DATABASE"),
                        username = env.getValue("DATABASE_USERNAME"),
                        password = env.getValue("DATABASE_PASSWORD"),
                    ),
                kafka =
                    KafkaModule.Configuration(
                        versjonAvKode = versjonAvKode,
                        ignorerMeldingerForUkjentePersoner =
                            env.getBoolean("IGNORER_MELDINGER_FOR_UKJENTE_PERSONER"),
                    ),
                valkey =
                    ValkeyModule.Configuration(
                        ValkeyModule.Configuration.Valkey(
                            host = env.getValue("VALKEY_HOST_SPESIALIST_CACHE"),
                            port = env.getInt("VALKEY_PORT_SPESIALIST_CACHE"),
                            username = env.getValue("VALKEY_USERNAME_SPESIALIST_CACHE"),
                            password = env.getValue("VALKEY_PASSWORD_SPESIALIST_CACHE"),
                        ),
                    ),
                tilgangsgrupperTilBrukerroller =
                    TilgangsgrupperTilBrukerroller(
                        næringsdrivendeBeta = env.getUUIDList("ROLLE_SELVSTENDIG_BETA"),
                        beslutter = env.getUUIDList("ROLLE_BESLUTTER"),
                        egenAnsatt = env.getUUIDList("ROLLE_EGEN_ANSATT"),
                        kode7 = env.getUUIDList("ROLLE_KODE_7"),
                        stikkprøve = env.getUUIDList("ROLLE_STIKKPROVE"),
                        utvikler = env.getUUIDList("ROLLE_UTVIKLER"),
                        dialogmelding = env.getUUIDList("ROLLE_DIALOGMELDING"),
                    ),
                tilgangsgrupperTilTilganger =
                    TilgangsgrupperTilTilganger(
                        skrivetilgang = env.getUUIDList("TILGANG_SKRIV"),
                        lesetilgang = env.getUUIDList("TILGANG_LES"),
                    ),
                environmentToggles = EnvironmentTogglesImpl(env),
                stikkprøver = Stikkprøver.Configuration.fromEnv(env),
            ),
        rapidsConnection =
            RapidApplication.create(
                env = env,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also(Metrics.globalRegistry::add),
                builder = {
                    withKtorModule {
                        // Deferret fordi det er sirkulær avhengighet mellom RapidApplication og Ktor-oppsettet...
                        rapidApp.ktorSetupCallback(this)
                    }
                },
            ),
    )
}

private fun Map<String, String>.getUUIDList(key: String): List<UUID> = this[key]?.split(",")?.map { UUID.fromString(it.trim()) } ?: emptyList()

private fun Map<String, String>.getBoolean(key: String): Boolean = this[key].toBoolean()

private fun Map<String, String>.getInt(key: String): Int = getValue(key).toInt()

class RapidApp {
    class Modules(
        val dbModule: DBModule,
    )

    lateinit var ktorSetupCallback: (Application) -> Unit

    fun start(
        configuration: Configuration,
        rapidsConnection: RapidsConnection,
    ): Modules {
        val clientEntraIdModule =
            ClientEntraIDModule(
                configuration = configuration.clientEntraID,
                tilgangsgrupperTilBrukerroller = configuration.tilgangsgrupperTilBrukerroller,
            )

        val valkeyModule =
            ValkeyModule(
                configuration = configuration.valkey,
            )

        val clientKrrModule =
            ClientKrrModule(
                configuration = configuration.clientKrr,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
                cache = valkeyModule.cache,
            )

        val clientSparkelNorgModule =
            ClientSparkelNorgModule(
                configuration = configuration.clientSparkelNorg,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
                cache = valkeyModule.cache,
            )

        val clientSparkelSykepengeperioderModule =
            ClientSparkelSykepengeperioderModule(
                configuration = configuration.clientSparkelSykepengeperioder,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
                cache = valkeyModule.cache,
            )

        val clientSpeedModule =
            ClientSpeedModule(
                configuration = configuration.clientSpeed,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
                cache = valkeyModule.cache,
            )

        val clientSpillkarModule =
            ClientSpillkarModule(
                configuration = configuration.clientSpillkar,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
            )

        val clientSpForsikringModule =
            ClientSpForsikringModule(
                configuration = configuration.clientSpForsikring,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
            )

        val clientSpleisModule =
            ClientSpleisModule(
                configuration = configuration.clientSpleis,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
            )

        val clientTilgangsmaskinenModule =
            ClientTilgangsmaskinenModule(
                configuration = configuration.clientTilgangsmaskinen,
                accessTokenProvider = clientEntraIdModule.accessTokenProvider,
            )

        val dbModule = DBModule(configuration.db)

        val personPseudoIdProvider =
            ValkeyPersonPseudoIdProvider(
                configuration = configuration.clientPersonPseudoId,
                fallbackPersonPseudoIdProvider = dbModule.personPseudoIdDao,
            )

        val kafkaModule =
            KafkaModule(
                configuration = configuration.kafka,
                rapidsConnection = rapidsConnection,
                sessionFactory = dbModule.sessionFactory,
                daos = dbModule.daos,
                stikkprøver = configuration.stikkprøver,
                brukerrollehenter = clientEntraIdModule.tilgangsgruppehenter,
                forsikringsvurderingHenter = clientSpForsikringModule.spForsikringClientForsikringsvurderingHenter,
                environmentToggles = configuration.environmentToggles,
            )

        val apiModule =
            ApiModule(
                configuration = configuration.api,
                daos = dbModule.daos,
                meldingPubliserer = kafkaModule.meldingPubliserer,
                sessionFactory = dbModule.sessionFactory,
                opptegnelseListener = dbModule.opptegnelseListener,
                environmentToggles = configuration.environmentToggles,
                snapshothenter = clientSpleisModule.snapshothenter,
                krrRegistrertStatusHenter = clientKrrModule.krrRegistrertStatusHenter,
                behandlendeEnhetHenter = clientSparkelNorgModule.behandlendeEnhetHenter,
                forsikringsvurderingHenter = clientSpForsikringModule.spForsikringClientForsikringsvurderingHenter,
                inngangsvilkårHenter = clientSpillkarModule.inngangsvilkårHenter,
                inngangsvilkårInnsender = clientSpillkarModule.inngangsvilkårInnsender,
                alleIdenterHenter = clientSpeedModule.alleIdenterHenter,
                tilgangsgrupperTilBrukerroller = configuration.tilgangsgrupperTilBrukerroller,
                tilgangsgrupperTilTilganger = configuration.tilgangsgrupperTilTilganger,
                personinfoHenter = clientSpeedModule.personinfoHenter,
                infotrygdperiodeHenter = clientSparkelSykepengeperioderModule.sykepengeperioderHenter,
                personPseudoIdProvider = personPseudoIdProvider,
                populasjonstilgangskontrollProvider = clientTilgangsmaskinenModule.tilgangsmaskinenClient,
            )

        kafkaModule.kobleOppRivers()

        val modules = Modules(dbModule)
        ktorSetupCallback = { ktorApplication ->
            apiModule.setUpApi(
                application = ktorApplication,
            )
            ktorApplication.monitor.subscribe(ApplicationStopped) {
                modules.dbModule.shutdown()
            }
        }

        rapidsConnection.start()
        return modules
    }
}
