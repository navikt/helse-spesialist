package no.nav.helse.spesialist.api.rest

import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Enhet
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import kotlin.test.Test
import kotlin.test.assertEquals

class GetBehandlendeEnhetForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `henter behandlende enhet som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        every {
            integrationTestFixture.behandlendeEnhetHenterMock.hentFor(person.id)
        } returns Enhet(enhetNr = "1337", navn = "Nav Gamle Oslo", type = "LOKAL")

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/behandlende-enhet")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            {
              "enhetNr": "1337",
              "navn": "Nav Gamle Oslo",
              "type": "LOKAL"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir 404 om behandlende enhet ikke finnes`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        every {
            integrationTestFixture.behandlendeEnhetHenterMock.hentFor(person.id)
        } returns null

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/behandlende-enhet")

        // Then:
        assertEquals(404, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ingen behandlende enhet for personen",
              "code": "BEHANDLENDE_ENHET_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/behandlende-enhet")

        // Then:
        assertEquals(404, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "PersonPseudoId har utløpt (eller aldri eksistert)",
              "code": "PERSON_PSEUDO_ID_IKKE_FUNNET"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )

        verify { integrationTestFixture.behandlendeEnhetHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/behandlende-enhet")

        // Then:
        assertEquals(403, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 403,
              "title": "Mangler tilgang til person",
              "code": "MANGLER_TILGANG_TIL_PERSON"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )

        verify { integrationTestFixture.behandlendeEnhetHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om kallet mot sparkel-norg feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        every {
            integrationTestFixture.behandlendeEnhetHenterMock.hentFor(person.id)
        } throws IllegalStateException("Mocker feil mot sparkel-norg")

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/behandlende-enhet")

        // Then:
        assertEquals(500, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 500,
              "title": "Internal Server Error"
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }
}
