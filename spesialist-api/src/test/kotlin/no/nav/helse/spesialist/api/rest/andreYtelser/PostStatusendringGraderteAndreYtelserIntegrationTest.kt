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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostStatusendringGraderteAndreYtelserIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `kan fjerne graderte andre ytelser`() {
        val saksbehandler = lagreSaksbehandler()
        val person = lagrePerson()
        val graderteAndreYtelser = opprettEnGradertAnnenYtelse(person, saksbehandler)
        sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)

        val response =
            integrationTestFixture.post(
                url = "/api/graderte-andre-ytelser/${graderteAndreYtelser.id.value}/fjern",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "notatTilBeslutter": "fjerner ytelsen"
                    }
                    """.trimIndent(),
            )

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals("""{"andreYtelserId":"${graderteAndreYtelser.id.value}"}""", response.bodyAsJsonNode!!)
        assertTrue(hentFraRepository(graderteAndreYtelser).fjernet)
    }

    @Test
    fun `kan gjenopprette fjernede graderte andre ytelser`() {
        val saksbehandler = lagreSaksbehandler()
        val person = lagrePerson()
        val graderteAndreYtelser =
            opprettEnGradertAnnenYtelse(person, saksbehandler).also {
                it.fjern(
                    saksbehandlerIdent = saksbehandler.ident,
                    notatTilBeslutter = "fjernet",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                )
            }
        sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)

        val response =
            integrationTestFixture.post(
                url = "/api/graderte-andre-ytelser/${graderteAndreYtelser.id.value}/gjenopprett",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "notatTilBeslutter": "gjenoppretter ytelsen"
                    }
                    """.trimIndent(),
            )

        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals("""{"andreYtelserId":"${graderteAndreYtelser.id.value}"}""", response.bodyAsJsonNode!!)
        assertFalse(hentFraRepository(graderteAndreYtelser).fjernet)
    }

    @Test
    fun `får conflict ved fjerning av allerede fjernede graderte andre ytelser`() {
        val saksbehandler = lagreSaksbehandler()
        val person = lagrePerson()
        val graderteAndreYtelser =
            opprettEnGradertAnnenYtelse(person, saksbehandler).also {
                it.fjern(
                    saksbehandlerIdent = saksbehandler.ident,
                    notatTilBeslutter = "fjernet",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                )
            }
        sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)

        val response =
            integrationTestFixture.post(
                url = "/api/graderte-andre-ytelser/${graderteAndreYtelser.id.value}/fjern",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "notatTilBeslutter": "fjerner ytelsen igjen"
                    }
                    """.trimIndent(),
            )

        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    @Test
    fun `får conflict ved endring av fjernede graderte andre ytelser`() {
        val saksbehandler = lagreSaksbehandler()
        val person = lagrePerson()
        val graderteAndreYtelser =
            opprettEnGradertAnnenYtelse(person, saksbehandler).also {
                it.fjern(
                    saksbehandlerIdent = saksbehandler.ident,
                    notatTilBeslutter = "fjernet",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                )
            }
        sessionContext.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)

        val response =
            integrationTestFixture.patch(
                url = "/api/graderte-andre-ytelser/${graderteAndreYtelser.id.value}",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "graderteAndreYtelserId": "${graderteAndreYtelser.id.value}",
                      "perioder": [
                        {
                          "fom": "2024-09-12",
                          "tom": "2024-10-02",
                          "grad": 66
                        }
                      ],
                      "andreYtelseType": "SVANGERSKAPSPENGER",
                      "notatTilBeslutter": "prøver å endre fjernet ytelse"
                    }
                    """.trimIndent(),
            )

        assertEquals(HttpStatusCode.Conflict.value, response.status)
    }

    private fun lagrePerson() = lagPerson().also(sessionContext.personRepository::lagre)

    private fun lagreSaksbehandler() = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

    private fun hentFraRepository(graderteAndreYtelser: GraderteAndreYtelser): GraderteAndreYtelser = requireNotNull(sessionContext.graderteAndreYtelserRepository.finn(graderteAndreYtelser.id))

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
