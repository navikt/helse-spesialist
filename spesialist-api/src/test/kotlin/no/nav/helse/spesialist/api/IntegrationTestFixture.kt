package no.nav.helse.spesialist.api

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson3.*
import io.ktor.server.testing.*
import io.ktor.sse.*
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.api.testfixtures.InMemoryPopulasjonstilgangskontrollProvider
import no.nav.helse.spesialist.application.*
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.readValue
import java.util.*
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class IntegrationTestFixture {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val daos = inMemoryRepositoriesAndDaos.daos
    val sessionFactory = inMemoryRepositoriesAndDaos.sessionFactory
    val sessionContext = sessionFactory.sessionContext
    val meldingPubliserer = InMemoryMeldingPubliserer()
    val personPseudoIdProvider = InMemoryPersonPseudoIdProvider()
    val populasjonstilgangskontrollProvider = InMemoryPopulasjonstilgangskontrollProvider()

    companion object {
        val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
        val tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller()
        val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()

        val apiModuleIntegrationTestFixture =
            ApiModuleIntegrationTestFixture(
                mockOAuth2Server = mockOAuth2Server,
                tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
                tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
            )

        fun Response.get(sti: String): JsonNode {
            val stiSegments = sti.split(".")
            val gjeldende = this.bodyAsJsonNode ?: error("Body er tom")
            return stiSegments.fold(gjeldende) { acc, segment ->
                acc.get(segment)
            }
        }
    }

    val krrRegistrertStatusHenterMock: KrrRegistrertStatusHenter = mockk(relaxed = true)
    val behandlendeEnhetHenterMock: BehandlendeEnhetHenter = mockk(relaxed = true)
    val personinfoHenterMock: PersoninfoHenter = mockk(relaxed = true)
    val forsikringsvurderingHenterMock: ForsikringsvurderingHenter = mockk(relaxed = true)
    val alleIdenterHenterMock: AlleIdenterHenter = mockk(relaxed = true)
    val infotrygdperiodeHenterMock: InfotrygdperiodeHenter = mockk(relaxed = true)

    private val apiModule =
        ApiModule(
            configuration = apiModuleIntegrationTestFixture.apiModuleConfiguration,
            daos = daos,
            meldingPubliserer = meldingPubliserer,
            sessionFactory = sessionFactory,
            opptegnelseListener =
                object : OpptegnelseListener {
                    override suspend fun onOpptegnelse(
                        identitetsnummer: Identitetsnummer,
                        block: suspend () -> Unit,
                    ) {
                        flow {
                            repeat(10) {
                                delay(50.milliseconds)
                                emit(Unit)
                            }
                        }.collect {
                            block()
                        }
                    }
                },
            environmentToggles = mockk(relaxed = true),
            snapshothenter = mockk(relaxed = true),
            krrRegistrertStatusHenter = krrRegistrertStatusHenterMock,
            behandlendeEnhetHenter = behandlendeEnhetHenterMock,
            forsikringsvurderingHenter = forsikringsvurderingHenterMock,
            alleIdenterHenter = alleIdenterHenterMock,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
            tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
            personinfoHenter = personinfoHenterMock,
            infotrygdperiodeHenter = infotrygdperiodeHenterMock,
            personPseudoIdProvider = personPseudoIdProvider,
            populasjonstilgangskontrollProvider = populasjonstilgangskontrollProvider,
        )

    class Response(
        val status: Int,
        val bodyAsText: String,
    ) {
        val bodyAsJsonNode = bodyAsText.takeUnless(String::isEmpty)?.let(objectMapper::readTree)

        inline fun <reified T> body(): T = objectMapper.readValue<T>(bodyAsText)
    }

    fun sse(
        url: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Les),
        brukerroller: Set<Brukerrolle> = emptySet(),
        block: suspend (List<ServerSentEvent>) -> Unit,
    ) {
        testApplication {
            application { apiModule.setUpApi(this) }
            client = createClient { install(SSE) }

            logg.info("Starter SSE-tilkobling til $url")
            val events = Collections.synchronizedList(mutableListOf<ServerSentEvent>())
            coroutineScope {
                val started = CompletableDeferred<Unit>()
                val sseJob =
                    launch {
                        client.sse(urlString = url, request = {
                            bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                        }) {
                            started.complete(Unit)
                            incoming.collect { event ->
                                logg.info("Mottok server sent event: $event")
                                events.add(event)
                            }
                        }
                    }
                // Vent til tilkoblingen er aktiv
                started.await()

                try {
                    block(events)
                } finally {
                    sseJob.cancelAndJoin()
                    logg.info("Avsluttet SSE-tilkobling til $url")
                }
            }
        }
    }

    fun get(
        url: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Les),
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            logg.info("Sender GET $url")
            val httpResponse =
                client.get(url) {
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                }
            val bodyAsText = httpResponse.bodyAsText()
            logg.info("Fikk respons: $bodyAsText")
            response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
        }

        return response
    }

    fun post(
        url: String,
        @Language("JSON") body: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Skriv),
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            logg.info("Sender POST $url med data $body")
            val httpResponse =
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                    setBody(body)
                }
            val bodyAsText = httpResponse.bodyAsText()
            logg.info("Fikk respons: $bodyAsText")
            response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
        }

        return response
    }

    fun put(
        url: String,
        @Language("JSON") body: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Skriv),
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            logg.info("Sender PUT $url med data $body")
            val httpResponse =
                client.put(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                    setBody(body)
                }
            val bodyAsText = httpResponse.bodyAsText()
            logg.info("Fikk respons: $bodyAsText")
            response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
        }

        return response
    }

    fun patch(
        url: String,
        @Language("JSON") body: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Skriv),
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            logg.info("Sender PATCH $url med data $body")
            val httpResponse =
                client.patch(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                    setBody(body)
                }
            val bodyAsText = httpResponse.bodyAsText()
            logg.info("Fikk respons: $bodyAsText")
            response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
        }

        return response
    }

    fun delete(
        url: String,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        tilganger: Set<Tilgang> = setOf(Tilgang.Skriv),
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Response {
        lateinit var response: Response

        testApplication {
            application {
                apiModule.setUpApi(this)
            }

            client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            logg.info("Sender DELETE $url")
            val httpResponse =
                client.delete(url) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler, tilganger, brukerroller))
                }
            val bodyAsText = httpResponse.bodyAsText()
            logg.info("Fikk respons: $bodyAsText")
            response = Response(status = httpResponse.status.value, bodyAsText = bodyAsText)
        }

        return response
    }

    fun assertPubliserteBehovLister(
        vararg publiserteBehovLister: InMemoryMeldingPubliserer.PublisertBehovListe,
    ) {
        assertEquals(
            publiserteBehovLister.toList(),
            meldingPubliserer.publiserteBehovLister,
        )
    }

    fun assertPubliserteKommandokjedeEndretEvents(
        vararg publiserteKommandokjedeEndretEvents: InMemoryMeldingPubliserer.PublisertKommandokjedeEndretEvent,
    ) {
        assertEquals(
            publiserteKommandokjedeEndretEvents.toList(),
            meldingPubliserer.publiserteKommandokjedeEndretEvents,
        )
    }

    fun assertPubliserteSubsumsjoner(
        vararg publiserteSubsumsjoner: InMemoryMeldingPubliserer.PublisertSubsumsjon,
    ) {
        assertEquals(publiserteSubsumsjoner.size, meldingPubliserer.publiserteSubsumsjoner.size)
        publiserteSubsumsjoner.zip(meldingPubliserer.publiserteSubsumsjoner).forEach { (expected, actual) ->
            assertEquals(expected.fødselsnummer, actual.fødselsnummer)
            assertSubsumsjonEventEquals(expected.subsumsjonEvent, actual.subsumsjonEvent)
            assertEquals(expected.versjonAvKode, actual.versjonAvKode)
        }
    }

    private fun assertSubsumsjonEventEquals(
        expected: SubsumsjonEvent,
        actual: SubsumsjonEvent,
    ) {
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
        vararg publiserteUtgåendeHendelse: InMemoryMeldingPubliserer.PublisertUtgåendeHendelse,
    ) {
        assertEquals(
            publiserteUtgåendeHendelse.toList(),
            meldingPubliserer.publiserteUtgåendeHendelser,
        )
    }

    fun assertPubliserteUtgåendeHendelser(
        vararg assertionsPerHendelse: (actualUtgåendeHendelse: InMemoryMeldingPubliserer.PublisertUtgåendeHendelse) -> Unit,
    ) {
        assertEquals(
            expected = assertionsPerHendelse.size,
            actual = meldingPubliserer.publiserteUtgåendeHendelser.size,
            message = "Uventet antall meldinger. Faktiske meldinger: ${meldingPubliserer.publiserteUtgåendeHendelser}",
        )
        assertionsPerHendelse
            .zip(meldingPubliserer.publiserteUtgåendeHendelser)
            .forEach { (assertion, actual) -> assertion(actual) }
    }

    fun assertIngenPubliserteUtgåendeHendelser() {
        assertEquals(
            expected = 0,
            actual = meldingPubliserer.publiserteUtgåendeHendelser.size,
            message = "Forventet ingen meldinger, men følgende ble publisert: ${meldingPubliserer.publiserteUtgåendeHendelser}",
        )
    }
}
