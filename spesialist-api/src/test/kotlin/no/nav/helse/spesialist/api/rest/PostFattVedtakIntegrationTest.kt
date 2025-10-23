package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PostFattVedtakIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val saksbehandlerRepository = integrationTestFixture.sessionFactory.sessionContext.saksbehandlerRepository
    private val behandlingRepository = integrationTestFixture.sessionFactory.sessionContext.behandlingRepository
    private val egenansattDao = integrationTestFixture.sessionFactory.sessionContext.egenAnsattDao

    @Test
    fun `gir 404 hvis behandlingen ikke finnes`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.NotFound.value, response.status)
    }

    @Test
    fun `gir forbidden hvis saksbehandler ikke har tilgang til personen`() {
        // Given:
        val behandlingId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val behandling = Behandling.fraLagring(
            id = SpleisBehandlingId(behandlingId),
            tags = emptySet(),
            fødselsnummer = fødselsnummer
        )
        val saksbehandler = Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = "Navn Navnesen",
            epost = "navn@navnesen.no",
            ident = "L112233"
        )
        saksbehandlerRepository.lagre(saksbehandler)
        behandlingRepository.lagre(behandling)
        egenansattDao.lagre(fødselsnummer, true, LocalDateTime.now())

        // When:
        val response = integrationTestFixture.post(
            url = "/api/vedtak/$behandlingId/fatt",
            body = "{}",
            saksbehandler = saksbehandler,
        )

        // Then:
        assertEquals(HttpStatusCode.Forbidden.value, response.status)
    }

}
