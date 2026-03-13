package no.nav.helse.spesialist.api.rest.oppgaver

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GetAntallOppgaverBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `returnerer 0 antall mine saker og mine saker på vent når saksbehandler ikke har noen tildelte oppgaver`() {
        // given
        val saksbehandler = lagSaksbehandler()

        // when
        val response = integrationTestFixture.get("/api/antall-oppgaver", saksbehandler = saksbehandler)

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }

        assertEquals(0, body["antallMineSaker"].asInt())
        assertEquals(0, body["antallMineSakerPåVent"].asInt())
    }

    @Test
    fun `returnerer antall mine saker for innlogget saksbehandler`() {
        // given
        val saksbehandler = lagSaksbehandler()
        val annenSaksbehandler = lagSaksbehandler()

        val saksbehandlerWrapper = SaksbehandlerWrapper(saksbehandler)
        val annenSaksbehandlerWrapper = SaksbehandlerWrapper(annenSaksbehandler)

        lagOppgave(SpleisBehandlingId(UUID.randomUUID()), UUID.randomUUID())
            .also { it.forsøkTildeling(saksbehandlerWrapper, emptySet()) }
            .also(sessionContext.oppgaveRepository::lagre)

        lagOppgave(SpleisBehandlingId(UUID.randomUUID()), UUID.randomUUID())
            .also { it.forsøkTildeling(saksbehandlerWrapper, emptySet()) }
            .also(sessionContext.oppgaveRepository::lagre)

        lagOppgave(SpleisBehandlingId(UUID.randomUUID()), UUID.randomUUID())
            .also {
                it.forsøkTildeling(annenSaksbehandlerWrapper, emptySet())
                it.leggPåVent(skalTildeles = true, saksbehandlerWrapper = saksbehandlerWrapper)
            }.also(sessionContext.oppgaveRepository::lagre)

        // when
        val response = integrationTestFixture.get("/api/antall-oppgaver", saksbehandler = saksbehandler)

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }

        assertEquals(2, body["antallMineSaker"].asInt())
        assertEquals(1, body["antallMineSakerPåVent"].asInt())
    }
}
