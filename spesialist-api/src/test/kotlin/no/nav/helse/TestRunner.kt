package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.configureKtorApplication
import no.nav.helse.spesialist.api.graphql.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.SpesialistSchema
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.MutationHandlers
import no.nav.helse.spesialist.api.graphql.SpesialistSchema.QueryHandlers
import no.nav.helse.spesialist.api.graphql.StansAutomatiskBehandlinghåndterer
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
import no.nav.helse.spesialist.api.rest.DokumentMediator
import no.nav.helse.spesialist.api.rest.RestAdapter
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.HistoriskeIdenterHenter
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language

object TestRunner {
    private val mockOAuth2Server: MockOAuth2Server =
        MockOAuth2Server().also {
            it.start()
        }
    private val issuerId = "EntraID"
    private val clientId = "spesialist-dev"

    private val configuration =
        ApiModule.Configuration(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
            eksponerOpenApi = true,
            versjonAvKode = "0.0.0",
        )

    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()

    private fun token(
        saksbehandler: Saksbehandler,
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = issuerId,
                audience = clientId,
                subject = saksbehandler.id.value.toString(),
                claims =
                    mapOf(
                        "NAVident" to saksbehandler.ident.value,
                        "preferred_username" to saksbehandler.epost,
                        "oid" to saksbehandler.id.value.toString(),
                        "name" to saksbehandler.navn,
                        "groups" to emptyList<String>(),
                    ),
            ).serialize()

    fun runQuery(
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        given: (avhengigheter: Avhengigheter) -> Unit = {},
        @Language("GraphQL") whenever: String,
        then: suspend (response: HttpResponse, body: JsonNode, avhengigheter: Avhengigheter) -> Unit,
    ) {
        val avhengigheter =
            Avhengigheter(
                daos = mockk(relaxed = true),
                sessionFactory = inMemoryRepositoriesAndDaos.sessionFactory,
                saksbehandlerMediator = mockk(relaxed = true),
                restAdapter = mockk(relaxed = true),
                apiOppgaveService = mockk(relaxed = true),
                personhåndterer = mockk(relaxed = true),
                dokumentMediator = mockk(relaxed = true),
                stansAutomatiskBehandlinghåndterer = mockk(relaxed = true),
                behandlingstatistikk = mockk(relaxed = true),
                snapshothenter = mockk(relaxed = true),
                krrRegistrertStatusHenter = mockk(relaxed = true),
                forsikringHenter = mockk(relaxed = true),
                inngangsvilkårHenter = mockk(relaxed = true),
                inngangsvilkårInnsender = mockk(relaxed = true),
                historiskeIdenterHenter = mockk(relaxed = true),
                meldingPubliserer = mockk(relaxed = true),
                tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller(),
                tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger(),
            )
        testApplication {
            application {
                val spesialistSchema =
                    SpesialistSchema(
                        queryHandlers =
                            QueryHandlers(
                                person =
                                    PersonQueryHandler(
                                        daos = avhengigheter.daos,
                                        apiOppgaveService = avhengigheter.apiOppgaveService,
                                        stansAutomatiskBehandlinghåndterer = avhengigheter.stansAutomatiskBehandlinghåndterer,
                                        personhåndterer = avhengigheter.personhåndterer,
                                        snapshothenter = avhengigheter.snapshothenter,
                                        sessionFactory = avhengigheter.sessionFactory,
                                    ),
                                oppgaver =
                                    OppgaverQueryHandler(
                                        apiOppgaveService = avhengigheter.apiOppgaveService,
                                    ),
                                behandlingsstatistikk =
                                    BehandlingsstatistikkQueryHandler(
                                        behandlingsstatistikkMediator = avhengigheter.behandlingstatistikk,
                                    ),
                            ),
                        mutationHandlers =
                            MutationHandlers(
                                tildeling = TildelingMutationHandler(saksbehandlerMediator = avhengigheter.saksbehandlerMediator),
                                overstyring =
                                    OverstyringMutationHandler(
                                        saksbehandlerMediator = avhengigheter.saksbehandlerMediator,
                                    ),
                                skjonnsfastsettelse = SkjonnsfastsettelseMutationHandler(saksbehandlerMediator = avhengigheter.saksbehandlerMediator),
                                totrinnsvurdering =
                                    TotrinnsvurderingMutationHandler(
                                        saksbehandlerMediator = avhengigheter.saksbehandlerMediator,
                                    ),
                                person = PersonMutationHandler(personhåndterer = avhengigheter.personhåndterer),
                                paVent = PaVentMutationHandler(saksbehandlerMediator = avhengigheter.saksbehandlerMediator),
                                stansAutomatiskBehandling = StansAutomatiskBehandlingMutationHandler(sessionFactory = avhengigheter.sessionFactory),
                            ),
                    )
                configureKtorApplication(
                    ktorApplication = this,
                    apiModuleConfiguration = configuration,
                    spesialistSchema = spesialistSchema,
                    dokumentMediator = avhengigheter.dokumentMediator,
                    sessionFactory = avhengigheter.sessionFactory,
                    meldingPubliserer = avhengigheter.meldingPubliserer,
                    environmentToggles =
                        object : EnvironmentToggles {
                            override val kanBeslutteEgneSaker: Boolean = false
                            override val kanGodkjenneUtenBesluttertilgang: Boolean = false
                            override val kanSeForsikring: Boolean = false
                            override val devGcp: Boolean = false
                        },
                    krrRegistrertStatusHenter = avhengigheter.krrRegistrertStatusHenter,
                    tilgangsgrupperTilBrukerroller = avhengigheter.tilgangsgrupperTilBrukerroller,
                    tilgangsgrupperTilTilganger = avhengigheter.tilgangsgrupperTilTilganger,
                    forsikringHenter = avhengigheter.forsikringHenter,
                    inngangsvilkårHenter = avhengigheter.inngangsvilkårHenter,
                    inngangsvilkårInnsender = avhengigheter.inngangsvilkårInnsender,
                    historiskeIdenterHenter = avhengigheter.historiskeIdenterHenter,
                )
            }

            given(avhengigheter)

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }
            val response =
                client.post("/graphql") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(token(saksbehandler))
                    setBody(mapOf("query" to whenever))
                }

            then(response, response.body(), avhengigheter)
        }
    }

    data class Avhengigheter(
        val daos: Daos,
        val sessionFactory: SessionFactory,
        val saksbehandlerMediator: SaksbehandlerMediator,
        val restAdapter: RestAdapter,
        val apiOppgaveService: ApiOppgaveService,
        val personhåndterer: Personhåndterer,
        val dokumentMediator: DokumentMediator,
        val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
        val behandlingstatistikk: IBehandlingsstatistikkService,
        val snapshothenter: Snapshothenter,
        val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
        val forsikringHenter: ForsikringHenter,
        val inngangsvilkårHenter: InngangsvilkårHenter,
        val inngangsvilkårInnsender: InngangsvilkårInnsender,
        val historiskeIdenterHenter: HistoriskeIdenterHenter,
        val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
        val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
        val meldingPubliserer: MeldingPubliserer,
    )
}
