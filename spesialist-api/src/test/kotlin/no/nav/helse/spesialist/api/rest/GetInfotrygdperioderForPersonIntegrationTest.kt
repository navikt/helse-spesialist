package no.nav.helse.spesialist.api.rest

import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Infotrygdperiode
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetInfotrygdperioderForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext
    private val personPseudoIdDao = sessionContext.personPseudoIdDao
    private val personRepository = sessionContext.personRepository
    private val vedtaksperiodeRepository = sessionContext.vedtaksperiodeRepository
    private val behandlingRepository = sessionContext.behandlingRepository

    @Test
    fun `henter infotrygdperioder som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val behandlingFom = LocalDate.of(2022, 1, 1)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = behandlingFom)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        val expectedFom = behandlingFom.minusYears(3)
        val periode =
            lagInfotrygdperiode(
                fom = LocalDate.of(2021, 6, 1),
                tom = LocalDate.of(2021, 6, 30),
                type = "PERM",
            )
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, expectedFom) } returns listOf(periode)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            [
              {
                "fom": "2021-06-01",
                "tom": "2021-06-30"
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `filtrerer bort SANKSJON, TILBAKEFØRT og UKJENT perioder`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val behandlingFom = LocalDate.of(2022, 1, 1)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = behandlingFom)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        val expectedFom = behandlingFom.minusYears(3)
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, expectedFom) } returns
            listOf(
                lagInfotrygdperiode(type = "UTBETALING"),
                lagInfotrygdperiode(type = "SANKSJON"),
                lagInfotrygdperiode(type = "TILBAKEFØRT"),
                lagInfotrygdperiode(type = "UKJENT"),
            )

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode!!
        assertEquals(1, body.size())
    }

    @Test
    fun `returnerer tom liste når personen ikke har noen kjente vedtaksperioder`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("[]", response.bodyAsJsonNode!!)
        verify { integrationTestFixture.infotrygdperiodeHenterMock wasNot Called }
    }

    @Test
    fun `bruker første kjente dag minus tre år som fom`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val tidligsteBehandlingFom = LocalDate.of(2020, 3, 15)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val tidligsteBehandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = tidligsteBehandlingFom)
        val senereBehandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = tidligsteBehandlingFom.plusMonths(6))
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(tidligsteBehandling)
        behandlingRepository.lagre(senereBehandling)

        val forventetFom = tidligsteBehandlingFom.minusYears(3)
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, forventetFom) } returns emptyList()

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        verify { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, forventetFom) }
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = PersonPseudoId.ny()

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

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
        verify { integrationTestFixture.infotrygdperiodeHenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

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
        verify { integrationTestFixture.infotrygdperiodeHenterMock wasNot Called }
    }

    @Test
    fun `sorterer perioder stigende på fom`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val behandlingFom = LocalDate.of(2022, 1, 1)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = behandlingFom)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        val expectedFom = behandlingFom.minusYears(3)
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, expectedFom) } returns
            listOf(
                lagInfotrygdperiode(fom = LocalDate.of(2021, 3, 1), tom = LocalDate.of(2021, 3, 15)),
                lagInfotrygdperiode(fom = LocalDate.of(2021, 1, 1), tom = LocalDate.of(2021, 1, 31)),
                lagInfotrygdperiode(fom = LocalDate.of(2021, 5, 1), tom = LocalDate.of(2021, 5, 31)),
            )

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode!!
        assertEquals("2021-01-01", body[0]["fom"].asText())
        assertEquals("2021-03-01", body[1]["fom"].asText())
        assertEquals("2021-05-01", body[2]["fom"].asText())
    }

    @Test
    fun `slår sammen sammenhengende perioder til én`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val behandlingFom = LocalDate.of(2022, 1, 1)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = behandlingFom)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        val expectedFom = behandlingFom.minusYears(3)
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, expectedFom) } returns
            listOf(
                lagInfotrygdperiode(fom = LocalDate.of(2021, 1, 1), tom = LocalDate.of(2021, 1, 15)),
                lagInfotrygdperiode(fom = LocalDate.of(2021, 1, 16), tom = LocalDate.of(2021, 1, 31)),
            )

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode!!
        assertEquals(1, body.size())
        assertEquals("2021-01-01", body[0]["fom"].asText())
        assertEquals("2021-01-31", body[0]["tom"].asText())
    }

    @Test
    fun `beholder separate perioder når det er gap mellom dem`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val behandlingFom = LocalDate.of(2022, 1, 1)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, fom = behandlingFom)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        val expectedFom = behandlingFom.minusYears(3)
        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(person.id, expectedFom) } returns
            listOf(
                lagInfotrygdperiode(fom = LocalDate.of(2021, 1, 1), tom = LocalDate.of(2021, 1, 10)),
                lagInfotrygdperiode(fom = LocalDate.of(2021, 1, 15), tom = LocalDate.of(2021, 1, 31)),
            )

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

        // Then:
        assertEquals(200, response.status)
        assertEquals(2, response.bodyAsJsonNode!!.size())
    }

    @Test
    fun `gir feilmelding om kallet mot infotrygd feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        vedtaksperiodeRepository.lagre(vedtaksperiode)
        behandlingRepository.lagre(behandling)

        every { integrationTestFixture.infotrygdperiodeHenterMock.hentFor(any(), any()) } throws
            IllegalStateException("Mocker feil mot infotrygd")

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/infotrygdperioder")

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

    private fun lagInfotrygdperiode(
        fom: LocalDate = LocalDate.of(2021, 1, 1),
        tom: LocalDate = LocalDate.of(2021, 1, 31),
        type: String = "PERM",
    ) = Infotrygdperiode(
        fom = fom,
        tom = tom,
        type = type,
    )
}
