package no.nav.helse.spesialist.api.rest.personer

import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PostVurderteInngangsvilkårForPersonBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `gir 204 og sender vurderinger videre`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt",
                body =
                    """
                    {
                      "versjon": 1,
                      "vurderinger": [
                        {
                          "vilkårskode": "8-2",
                          "vurderingskode": "OPPFYLT",
                          "begrunnelse": "Begrunnelse for vurdering"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

        // Then:
        assertEquals(204, response.status)
        verify(exactly = 1) {
            integrationTestFixture.inngangsvilkårInnsenderMock.sendManuelleVurderinger(
                match { vurderinger ->
                    vurderinger.personidentifikator == person.id.value &&
                        vurderinger.skjæringstidspunkt == skjæringstidspunkt &&
                        vurderinger.versjon == 1 &&
                        vurderinger.vurderinger.size == 1 &&
                        vurderinger.vurderinger[0].vilkårskode == "8-2" &&
                        vurderinger.vurderinger[0].vurderingskode == "OPPFYLT" &&
                        vurderinger.vurderinger[0].begrunnelse == "Begrunnelse for vurdering"
                }
            )
        }
    }

    @Test
    fun `gir 404 dersom personPseudoId ikke finnes`() {
        // Given:
        val ukjentPseudoId = UUID.randomUUID()
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/personer/$ukjentPseudoId/vurderte-inngangsvilkar/$skjæringstidspunkt",
                body =
                    """
                    {
                      "versjon": 1,
                      "vurderinger": [
                        {
                          "vilkårskode": "8-2",
                          "vurderingskode": "OPPFYLT",
                          "begrunnelse": "Begrunnelse"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

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
        verify(exactly = 0) { integrationTestFixture.inngangsvilkårInnsenderMock.sendManuelleVurderinger(any()) }
    }

    @Test
    fun `gir 403 dersom saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt",
                body =
                    """
                    {
                      "versjon": 1,
                      "vurderinger": [
                        {
                          "vilkårskode": "8-2",
                          "vurderingskode": "OPPFYLT",
                          "begrunnelse": "Begrunnelse"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

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
        verify(exactly = 0) { integrationTestFixture.inngangsvilkårInnsenderMock.sendManuelleVurderinger(any()) }
    }

    @Test
    fun `gir 500 dersom kallet mot inngangsvilkår-tjenesten feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        io.mockk.every {
            integrationTestFixture.inngangsvilkårInnsenderMock.sendManuelleVurderinger(any())
        } throws RuntimeException("Feil mot spillkar")

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt",
                body =
                    """
                    {
                      "versjon": 1,
                      "vurderinger": [
                        {
                          "vilkårskode": "8-2",
                          "vurderingskode": "OPPFYLT",
                          "begrunnelse": "Begrunnelse"
                        }
                      ]
                    }
                    """.trimIndent(),
            )

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
