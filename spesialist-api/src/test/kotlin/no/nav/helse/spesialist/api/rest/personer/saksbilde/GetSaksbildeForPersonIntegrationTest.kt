package no.nav.helse.spesialist.api.rest.personer.saksbilde

import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangSomMangler
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetSaksbildeForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider
    private val personRepository = integrationTestFixture.sessionContext.personRepository

    @Test
    fun `gir feilmelding om personPseudoId ikke finnes`() {
        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${PersonPseudoId.ny().value}/saksbilde")

        // Then:
        assertFeilrespons(response, 404, "PersonPseudoId har utløpt (eller aldri eksistert)", "PERSON_PSEUDO_ID_IKKE_FUNNET")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om personen ikke finnes`() {
        // Given:
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(lagPerson().id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde")

        // Then:
        assertFeilrespons(response, 404, "Fant ikke data for person", "PERSON_IKKE_FUNNET")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om saksbehandler ikke har tilgang til personen`() {
        // Given:
        integrationTestFixture.populasjonstilgangskontrollProvider.resultat =
            TilgangskontrollResultat.ManglerTilgang(TilgangSomMangler.StrengtFortroligAdresse)
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde")

        // Then:
        assertFeilrespons(response, 403, "Mangler tilgang til person", "MANGLER_TILGANG_TIL_PERSON")
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `krever lesetilgang`() {
        // Given:
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/saksbilde",
                tilganger = emptySet<Tilgang>(),
            )

        // Then:
        assertEquals(403, response.status)
        verify { integrationTestFixture.snapshothenterMock wasNot Called }
    }

    @Test
    fun `gir feilmelding om snapshot ikke kan hentes`() {
        // Given:
        val person = lagPerson().also(personRepository::lagre)
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(person.id)
        every { integrationTestFixture.snapshothenterMock.hentPerson(person.id.value) } throws RuntimeException("Spleis er nede")

        // When:
        val response = integrationTestFixture.get(url = "/api/personer/${personPseudoId.value}/saksbilde")

        // Then:
        assertFeilrespons(response, 500, "Feil ved henting av snapshot for person", "SNAPSHOT_KUNNE_IKKE_HENTES")
    }

    private fun assertFeilrespons(
        response: IntegrationTestFixture.Response,
        status: Int,
        title: String,
        code: String,
    ) {
        assertEquals(status, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            {
              "type" : "about:blank",
              "status" : $status,
              "title" : "$title",
              "code" : "$code"
            }
            """.trimIndent(),
            body,
        )
    }
}
