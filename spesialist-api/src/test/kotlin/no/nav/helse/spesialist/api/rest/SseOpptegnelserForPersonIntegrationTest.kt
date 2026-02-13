package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
class SseOpptegnelserForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val opptegnelseRepository = integrationTestFixture.sessionFactory.sessionContext.opptegnelseRepository
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `mottar opptegnelser som forventet`() {
        // Given:
        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)

        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = lagIdentitetsnummer(),
                type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            ),
        )
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = person.id,
                type = Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING,
            ),
        )

        val result =
            integrationTestFixture.sse(
                url = "/api/personer/${personPseudoId.value}/opptegnelser-stream",
                nyeOpptegnelserEtterEtablertForbindelse = {
                    opptegnelseRepository.lagre(
                        Opptegnelse.ny(
                            identitetsnummer = person.id,
                            type = Opptegnelse.Type.UTBETALING_ANNULLERING_OK,
                        ),
                    )
                },
            )
        assertJsonEquals(
            """
            {
              "sekvensnummer": 3,
              "type": "UTBETALING_ANNULLERING_OK"
            }
            """.trimIndent(),
            result,
        )
    }
}
