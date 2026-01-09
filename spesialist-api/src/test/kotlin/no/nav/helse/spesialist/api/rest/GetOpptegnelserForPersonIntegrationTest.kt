package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.IntegrationTestFixture
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class GetOpptegnelserForPersonIntegrationTest {
    private val integrationTestFixture = IntegrationTestFixture()
    private val opptegnelseRepository = integrationTestFixture.sessionFactory.sessionContext.opptegnelseRepository
    private val personPseudoIdDao = integrationTestFixture.sessionFactory.sessionContext.personPseudoIdDao
    private val personRepository = integrationTestFixture.sessionFactory.sessionContext.personRepository

    @Test
    fun `henter opptegnelser som forventet`() {
        // Given:
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = lagIdentitetsnummer(),
                type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE,
            )
        )

        val person = lagPerson()
        personRepository.lagre(person)
        val personPseudoId = personPseudoIdDao.nyPersonPseudoId(person.id)
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = person.id,
                type = Opptegnelse.Type.PERSON_KLAR_TIL_BEHANDLING,
            )
        )
        opptegnelseRepository.lagre(
            Opptegnelse.ny(
                identitetsnummer = person.id,
                type = Opptegnelse.Type.UTBETALING_ANNULLERING_OK
            )
        )

        // When:
        val response =
            integrationTestFixture.get(
                url = "/api/personer/${personPseudoId.value}/opptegnelser?etterSekvensnummer=1",
            )

        // Then:
        assertEquals(200, response.status)
        val body = response.bodyAsJsonNode
        assertNotNull(body)
        assertEquals(2, body.size())
        assertJsonEquals("""
            [
              {
                "sekvensnummer": 3,
                "type": "UTBETALING_ANNULLERING_OK"
              },
              {
                "sekvensnummer": 2,
                "type": "PERSON_KLAR_TIL_BEHANDLING"
              }
            ]
        """.trimIndent(), body)
    }
}
