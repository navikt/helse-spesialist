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
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.graphql.settOppGraphQLApi
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language

object TestRunner {
    private val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server().also {
        it.start()
    }
    private val issuerId = "EntraID"
    private val clientId = "spesialist-dev"

    private val azureConfig = AzureConfig(
        clientId = clientId,
        issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
        jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
        tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
    )

    private fun token(saksbehandlerFraApi: SaksbehandlerFraApi): String = mockOAuth2Server.issueToken(
        issuerId = issuerId,
        audience = clientId,
        subject = saksbehandlerFraApi.oid.toString(),
        claims = mapOf(
            "NAVident" to saksbehandlerFraApi.ident,
            "preferred_username" to saksbehandlerFraApi.epost,
            "oid" to saksbehandlerFraApi.oid.toString(),
            "name" to saksbehandlerFraApi.navn,
            "groups" to saksbehandlerFraApi.grupper.map { it.toString() }.toTypedArray()
        )
    ).serialize()

    fun runQuery(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        given: (avhengigheter: Avhengigheter) -> Unit = {},
        @Language("GraphQL") whenever: String,
        then: suspend (response: HttpResponse, body: JsonNode) -> Unit,
    ) {
        val avhengigheter = Avhengigheter(
            daos = mockk(relaxed = true),
            sessionFactory = mockk(relaxed = true),
            saksbehandlerMediator = mockk(relaxed = true),
            apiOppgaveService = mockk(relaxed = true),
            godkjenninghåndterer = mockk(relaxed = true),
            personhåndterer = mockk(relaxed = true),
            dokumenthåndterer = mockk(relaxed = true),
            stansAutomatiskBehandlinghåndterer = mockk(relaxed = true),
            behandlingstatistikk = mockk(relaxed = true),
            snapshothenter = mockk(relaxed = true),
            reservasjonshenter = mockk(relaxed = true),
            tilgangsgrupper = mockk(relaxed = true),
            meldingPubliserer = mockk(relaxed = true),
            featureToggles = mockk(relaxed = true),
        )
        testApplication {
            application {
                settOppGraphQLApi(
                    daos = avhengigheter.daos,
                    sessionFactory = avhengigheter.sessionFactory,
                    saksbehandlerMediator = avhengigheter.saksbehandlerMediator,
                    apiOppgaveService = avhengigheter.apiOppgaveService,
                    godkjenninghåndterer = avhengigheter.godkjenninghåndterer,
                    personhåndterer = avhengigheter.personhåndterer,
                    dokumenthåndterer = avhengigheter.dokumenthåndterer,
                    stansAutomatiskBehandlinghåndterer = avhengigheter.stansAutomatiskBehandlinghåndterer,
                    behandlingstatistikk = avhengigheter.behandlingstatistikk,
                    snapshothenter = avhengigheter.snapshothenter,
                    reservasjonshenter = avhengigheter.reservasjonshenter,
                    tilgangsgrupper = avhengigheter.tilgangsgrupper,
                    meldingPubliserer = avhengigheter.meldingPubliserer,
                    featureToggles = avhengigheter.featureToggles,
                    azureConfig = azureConfig,
                )
            }

            given(avhengigheter)
            val client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }
            val response = client.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(token(saksbehandlerFraApi))
                setBody(mapOf("query" to whenever))
            }

            then(response, response.body())
        }
    }

    data class Avhengigheter(
        val daos: Daos,
        val sessionFactory: SessionFactory,
        val saksbehandlerMediator: SaksbehandlerMediator,
        val apiOppgaveService: ApiOppgaveService,
        val godkjenninghåndterer: Godkjenninghåndterer,
        val personhåndterer: Personhåndterer,
        val dokumenthåndterer: Dokumenthåndterer,
        val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
        val behandlingstatistikk: IBehandlingsstatistikkService,
        val snapshothenter: Snapshothenter,
        val reservasjonshenter: Reservasjonshenter,
        val tilgangsgrupper: Tilgangsgrupper,
        val meldingPubliserer: MeldingPubliserer,
        val featureToggles: FeatureToggles,
    )
}
