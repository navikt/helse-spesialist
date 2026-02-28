package no.nav.helse.spesialist.api.rest.behandlinger.forsikring

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagForsikring
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetForsikringIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `Returnerer forsikring hvis den finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id, yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        val forsikring = lagForsikring()
        coEvery {
            integrationTestFixture.spiskammersetForsikringHenterMock.hentForsikringsinformasjon(SpleisBehandlingId(behandling.spleisBehandlingId!!.value))
        } returns ResultatAvForsikring.MottattForsikring(forsikring)


        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forsikring",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
               "eksisterer" : true,
               "forsikringInnhold" : {
                  "gjelderFraDag" : 17,
                  "dekningsgrad" : 100
               }
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `Returnerer ikke forsikring hvis den ikke finnes`() {
        // Given:
        val person = lagPerson().also(sessionContext.personRepository::lagre)
        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.id)
        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        sessionContext.behandlingRepository.lagre(behandling)

        coEvery {
            integrationTestFixture.spiskammersetForsikringHenterMock.hentForsikringsinformasjon(SpleisBehandlingId(behandling.spleisBehandlingId!!.value))
        } returns ResultatAvForsikring.IngenForsikring


        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/behandlinger/${behandling.spleisBehandlingId?.value}/forsikring",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.OK.value, response.status)
        assertJsonEquals(
            """
            {
              "eksisterer" : false,
              "forsikringInnhold" : null
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

    @Test
    fun `gir NotFound hvis behandlingen ikke finnes`() {
        // Given:
        val spleisBehandlingId = UUID.randomUUID()
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/behandlinger/$spleisBehandlingId/forsikring",
                saksbehandler = saksbehandler,
            )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
        assertJsonEquals(
            """
            {
              "type": "about:blank",
              "status": 404,
              "title": "Fant ikke behandling",
              "code": "BEHANDLING_IKKE_FUNNET" 
            }
            """.trimIndent(),
            response.bodyAsJsonNode!!,
        )
    }

}