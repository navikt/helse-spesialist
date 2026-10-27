package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.mediator.asLocalDateTime
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.application.testing.assertMindreEnnNSekunderSiden
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.sep
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetGraderteAndreYtelserIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `tom liste hvis det ikke er lagt inn noe for personen`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/graderte-andre-ytelser",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals("[]", response.bodyAsJsonNode!!)
    }

    @Test
    fun `kan hente en graderte-andre-ytelser for en person`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        val graderteAndreYtelser = opprettEnGradertAnnenYtelse(person, saksbehandler)
        graderteAndreYtelser.endre(
            dager = listOf(dag(13 sep 2024, svangerskapspenger = 50, sykdomsgrad = 50)),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = "et nytt notat",
        )
        integrationTestFixture.sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)
        val (førsteEvent, andreEvent) = graderteAndreYtelser.events

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/graderte-andre-ytelser",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertJsonEquals(
            """
            [
              {
                "yrkesaktivitet": "ARBEIDSTAKER",
                "organisasjonsnummer": "123456789",
                "versjon": "${andreEvent.versjon}",
                "dager": [
                  {
                    "dato": "2024-09-12",
                    "andreYtelserGrader": { "SVANGERSKAPSPENGER": 66 },
                    "sykdomsgrad": 34,
                    "arbeidsgrad": 0
                  },
                  {
                    "dato": "2024-09-13",
                    "andreYtelserGrader": { "SVANGERSKAPSPENGER": 50 },
                    "sykdomsgrad": 50,
                    "arbeidsgrad": 0
                  }
                ],
                "events": [
                  {
                    "versjon": "${førsteEvent.versjon}",
                    "utfortAvSaksbehandlerIdent": "${saksbehandler.ident.value}",
                    "notatTilBeslutter": "et notat til beslutter",
                    "dager": [
                      {
                        "dato": "2024-09-12",
                        "andreYtelserGrader": { "SVANGERSKAPSPENGER": 66 },
                        "sykdomsgrad": 34,
                        "arbeidsgrad": 0
                      },
                      {
                        "dato": "2024-09-13",
                        "andreYtelserGrader": { "SVANGERSKAPSPENGER": 66 },
                        "sykdomsgrad": 34,
                        "arbeidsgrad": 0
                      }
                    ]
                  },
                  {
                    "versjon": "${andreEvent.versjon}",
                    "utfortAvSaksbehandlerIdent": "${saksbehandler.ident.value}",
                    "notatTilBeslutter": "et nytt notat",
                    "dager": [
                      {
                        "dato": "2024-09-13",
                        "andreYtelserGrader": { "SVANGERSKAPSPENGER": 50 },
                        "sykdomsgrad": 50,
                        "arbeidsgrad": 0
                      }
                    ]
                  }
                ]
              }
            ]
            """.trimIndent(),
            body,
            "events.tidspunkt",
        )
        assertMindreEnnNSekunderSiden(15, body.first()["events"].first()["tidspunkt"].asLocalDateTime())
    }

    private fun opprettEnGradertAnnenYtelse(
        person: Person,
        saksbehandler: Saksbehandler,
    ): GraderteAndreYtelser =
        GraderteAndreYtelser.ny(
            id = GraderteAndreYtelserId(person.id, Yrkesaktivitetstype.ARBEIDSTAKER, "123456789"),
            dager = listOf(dag(12 sep 2024), dag(13 sep 2024)),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = "et notat til beslutter",
        )

    private fun dag(
        dato: LocalDate,
        svangerskapspenger: Int = 66,
        sykdomsgrad: Int = 34,
    ) = GraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = mapOf(GraderteAndreYtelserType.SVANGERSKAPSPENGER to svangerskapspenger),
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = 0,
    )
}
