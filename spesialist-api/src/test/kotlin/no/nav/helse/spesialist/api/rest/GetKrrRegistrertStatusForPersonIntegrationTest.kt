package no.nav.helse.spesialist.api.rest

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetKrrRegistrertStatusForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `henter registrert status for reservert person som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery {
            integrationTestFixture.krrRegistrertStatusHenterMock.hentForPerson(person.id.value)
        } returns KrrRegistrertStatusHenter.KrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsText
        assertNotNull(body)
        assertEquals("\"RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING\"", body)
    }

    @Test
    fun `henter status for ikke-reservert person som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery {
            integrationTestFixture.krrRegistrertStatusHenterMock.hentForPerson(person.id.value)
        } returns KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsText
        assertNotNull(body)
        assertEquals("\"IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING\"", body)
    }

    @Test
    fun `henter status for person som ikke er registrert i KRR som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery {
            integrationTestFixture.krrRegistrertStatusHenterMock.hentForPerson(person.id.value)
        } returns KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsText
        assertNotNull(body)
        assertEquals("\"IKKE_REGISTRERT_I_KRR\"", body)
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
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

        verify { integrationTestFixture.krrRegistrertStatusHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery {
            integrationTestFixture.krrRegistrertStatusHenterMock.hentForPerson(person.id.value)
        } returns KrrRegistrertStatusHenter.KrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
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

        verify { integrationTestFixture.krrRegistrertStatusHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om kallet mot KRR feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        coEvery {
            integrationTestFixture.krrRegistrertStatusHenterMock.hentForPerson(person.id.value)
        } throws IllegalStateException("Mocker feil mot KRR")

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/krr-registrert-status",
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
