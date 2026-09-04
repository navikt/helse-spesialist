package no.nav.helse.spesialist.api.rest

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Isolated
class ServerSentEventsIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val opptegnelseRepository = integrationTestFixture.sessionContext.opptegnelseRepository
    private val personPseudoIdProvider = integrationTestFixture.personPseudoIdProvider
    private val personRepository = integrationTestFixture.sessionContext.personRepository

    @Test
    fun `mottar server sent events som forventet`() {
        // Given:
        val identitetsnummer = lagPerson().also(personRepository::lagre).id
        val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = lagIdentitetsnummer(),
                type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            ),
        )
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = identitetsnummer,
                type = Opptegnelse.Type.REVURDERING_AVVIST,
            ),
        )

        integrationTestFixture.sse("/api/personer/${personPseudoId.value}/sse") { events ->
            delay(200.milliseconds)
            assertEquals(0, events.size)

            // When:
            opptegnelseRepository.lagre(
                Opptegnelse.ny(
                    identitetsnummer = identitetsnummer,
                    type = Opptegnelse.Type.UTBETALING_ANNULLERING_OK,
                ),
            )
            withTimeout(10.seconds) {
                while (events.isEmpty()) delay(100.milliseconds)
            }

            // Then:
            assertEquals(1, events.size)
            assertEquals("UTBETALING_ANNULLERING_OK", events.first().event)
            assertEquals("{}", events.first().data)
        }
    }
}
