package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.asLocalDateTime
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetBehandledeOppgaverBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `returnerer behandlede oppgaver for saksbehandler som gjør kallet`() {
        // given
        lagOppgave(
            SpleisBehandlingId(UUID.randomUUID()), UUID.randomUUID()
        ).also(sessionContext.oppgaveRepository::lagre)

        // when
        val response =
            integrationTestFixture.get("/api/behandlede-oppgaver?fom=${now().minusDays(1)}&tom=${now()}&sidestoerrelse=14&sidetall=1")

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }

        assertEquals(1, body["sidetall"].asInt())
        assertEquals(14, body["sidestoerrelse"].asInt())
        assertTrue { body["totaltAntall"].canConvertToLong() }
        assertEquals(1, body["elementer"].size())
        body["elementer"].forEach { element ->
            assertTrue { element["id"].canConvertToLong() }
            assertEquals(element["ferdigstiltTidspunkt"].asLocalDateTime(), LocalDateTime.of(2020, 2, 2, 12, 30))
            // .. flere asserts her en gang i fremtiden
        }
    }

}
