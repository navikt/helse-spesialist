package no.nav.helse.spesialist.api.rest

import kotlinx.coroutines.delay
import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals

@Isolated
class SseOpptegnelserForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val opptegnelseRepository = integrationTestFixture.sessionFactory.sessionContext.opptegnelseRepository
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `mottar opptegnelser som forventet`() {
        // Given:
        val identitetsnummer = lagPerson().also(personRepository::lagre).id
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(identitetsnummer)

        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = lagIdentitetsnummer(),
                type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            ),
        )
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = identitetsnummer,
                type = Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING,
            ),
        )

        integrationTestFixture.sse("/api/personer/${personPseudoId.value}/opptegnelser-stream") { events ->
            delay(200)
            assertEquals(0, events.size)

            // When:
            opptegnelseRepository.lagre(
                Opptegnelse.ny(
                    identitetsnummer = identitetsnummer,
                    type = Opptegnelse.Type.UTBETALING_ANNULLERING_OK,
                ),
            )
            delay(200)

            // Then:
            assertEquals(1, events.size)
            assertEquals("""{"sekvensnummer":3,"type":"UTBETALING_ANNULLERING_OK"}""", events.first().data)
        }
    }
}
