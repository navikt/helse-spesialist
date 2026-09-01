package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.okt
import no.nav.helse.spesialist.domain.testfixtures.sep
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import kotlin.random.Random
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

        val enGradertAnnenYtelse = opprettEnGradertAnnenYtelse(person, saksbehandler)
        integrationTestFixture.sessionContext.graderteAndreYtelserRepository.lagre(enGradertAnnenYtelse)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/graderte-andre-ytelser",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertNotNull(response.bodyAsJsonNode)
        assertJsonEquals(
            """
            [
              {
                "andreYtelserId": "${enGradertAnnenYtelse.id.value}",
                "perioder": [
                  {
                    "fom": "2024-09-12",
                    "tom": "2024-10-02",
                    "grad": 66
                  }
                ],
                "andreYtelserType": "SVANGERSKAPSPENGER",
                "fjernet": false
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode,
        )
    }

    @Test
    fun `kan hente en fjernet graderte-andre-ytelser for en person`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val personPseudoId = integrationTestFixture.personPseudoIdProvider.nyPersonPseudoId(person.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        val enFjernetGradertAnnenYtelse =
            opprettEnGradertAnnenYtelse(person, saksbehandler).also {
                it.fjern(
                    saksbehandlerIdent = saksbehandler.ident,
                    notatTilBeslutter = "fjerner ytelsen",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                )
            }
        integrationTestFixture.sessionContext.graderteAndreYtelserRepository.lagre(enFjernetGradertAnnenYtelse)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/graderte-andre-ytelser",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertNotNull(response.bodyAsJsonNode)
        assertJsonEquals(
            """
            [
              {
                "andreYtelserId": "${enFjernetGradertAnnenYtelse.id.value}",
                "perioder": [
                  {
                    "fom": "2024-09-12",
                    "tom": "2024-10-02",
                    "grad": 66
                  }
                ],
                "andreYtelserType": "SVANGERSKAPSPENGER",
                "fjernet": true
              }
            ]
            """.trimIndent(),
            response.bodyAsJsonNode,
        )
    }

    private fun opprettEnGradertAnnenYtelse(
        person: Person,
        saksbehandler: Saksbehandler,
    ): GraderteAndreYtelser =
        GraderteAndreYtelser.ny(
            identitetsnummer = person.id,
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            graderteAndreYtelserPerioder =
                listOf(
                    GraderteAndreYtelserPeriode(
                        periode = (12 sep 2024) tilOgMed (2 okt 2024),
                        grad = 66,
                    ),
                ),
            graderteAndreYtelserType = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
        )
}
