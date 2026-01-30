package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgruppeUuider
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import kotlin.test.assertEquals

class IntegrationTestFixture() {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val daos = inMemoryRepositoriesAndDaos.daos
    val sessionFactory = inMemoryRepositoriesAndDaos.sessionFactory
    val meldingPubliserer = InMemoryMeldingPubliserer()

    companion object {
        val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
        val tilgangsgruppeUuider = randomTilgangsgruppeUuider()
        val apiModuleIntegrationTestFixture = ApiModuleIntegrationTestFixture(
            mockOAuth2Server = mockOAuth2Server,
            tilgangsgruppeUuider = tilgangsgruppeUuider
        )
        val tilgangsgrupperTilBrukerroller = randomTilgangsgrupperTilBrukerroller()

        fun Response.get(sti: String): JsonNode {
            val stiSegments = sti.split(".")
            val gjeldende = this.bodyAsJsonNode ?: error("Body er tom")
            return stiSegments.fold(gjeldende) { acc, segment ->
                acc.get(segment)
            }
        }
    }

    val krrRegistrertStatusHenterMock: KrrRegistrertStatusHenter = mockk(relaxed = true)

    private val apiModule = ApiModule(
        configuration = apiModuleIntegrationTestFixture.apiModuleConfiguration,
        tilgangsgruppeUuider = tilgangsgruppeUuider,
        daos = daos,
        meldingPubliserer = meldingPubliserer,
        tilgangsgruppehenter = { Either.Success(emptySet<Tilgangsgruppe>() to emptySet()) },
        sessionFactory = sessionFactory,
        environmentToggles = mockk(relaxed = true),
        snapshothenter = mockk(relaxed = true),
        krrRegistrertStatusHenter = krrRegistrertStatusHenterMock,
        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller
    )

    class Response(
        val status: Int,
        val bodyAsText: String
    ) {
        val bodyAsJsonNode = bodyAsText.takeUnless(String::isEmpty)?.let(objectMapper::readTree)
    }

    fun get(
        url: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilgangsgrupper: Set<Tilgangsgruppe> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            runBlocking {
                logg.info("Sender GET $url")
                val httpResponse = client.get(url) {
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
                }
                val bodyAsText = httpResponse.bodyAsText()
                logg.info("Fikk respons: $bodyAsText")
                response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
            }
        }

        return response
    }

    fun post(
        url: String,
        @Language("JSON") body: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilgangsgrupper: Set<Tilgangsgruppe> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            runBlocking {
                logg.info("Sender POST $url med data $body")
                val httpResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
                    setBody(body)
                }
                val bodyAsText = httpResponse.bodyAsText()
                logg.info("Fikk respons: $bodyAsText")
                response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
            }
        }

        return response
    }

    fun put(
        url: String,
        @Language("JSON") body: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilgangsgrupper: Set<Tilgangsgruppe> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            runBlocking {
                logg.info("Sender PUT $url med data $body")
                val httpResponse = client.put(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
                    setBody(body)
                }
                val bodyAsText = httpResponse.bodyAsText()
                logg.info("Fikk respons: $bodyAsText")
                response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
            }
        }

        return response
    }

    fun delete(
        url: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilgangsgrupper: Set<Tilgangsgruppe> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client = createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }

            runBlocking {
                logg.info("Sender DELETE $url")
                val httpResponse = client.delete(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
                }
                val bodyAsText = httpResponse.bodyAsText()
                logg.info("Fikk respons: $bodyAsText")
                response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
            }
        }

        return response
    }

    fun assertPubliserteBehovLister(
        vararg publiserteBehovLister: InMemoryMeldingPubliserer.PublisertBehovListe
    ) {
        assertEquals(
            publiserteBehovLister.toList(),
            meldingPubliserer.publiserteBehovLister
        )
    }

    fun assertPubliserteKommandokjedeEndretEvents(
        vararg publiserteKommandokjedeEndretEvents: InMemoryMeldingPubliserer.PublisertKommandokjedeEndretEvent
    ) {
        assertEquals(
            publiserteKommandokjedeEndretEvents.toList(),
            meldingPubliserer.publiserteKommandokjedeEndretEvents
        )
    }

    fun assertPubliserteSubsumsjoner(
        vararg publiserteSubsumsjoner: InMemoryMeldingPubliserer.PublisertSubsumsjon
    ) {
        assertEquals(publiserteSubsumsjoner.size, meldingPubliserer.publiserteSubsumsjoner.size)
        publiserteSubsumsjoner.zip(meldingPubliserer.publiserteSubsumsjoner).forEach { (expected, actual) ->
            assertEquals(expected.fødselsnummer, actual.fødselsnummer)
            assertSubsumsjonEventEquals(expected.subsumsjonEvent, actual.subsumsjonEvent)
            assertEquals(expected.versjonAvKode, actual.versjonAvKode)
        }
    }

    private fun assertSubsumsjonEventEquals(expected: SubsumsjonEvent, actual: SubsumsjonEvent) {
        assertEquals(expected.fødselsnummer, actual.fødselsnummer)
        assertEquals(expected.paragraf, actual.paragraf)
        assertEquals(expected.ledd, actual.ledd)
        assertEquals(expected.bokstav, actual.bokstav)
        assertEquals(expected.lovverk, actual.lovverk)
        assertEquals(expected.lovverksversjon, actual.lovverksversjon)
        assertEquals(expected.utfall, actual.utfall)
        assertEquals(expected.input, actual.input)
        assertEquals(expected.output, actual.output)
        assertEquals(expected.sporing, actual.sporing)
        assertEquals(expected.kilde, actual.kilde)
    }

    fun assertPubliserteUtgåendeHendelser(
        vararg publiserteUtgåendeHendelse: InMemoryMeldingPubliserer.PublisertUtgåendeHendelse
    ) {
        assertEquals(
            publiserteUtgåendeHendelse.toList(),
            meldingPubliserer.publiserteUtgåendeHendelser
        )
    }

    fun assertPubliserteUtgåendeHendelser(
        vararg assertionsPerHendelse: (actualUtgåendeHendelse: InMemoryMeldingPubliserer.PublisertUtgåendeHendelse) -> Unit
    ) {
        assertEquals(
            expected = assertionsPerHendelse.size,
            actual = meldingPubliserer.publiserteUtgåendeHendelser.size,
            message = "Uventet antall meldinger. Faktiske meldinger: ${meldingPubliserer.publiserteUtgåendeHendelser}"
        )
        assertionsPerHendelse
            .zip(meldingPubliserer.publiserteUtgåendeHendelser)
            .forEach { (assertion, actual) -> assertion(actual) }
    }

    fun assertIngenPubliserteUtgåendeHendelser() {
        assertEquals(
            expected = 0,
            actual = meldingPubliserer.publiserteUtgåendeHendelser.size,
            message = "Forventet ingen meldinger, men følgende ble publisert: ${meldingPubliserer.publiserteUtgåendeHendelser}"
        )
    }
}
