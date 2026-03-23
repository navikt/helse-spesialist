package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.lagNotat
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import kotlin.test.Test
import kotlin.test.assertEquals

class GetNotatVedtaksperiodeIderForPersonBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    private fun lagPersonOgPseudoId() = lagPerson()
        .also(sessionContext.personRepository::lagre)
        .let { person -> person to sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id) }

    @Test
    fun `returnerer tom liste når person ikke har noen notater`() {
        val (_, pseudoId) = lagPersonOgPseudoId()

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals("[]", response.bodyAsJsonNode!!)
    }

    @Test
    fun `returnerer ett element med riktig vedtaksperiodeId og notattype`() {
        val (person, pseudoId) = lagPersonOgPseudoId()
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagNotat(type = NotatType.Generelt, vedtaksperiodeId = vedtaksperiode.id.value)
            .also(sessionContext.notatRepository::lagre)

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            [
              {
                "vedtaksperiodeId": "${vedtaksperiode.id.value}",
                "notattyper": ["Generelt"]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `returnerer begge notattyper når en vedtaksperiode har notater av to ulike typer`() {
        val (person, pseudoId) = lagPersonOgPseudoId()
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagNotat(type = NotatType.Generelt, vedtaksperiodeId = vedtaksperiode.id.value)
            .also(sessionContext.notatRepository::lagre)
        lagNotat(type = NotatType.OpphevStans, vedtaksperiodeId = vedtaksperiode.id.value)
            .also(sessionContext.notatRepository::lagre)

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            [
              {
                "vedtaksperiodeId": "${vedtaksperiode.id.value}",
                "notattyper": ["Generelt", "OpphevStans"]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `returnerer ett element per vedtaksperiode som har notater`() {
        val (person, pseudoId) = lagPersonOgPseudoId()
        val (vedtaksperiodeMedNotat, _) =
            listOf(
                lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre),
                lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre),
            ).sortedBy { it.id.value }
        lagNotat(type = NotatType.Generelt, vedtaksperiodeId = vedtaksperiodeMedNotat.id.value)
            .also(sessionContext.notatRepository::lagre)

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            [
              {
                "vedtaksperiodeId": "${vedtaksperiodeMedNotat.id.value}",
                "notattyper": ["Generelt"]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `returnerer ikke notater tilhørende en annen person`() {
        val (person, pseudoId) = lagPersonOgPseudoId()
        val (annenPerson, _) = lagPersonOgPseudoId()
        val vedtaksperiodeForPerson = lagVedtaksperiode(identitetsnummer = person.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        val vedtaksperiodeForAnnenPerson = lagVedtaksperiode(identitetsnummer = annenPerson.id)
            .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagNotat(type = NotatType.Generelt, vedtaksperiodeId = vedtaksperiodeForPerson.id.value)
            .also(sessionContext.notatRepository::lagre)
        lagNotat(type = NotatType.Generelt, vedtaksperiodeId = vedtaksperiodeForAnnenPerson.id.value)
            .also(sessionContext.notatRepository::lagre)

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            [
              {
                "vedtaksperiodeId": "${vedtaksperiodeForPerson.id.value}",
                "notattyper": ["Generelt"]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `returnerer 404 når personPseudoId ikke finnes`() {
        val ukjentPseudoId = PersonPseudoId.ny()

        val response = integrationTestFixture.get("/api/personer/${ukjentPseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.NotFound.value, response.status)
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
    }

    @Test
    fun `returnerer 403 når saksbehandler ikke har tilgang til person`() {
        val person =
            lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
                .also(sessionContext.personRepository::lagre)
        val pseudoId = sessionContext.personPseudoIdDao.nyPersonPseudoId(person.id)

        val response = integrationTestFixture.get("/api/personer/${pseudoId.value}/notat-vedtaksperiode-ider")

        assertEquals(HttpStatusCode.Forbidden.value, response.status)
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
    }
}
