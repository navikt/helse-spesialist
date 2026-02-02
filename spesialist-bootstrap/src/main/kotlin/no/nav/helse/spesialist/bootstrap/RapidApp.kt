package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.helse.spesialist.client.krr.ClientKrrModule
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.kafka.KafkaModule
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
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                        privateJwk = env.getValue("AZURE_APP_JWK"),
                        msGraphUrl = "https://graph.microsoft.com/v1.0",
                    ),
                clientKrr =
                    ClientKrrModule.Configuration(
                        if (env.getBoolean("BRUK_DUMMY_FOR_KONTAKT_OG_RESERVASJONSREGISTERET")) {
                            null
                        } else {
                            ClientKrrModule.Configuration.Client(
                                apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
                                scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
                            )
                        },
                    ),
                clientSpleis =
                    ClientSpleisModule.Configuration(
                        spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
                        spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
                        loggRespons = env.getBoolean("SPLEIS_CLIENT_LOGG_RESPONS"),
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
                tilgangsgrupperTilBrukerroller =
                    TilgangsgrupperTilBrukerroller(
                        næringsdrivendeBeta = env.getUUIDList("ROLLE_SELVSTENDIG_BETA"),
                        beslutter = env.getUUIDList("ROLLE_BESLUTTER"),
                        egenAnsatt = env.getUUIDList("ROLLE_EGEN_ANSATT"),
                        kode7 = env.getUUIDList("ROLLE_KODE_7"),
                        stikkprøve = env.getUUIDList("ROLLE_STIKKPROVE"),
                        utvikler = env.getUUIDList("ROLLE_UTVIKLER"),
                    ),
                environmentToggles = EnvironmentTogglesImpl(env),
                stikkprøver = Stikkprøver.fraEnv(env),
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

        val clientKrrModule =
            ClientKrrModule(
                configuration = configuration.clientKrr,
                accessTokenGenerator = clientEntraIdModule.accessTokenGenerator,
            )

        val clientSpleisModule =
            ClientSpleisModule(
                configuration = configuration.clientSpleis,
                accessTokenGenerator = clientEntraIdModule.accessTokenGenerator,
            )

        val dbModule = DBModule(configuration.db)

        val kafkaModule =
            KafkaModule(
                configuration = configuration.kafka,
                rapidsConnection = rapidsConnection,
                sessionFactory = dbModule.sessionFactory,
                daos = dbModule.daos,
                stikkprøver = configuration.stikkprøver,
                tilgangsgruppehenter = clientEntraIdModule.tilgangsgruppehenter,
            )

        val apiModule =
            ApiModule(
                configuration = configuration.api,
                daos = dbModule.daos,
                meldingPubliserer = kafkaModule.meldingPubliserer,
                tilgangsgruppehenter = clientEntraIdModule.tilgangsgruppehenter,
                sessionFactory = dbModule.sessionFactory,
                environmentToggles = configuration.environmentToggles,
                snapshothenter = clientSpleisModule.snapshothenter,
                krrRegistrertStatusHenter = clientKrrModule.krrRegistrertStatusHenter,
                tilgangsgrupperTilBrukerroller = configuration.tilgangsgrupperTilBrukerroller,
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
