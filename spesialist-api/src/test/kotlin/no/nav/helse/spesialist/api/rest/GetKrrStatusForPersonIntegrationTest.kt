package no.nav.helse.spesialist.api.rest

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetKrrStatusForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @ParameterizedTest(name = "kanVarsles={0}, reservert={1}")
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `henter krr-status som forventet`(kanVarsles: Boolean, reservert: Boolean) {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery { integrationTestFixture.reservasjonshenterMock.hentForPerson(person.id.value) } returns Reservasjonshenter.ReservasjonDto(
            kanVarsles = kanVarsles,
            reservert = reservert
        )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-status",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
              {
                "kanVarsles" : $kanVarsles,
                "reservert" : $reservert
              }
        """.trimIndent(),
            body
        )
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-status",
            )

        // Then:
        assertEquals(404, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
              {
                "type" : "about:blank",
                "status" : 404,
                "title" : "Person ikke funnet",
                "code" : "PERSON_IKKE_FUNNET"
              }
        """.trimIndent(),
            body
        )

        verify { integrationTestFixture.reservasjonshenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery { integrationTestFixture.reservasjonshenterMock.hentForPerson(person.id.value) } returns Reservasjonshenter.ReservasjonDto(
            kanVarsles = true,
            reservert = false
        )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-status",
            )

        // Then:
        assertEquals(403, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
              {
                "type" : "about:blank",
                "status" : 403,
                "title" : "Mangler tilgang til person",
                "code" : "MANGLER_TILGANG_TIL_PERSON"
              }
        """.trimIndent(),
            body
        )

        verify { integrationTestFixture.reservasjonshenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om kallet mot KRR feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery { integrationTestFixture.reservasjonshenterMock.hentForPerson(person.id.value) } returns null

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-status",
            )

        // Then:
        assertEquals(500, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
              {
                "type" : "about:blank",
                "status" : 500,
                "title" : "Klarte ikke hente status fra Kontakt- og Reservasjonsregisteret",
                "code" : "FEIL_VED_VIDERE_KALL"
              }
        """.trimIndent(),
            body
        )
    }
}
