package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetListeOppgaverBehandlerIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `returnerer liste-oppgaver for saksbehandler som gjør kallet`() {
        // given
        lagPerson(id = Identitetsnummer.fraString("42"))
            .also(sessionContext.personRepository::lagre)

        lagOppgave(
            SpleisBehandlingId(UUID.randomUUID()), UUID.randomUUID()
        ).also(sessionContext.oppgaveRepository::lagre)

        // when
        val response =
            integrationTestFixture.get("/api/liste-oppgaver?sidestoerrelse=14&sidetall=1")

        // then
        assertEquals(HttpStatusCode.OK.value, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body) { "Body er tom eller ugyldig JSON" }

        assertEquals(1, body["sidetall"].asInt())
        assertEquals(14, body["sidestoerrelse"].asInt())
        assertTrue { body["totaltAntall"].canConvertToLong() }
        assertEquals(1, body["elementer"].size())
        body["elementer"].forEach { element ->
            assertEquals(Instant.parse(element["opprettetTidspunkt"].asText()), LocalDateTime.of(2020, 2, 2, 12, 30).toInstant(
                ZoneOffset.UTC))
            assertEquals(Instant.parse(element["behandlingOpprettetTidspunkt"].asText()), LocalDateTime.of(2020, 2, 2, 12, 30).toInstant(ZoneOffset.UTC))
            // .. flere asserts her en gang i fremtiden
        }

    }
}