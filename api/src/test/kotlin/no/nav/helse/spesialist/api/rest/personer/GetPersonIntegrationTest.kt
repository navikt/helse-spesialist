package no.nav.helse.spesialist.api.rest.personer

import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `henter person uten mellomnavn som forventet`() {
        // Given:
        val person = lagPerson(
            fornavn = "Ola",
            mellomnavn = null,
            etternavn = "Nordmann",
            kjønn = Personinfo.Kjønn.Mann,
            fødselsdato = LocalDate.now().minusYears(45),
            enhet = 1234
        ).also(personRepository::lagre)
        val personinfo = person.info!!
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns personinfo

        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "identitetsnummer": "${person.id.value}",
              "andreIdentitetsnumre": [],
              "aktørId": "${person.aktørId}",
              "fornavn": "Ola",
              "etternavn": "Nordmann",
              "kjønn": "MANN",
              "alder": 45,
              "boenhet": { "enhetNr": "1234" }
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `henter person med mellomnavn konkatenert i fornavn`() {
        // Given:
        val person = lagPerson(
            fornavn = "Kari",
            mellomnavn = "Midtre",
            etternavn = "Nordmann",
            kjønn = Personinfo.Kjønn.Kvinne,
            fødselsdato = LocalDate.now().minusYears(29).plusDays(1),
            enhet = 5678
        ).also(personRepository::lagre)
        val personinfo = person.info!!
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns personinfo

        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "identitetsnummer": "${person.id.value}",
              "andreIdentitetsnumre": [],
              "aktørId": "${person.aktørId}",
              "fornavn": "Kari Midtre",
              "etternavn": "Nordmann",
              "kjønn": "KVINNE",
              "alder": 28,
              "boenhet": { "enhetNr": "5678" }
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `henter person med andre identitetsnumre`() {
        // Given:
        val aktørId = lagAktørId()
        val person1 = lagPerson(aktørId = aktørId).also(personRepository::lagre)
        val personinfo = person1.info!!

        val person2 = lagPerson(aktørId = aktørId).also(personRepository::lagre)
        val person3 = lagPerson(aktørId = aktørId).also(personRepository::lagre)

        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person1.id.value) } returns personinfo

        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person1.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        val sorterteIdentitetsnumre = listOf(person2.id.value, person3.id.value).sorted()
        assertJsonEquals(
            sorterteIdentitetsnumre.joinToString(prefix = "[ ", separator = ", ", postfix = " ]") { "\"$it\"" },
            body["andreIdentitetsnumre"]
        )
    }

    @Test
    fun `gir feilmelding om personPseudoId ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(404, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : 404,
              "title" : "PersonPseudoId har utløpt (eller aldri eksistert)",
              "code" : "PERSON_PSEUDO_ID_IKKE_FUNNET"
            }
            """.trimIndent(),
            body,
        )

        verify { integrationTestFixture.personinfoHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

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
            body,
        )

        verify { integrationTestFixture.personinfoHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om personinfo ikke finnes i Speed`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns null

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(404, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : 404,
              "title" : "Fant ikke data for person",
              "code" : "PERSON_IKKE_FUNNET"
            }
            """.trimIndent(),
            body,
        )
    }
}
