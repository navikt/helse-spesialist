package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.sep
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import kotlin.test.Test
import kotlin.test.assertEquals

class PostGraderteAndreYtelserIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `kan poste en graderte-andre-ytelser for en person`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id).also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            fom = 21 sep (2024),
            tom = 30 sep (2024),
        ).also(sessionContext.behandlingRepository::lagre)
        val saksbehandler = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)

        // When:
        val response =
            integrationTestFixture.post(
                url = "/api/graderte-andre-ytelser",
                saksbehandler = saksbehandler,
                body =
                    """
                    {
                      "fodselsnummer": "${person.id.value}",
                      "perioder": [
                        {
                          "fom": "2024-09-13",
                          "tom": "2024-10-03",
                          "grad": 67
                        }
                      ],
                      "andreYtelseType": "SVANGERSKAPSPENGER",
                      "notatTilBeslutter": "notat om en gradert annen ytelse"
                    }
                    """.trimIndent(),
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)

        val lagret = sessionContext.graderteAndreYtelserRepository.finnAlleForIdentitetsnummer(person.id).first()
        assertEquals(lagret.id.value, response.bodyAsJsonNode?.get("andreYtelserId")?.asUUID())
    }
}
