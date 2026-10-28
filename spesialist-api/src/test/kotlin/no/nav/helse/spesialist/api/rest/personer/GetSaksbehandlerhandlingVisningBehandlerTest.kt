package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.OMSORGSPENGER
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType.SVANGERSKAPSPENGER
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId
import no.nav.helse.spesialist.domain.testfixtures.sep
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSaksbehandlerhandlingVisningBehandlerTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionContext
    private val saksbehandler = lagSaksbehandler()

    @Test
    fun `viser notat og endrede dager gruppert for endring av graderte andre ytelser`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                id = GraderteAndreYtelserId(person.id, Yrkesaktivitetstype.ARBEIDSTAKER, "123456789"),
                dager = listOf(dag(23 sep 2024), dag(24 sep 2024), dag(25 sep 2024)),
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = "første notat",
            )
        val event =
            graderteAndreYtelser.endre(
                dager =
                    listOf(
                        dag(23 sep 2024, andreYtelserGrader = mapOf(SVANGERSKAPSPENGER to 30), sykdomsgrad = 70),
                        dag(24 sep 2024, andreYtelserGrader = mapOf(SVANGERSKAPSPENGER to 30), sykdomsgrad = 70),
                        dag(25 sep 2024, andreYtelserGrader = mapOf(OMSORGSPENGER to 50)),
                        dag(27 sep 2024, andreYtelserGrader = mapOf(SVANGERSKAPSPENGER to 20), sykdomsgrad = 80),
                        dag(28 sep 2024, andreYtelserGrader = mapOf(SVANGERSKAPSPENGER to 20), sykdomsgrad = 80),
                    ),
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = "endret etter ny dokumentasjon",
            )!!
        sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)
        sessionContext.saksbehandlerhandlingRepository.lagre(
            lagHandling(person.id, handlingId = event.versjon, handlingType = HandlingType.GRADERTE_ANDRE_YTELSER_ENDRET),
        )

        // When:
        val response = hent(personPseudoId.value, event.versjon)

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
              "felter": [
                {
                  "type": "Tekst",
                  "tittel": "Begrunnelse",
                  "tekst": "endret etter ny dokumentasjon"
                },
                {
                  "type": "Endringer",
                  "tittel": "Dager endret",
                  "grupper": [
                    {
                      "rader": [
                        { "navn": "Dato", "fra": null, "til": "23.09.2024 - 24.09.2024" },
                        { "navn": "Svangerskapspenger", "fra": "50 %", "til": "30 %" },
                        { "navn": "Sykdomsgrad", "fra": "50 %", "til": "70 %" },
                        { "navn": "Arbeidsgrad", "fra": null, "til": "0 %" }
                      ]
                    },
                    {
                      "rader": [
                        { "navn": "Dato", "fra": null, "til": "25.09.2024" },
                        { "navn": "Svangerskapspenger", "fra": "50 %", "til": "0 %" },
                        { "navn": "Omsorgspenger", "fra": "0 %", "til": "50 %" },
                        { "navn": "Sykdomsgrad", "fra": null, "til": "50 %" },
                        { "navn": "Arbeidsgrad", "fra": null, "til": "0 %" }
                      ]
                    },
                    {
                      "rader": [
                        { "navn": "Dato", "fra": null, "til": "27.09.2024 - 28.09.2024" },
                        { "navn": "Svangerskapspenger", "fra": null, "til": "20 %" },
                        { "navn": "Sykdomsgrad", "fra": null, "til": "80 %" },
                        { "navn": "Arbeidsgrad", "fra": null, "til": "0 %" }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `returnerer 404 når handlingen ikke finnes for personen`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val annenPerson = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val handlingForAnnenPerson =
            lagHandling(annenPerson.id, handlingType = HandlingType.GRADERTE_ANDRE_YTELSER_ENDRET)
                .also(sessionContext.saksbehandlerhandlingRepository::lagre)

        // When:
        val response = hent(personPseudoId.value, handlingForAnnenPerson.handlingId)

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertEquals("HANDLING_IKKE_FUNNET", response.bodyAsJsonNode?.get("code")?.asString())
    }

    @Test
    fun `returnerer 404 for handlingstyper uten visning`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val handling =
            lagHandling(person.id, handlingType = HandlingType.MANUELT_VURDERT_OPPTJENING)
                .also(sessionContext.saksbehandlerhandlingRepository::lagre)

        // When:
        val response = hent(personPseudoId.value, handling.handlingId)

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertEquals("VISNING_IKKE_FUNNET", response.bodyAsJsonNode?.get("code")?.asString())
    }

    @Test
    fun `returnerer 404 for ukjent PersonPseudoId`() {
        // When:
        val response = hent(UUID.randomUUID(), UUID.randomUUID())

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }

    private fun hent(
        pseudoId: UUID,
        handlingId: UUID,
    ) = integrationTestFixture.get(
        "/api/personer/$pseudoId/saksbehandlerhandlinger/$handlingId/visning",
        saksbehandler = saksbehandler,
        tilganger = setOf(Tilgang.Les),
    )

    private fun lagHandling(
        identitetsnummer: Identitetsnummer,
        handlingType: HandlingType,
        handlingId: UUID = UUID.randomUUID(),
    ) = Saksbehandlerhandling.ny(
        id = SaksbehandlerhandlingId(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        handlingId = handlingId,
        handlingType = handlingType,
        saksbehandlerIdent = saksbehandler.ident.value,
        tidspunkt = Instant.now(),
        skjæringstidspunkt = null,
        behandlingId = null,
        totrinnsvurderingId = null,
    )

    private fun dag(
        dato: LocalDate,
        andreYtelserGrader: Map<GraderteAndreYtelserType, Int> = mapOf(SVANGERSKAPSPENGER to 50),
        sykdomsgrad: Int = 50,
    ) = GraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = andreYtelserGrader,
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = 0,
    )
}
