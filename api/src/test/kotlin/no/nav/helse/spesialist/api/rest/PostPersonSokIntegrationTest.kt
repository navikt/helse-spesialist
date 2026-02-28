package no.nav.helse.spesialist.api.rest

import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostPersonSokIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Finner klar person med identitetsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "identitetsnummer": "$identitetsnummer" }"""
        )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asText()) }
        assertEquals("true", actualJson["klarForVisning"].asText())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Finner klar person med aktørId`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "aktørId": "$aktørId" }"""
        )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asText()) }
        assertEquals("true", actualJson["klarForVisning"].asText())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Finner ikke-klar person med identitetsnummer og setter igang klargjøring`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "identitetsnummer": "$identitetsnummer" }"""
        )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asText()) }
        assertEquals("false", actualJson["klarForVisning"].asText())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = identitetsnummer,
                hendelse = KlargjørPersonForVisning,
                årsak = "klargjørPersonForVisning",
            ),
        )
    }

    @Test
    fun `Finner ikke-klar person med aktørId og setter igang klargjøring`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "aktørId": "$aktørId" }"""
        )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asText()) }
        assertEquals("false", actualJson["klarForVisning"].asText())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = person.id.value,
                hendelse = KlargjørPersonForVisning,
                årsak = "klargjørPersonForVisning",
            ),
        )
    }

    @Test
    fun `Gir feilmelding ved både identitetsnummer og aktørId i request`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId
        val identitetsnummer = person.id.value

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "aktørId": "$aktørId", "identitetsnummer": "$identitetsnummer" }"""
        )

        // Then:
        assertEquals(400, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertJsonEquals("""
            {
              "type" : "about:blank",
              "status" : 400,
              "title" : "Enten aktørId eller identitetsnummer må spesifiseres, ikke begge",
              "code" : "FOR_MANGE_INPUTPARAMETERE"
            }
        """.trimIndent(), actualJson)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gir feilmelding ved hverken identitetsnummer og aktørId i request`() {
        // Given:

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ }"""
        )

        // Then:
        assertEquals(400, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertJsonEquals("""
            {
              "type" : "about:blank",
              "status" : 400,
              "title" : "Enten aktørId eller identitetsnummer må spesifiseres, begge manglet",
              "code" : "MANGLER_INPUTPARAMETERE"
            }
        """.trimIndent(), actualJson)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gir feilmelding når person ikke finnes`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer().value

        // When:
        val response = integrationTestFixture.post(
            "/api/personer/sok",
            body = """{ "identitetsnummer": "$identitetsnummer" }"""
        )

        // Then:
        assertEquals(404, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertJsonEquals("""
            {
              "type" : "about:blank",
              "status" : 404,
              "title" : "Person ikke funnet",
              "code" : "PERSON_IKKE_FUNNET"
            }
        """.trimIndent(), actualJson)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Setter kun igang klargjøring én gang tross to requester på ikke-klar person`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value

        // When:
        repeat(3) {
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }"""
            )
        }

        // Then:
        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertPubliserteUtgåendeHendelser(
            InMemoryMeldingPubliserer.PublisertUtgåendeHendelse(
                fødselsnummer = identitetsnummer,
                hendelse = KlargjørPersonForVisning,
                årsak = "klargjørPersonForVisning",
            ),
        )
    }
}
