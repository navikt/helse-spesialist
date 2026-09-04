package no.nav.helse.spesialist.api.rest

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.AlleIdenterHenter.IdentType
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostPersonSokIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext

    @Test
    fun `Finner person som allerede har personinfo, med identitetsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asString()) }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Finner person som allerede har personinfo, med aktørId`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "aktørId": "$aktørId" }""",
            )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asString()) }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Klargjør person uten personinfo synkront ved søk med identitetsnummer, og publiserer ingenting`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id) } returns lagPersoninfo()

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asString()) }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Klargjør person uten personinfo synkront ved søk med aktørId, og publiserer ingenting`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id) } returns lagPersoninfo()

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "aktørId": "$aktørId" }""",
            )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertNotNull(actualJson["personPseudoId"])
        assertDoesNotThrow { UUID.fromString(actualJson["personPseudoId"].asString()) }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gir feilmelding ved både identitetsnummer og aktørId i request`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val aktørId = person.aktørId
        val identitetsnummer = person.id.value

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "aktørId": "$aktørId", "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(400, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : 400,
              "title" : "Enten aktørId eller identitetsnummer må spesifiseres, ikke begge",
              "code" : "FOR_MANGE_INPUTPARAMETERE"
            }
            """.trimIndent(),
            actualJson,
        )

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
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ }""",
            )

        // Then:
        assertEquals(400, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : 400,
              "title" : "Enten aktørId eller identitetsnummer må spesifiseres, begge manglet",
              "code" : "MANGLER_INPUTPARAMETERE"
            }
            """.trimIndent(),
            actualJson,
        )

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Oppretter, klargjør og returnerer person når Spesialist ikke kjenner til den fra før av`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        every { integrationTestFixture.alleIdenterHenterMock.hentAlleIdenter(identitetsnummer) } returns
            listOf(
                AlleIdenterHenter.Ident(lagAktørId(), IdentType.AKTORID, true),
            )
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(identitetsnummer) } returns lagPersoninfo()

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "${identitetsnummer.value}" }""",
            )

        // Then:
        assertEquals(200, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gjentatte søk på samme person lykkes og publiserer ingenting`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id) } returns lagPersoninfo()

        // When:
        val responser =
            (1..3).map {
                integrationTestFixture.post(
                    "/api/personer/sok",
                    body = """{ "identitetsnummer": "$identitetsnummer" }""",
                )
            }

        // Then:
        responser.forEach { response ->
            assertEquals(200, response.status)
            val actualJson = response.bodyAsJsonNode
            assertNotNull(actualJson)
        }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gir feilmelding når personinfo ikke finnes i PDL, og publiserer ingenting`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id) } returns null

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(404, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertEquals("PERSONINFO_IKKE_FUNNET_I_PDL", actualJson["code"].asString())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Gir feilmelding når personinfo-oppslaget feiler, og publiserer ingenting`() {
        // Given:
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value
        every {
            integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id)
        } throws RuntimeException("Speed er nede")

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(502, response.status)
        val actualJson = response.bodyAsJsonNode
        assertNotNull(actualJson)
        assertEquals("PERSONINFO_OPPSLAG_FEILET", actualJson["code"].asString())

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }

    @Test
    fun `Saksbehandler uten tilgang blir avvist, og det gjøres ikke noe personinfo-oppslag`() {
        // Given:
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(
                TilgangSomMangler.StrengtFortroligAdresse,
            )
        val person = lagPerson(info = null).also(sessionContext.personRepository::lagre)
        val identitetsnummer = person.id.value

        // When:
        val response =
            integrationTestFixture.post(
                "/api/personer/sok",
                body = """{ "identitetsnummer": "$identitetsnummer" }""",
            )

        // Then:
        assertEquals(403, response.status)

        verify(exactly = 0) { integrationTestFixture.personinfoHenterMock.hentPersoninfo(any()) }

        // Sjekk publiserte meldinger
        integrationTestFixture.assertPubliserteBehovLister()
        integrationTestFixture.assertPubliserteKommandokjedeEndretEvents()
        integrationTestFixture.assertPubliserteSubsumsjoner()
        integrationTestFixture.assertIngenPubliserteUtgåendeHendelser()
    }
}
