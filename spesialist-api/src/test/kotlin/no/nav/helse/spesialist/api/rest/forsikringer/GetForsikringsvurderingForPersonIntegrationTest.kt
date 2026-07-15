package no.nav.helse.spesialist.api.rest.forsikringer

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetForsikringsvurderingForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Returnerer forsikring hvis den finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = person.id,
                harForsikring = true,
                dekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
               "eksisterer" : true,
               "forsikringInnhold" : {
                  "gjelderFraDag" : 17,
                  "dekningsgrad" : 100
               }
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `Returnerer ikke forsikring hvis den ikke finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = person.id,
                harForsikring = false,
                dekning = null,
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
              "eksisterer" : false,
              "forsikringInnhold" : null
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "PersonPseudoId har utløpt (eller aldri eksistert)",
              "code" : "PERSON_PSEUDO_ID_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis forsikringsvurderingen ikke finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns null

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "Forsikringsvurderingen ble ikke funnet",
              "code" : "FORSIKRINGSVURDERING_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis forsikringsvurderingen ikke har samme identitetsnummer`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val forsikringsvurderingId = ForsikringsvurderingId(UUID.randomUUID())
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        coEvery {
            integrationTestFixture.forsikringsvurderingHenterMock.hent(forsikringsvurderingId)
        } returns
            Forsikringsvurdering(
                identitetsnummer = lagIdentitetsnummer(),
                harForsikring = true,
                dekning =
                    Forsikringsvurdering.Dekning(
                        grad = 100,
                        fraDag = 17,
                    ),
            )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/forsikringsvurderinger/${forsikringsvurderingId.value}",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title" : "Forsikringsvurderingen ble ikke funnet",
              "code" : "FORSIKRINGSVURDERING_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
