package no.nav.helse.spesialist.api.rest.personer

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.spillkar.AutomatiskVurdering
import no.nav.helse.spesialist.application.spillkar.SamlingAvVurderteInngangsvilkår
import no.nav.helse.spesialist.application.spillkar.VurdertInngangsvilkår
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetVurderteInngangsvilkårForPersonBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `gir OK og tom liste når ingen vurderinger finnes`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        every {
            integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), skjæringstidspunkt)
        } returns emptyList()

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals("[]", response.bodyAsJsonNode!!)
    }

    @Test
    fun `gir OK med manuelt vurdert inngangsvilkår`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)
        val samlingId = UUID.randomUUID()
        val tidspunkt = LocalDateTime.of(2024, 1, 2, 10, 0, 0)

        every {
            integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), skjæringstidspunkt)
        } returns listOf(
            SamlingAvVurderteInngangsvilkår(
                samlingAvVurderteInngangsvilkårId = samlingId,
                versjon = 1,
                skjæringstidspunkt = skjæringstidspunkt,
                vurderteInngangsvilkår = listOf(
                    VurdertInngangsvilkår.ManueltVurdertInngangsvilkår(
                        vilkårskode = "8-2",
                        vurderingskode = "OPPFYLT",
                        tidspunkt = tidspunkt,
                        navident = "A123456",
                        begrunnelse = "Begrunnelse for vurdering",
                    ),
                ),
            ),
        )

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            [
              {
                "samlingAvVurderteInngangsvilkårId": "$samlingId",
                "versjon": 1,
                "skjæringstidspunkt": "2024-01-01",
                "vurderteInngangsvilkår": [
                  {
                    "vilkårskode": "8-2",
                    "vurderingskode": "OPPFYLT",
                    "manuellVurdering": {
                      "navident": "A123456",
                      "begrunnelse": "Begrunnelse for vurdering"
                    }
                  }
                ]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
            "vurderteInngangsvilkår.tidspunkt",
        )
    }

    @Test
    fun `gir OK med automatisk vurdert inngangsvilkår`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)
        val samlingId = UUID.randomUUID()
        val tidspunkt = LocalDateTime.of(2024, 1, 2, 10, 0, 0)

        every {
            integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), skjæringstidspunkt)
        } returns listOf(
            SamlingAvVurderteInngangsvilkår(
                samlingAvVurderteInngangsvilkårId = samlingId,
                versjon = 2,
                skjæringstidspunkt = skjæringstidspunkt,
                vurderteInngangsvilkår = listOf(
                    VurdertInngangsvilkår.AutomatiskVurdertInngangsvilkår(
                        vilkårskode = "8-4",
                        vurderingskode = null,
                        tidspunkt = tidspunkt,
                        automatiskVurdering = AutomatiskVurdering(
                            system = "spleis",
                            versjon = "1.0",
                            grunnlagsdata = mapOf("nøkkel" to "verdi"),
                        ),
                    ),
                ),
            ),
        )

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt")

        // Then:
        assertEquals(200, response.status)
        assertJsonEquals(
            """
            [
              {
                "samlingAvVurderteInngangsvilkårId": "$samlingId",
                "versjon": 2,
                "skjæringstidspunkt": "2024-01-01",
                "vurderteInngangsvilkår": [
                  {
                    "vilkårskode": "8-4",
                    "vurderingskode": null,
                    "automatiskVurdering": {
                      "system": "spleis",
                      "versjon": "1.0",
                      "grunnlagsdata": {
                        "nøkkel": "verdi"
                      }
                    }
                  }
                ]
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode!!,
            "vurderteInngangsvilkår.tidspunkt",
        )
    }

    @Test
    fun `gir 404 dersom personPseudoId ikke finnes`() {
        // Given:
        val ukjentPseudoId = UUID.randomUUID()
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        // When:
        val response = integrationTestFixture.get("/api/personer/$ukjentPseudoId/vurderte-inngangsvilkar/$skjæringstidspunkt")

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
        verify(exactly = 0) { integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), any()) }
    }

    @Test
    fun `gir 403 dersom saksbehandler ikke har tilgang til personen`() {
        // Given:
        val person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt")

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
        verify(exactly = 0) { integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), any()) }
    }

    @Test
    fun `gir 500 dersom kallet mot inngangsvilkår-tjenesten feiler`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        val skjæringstidspunkt = LocalDate.of(2024, 1, 1)

        every {
            integrationTestFixture.inngangsvilkårHenterMock.hentInngangsvilkår(any(), skjæringstidspunkt)
        } throws RuntimeException("Feil mot spillkar")

        // When:
        val response = integrationTestFixture.get("/api/personer/${personPseudoId.value}/vurderte-inngangsvilkar/$skjæringstidspunkt")

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
