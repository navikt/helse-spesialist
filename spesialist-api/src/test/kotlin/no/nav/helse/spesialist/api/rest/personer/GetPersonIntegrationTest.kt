package no.nav.helse.spesialist.api.rest.personer

import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.AlleIdenterHenter
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
        val fødselsdato = LocalDate.now().minusYears(45)
        val person = lagPerson(
            fornavn = "Ola",
            mellomnavn = null,
            etternavn = "Nordmann",
            kjønn = Personinfo.Kjønn.Mann,
            fødselsdato = fødselsdato,
        ).also(personRepository::lagre)
        val personinfo = person.info!!
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns personinfo
        every { integrationTestFixture.alleIdenterHenterMock.hentAlleIdenter(person.id.value) } returns listOf(
            AlleIdenterHenter.Ident(person.id.value, AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT, true),
            AlleIdenterHenter.Ident(person.aktørId, AlleIdenterHenter.IdentType.AKTORID, true),
        )

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
              "mellomnavn": null,
              "etternavn": "Nordmann",
              "fødselsdato": "$fødselsdato",
              "dødsdato": null,
              "kjønn": "MANN",
              "adressebeskyttelse": "UGRADERT"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `henter person med mellomnavn som separat felt`() {
        // Given:
        val fødselsdato = LocalDate.now().minusYears(29).plusDays(1)
        val person = lagPerson(
            fornavn = "Kari",
            mellomnavn = "Midtre",
            etternavn = "Nordmann",
            kjønn = Personinfo.Kjønn.Kvinne,
            fødselsdato = fødselsdato,
        ).also(personRepository::lagre)
        val personinfo = person.info!!
        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns personinfo
        every { integrationTestFixture.alleIdenterHenterMock.hentAlleIdenter(person.id.value) } returns listOf(
            AlleIdenterHenter.Ident(person.id.value, AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT, true),
            AlleIdenterHenter.Ident(person.aktørId, AlleIdenterHenter.IdentType.AKTORID, true),
        )

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
              "fornavn": "Kari",
              "mellomnavn": "Midtre",
              "etternavn": "Nordmann",
              "fødselsdato": "$fødselsdato",
              "dødsdato": null,
              "kjønn": "KVINNE",
              "adressebeskyttelse": "UGRADERT"
            }
            """.trimIndent(),
            body,
        )
    }

    @Test
    fun `henter person med andre identitetsnumre`() {
        // Given:
        val aktørId = lagAktørId()
        val person = lagPerson(aktørId = aktørId).also(personRepository::lagre)
        val personinfo = person.info!!
        val andreIdent1 = lagPerson().id.value
        val andreIdent2 = lagPerson().id.value

        every { integrationTestFixture.personinfoHenterMock.hentPersoninfo(person.id.value) } returns personinfo
        every { integrationTestFixture.alleIdenterHenterMock.hentAlleIdenter(person.id.value) } returns listOf(
            AlleIdenterHenter.Ident(person.id.value, AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT, true),
            AlleIdenterHenter.Ident(andreIdent1, AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT, true),
            AlleIdenterHenter.Ident(andreIdent2, AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT, true),
            AlleIdenterHenter.Ident(aktørId, AlleIdenterHenter.IdentType.AKTORID, true),
        )

        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        val sorterteIdentitetsnumre = listOf(andreIdent1, andreIdent2).sorted()
        assertJsonEquals(
            sorterteIdentitetsnumre.joinToString(prefix = "[ ", separator = ", ", postfix = " ]") { "\"$it\"" },
            body["andreIdentitetsnumre"],
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

