package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer
import kotlin.test.Test
import kotlin.test.assertEquals

class PgOpptegnelseRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgOpptegnelseRepository(session)
    private val person = opprettPerson()

    @Test
    fun `kan opprette og hente opptegnelse`() {
        // GIVEN:
        val type = Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE
        val opptegnelse = Opptegnelse.ny(identitetsnummer = person.id, type)

        // WHEN:
        repository.lagre(opptegnelse)
        val hentedeOpptegnelser = repository.finnAlleForPersonEtter(
            opptegnelseId = Sekvensnummer(value = 0),
            personIdentitetsnummer = person.id
        )

        // THEN:
        assertEquals(person.id, hentedeOpptegnelser.first().identitetsnummer)
        assertEquals(type, hentedeOpptegnelser.first().type)
    }

    @Test
    fun `kan hente nyeste sekvensnummer`() {
        // GIVEN:
        val nyesteSekvensnummer1 = repository.finnNyesteSekvensnummer()
        repeat(10) {
            repository.lagre(Opptegnelse.ny(identitetsnummer = person.id, Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE))
        }

        // WHEN:
        val nyesteSekvensnummer2 = repository.finnNyesteSekvensnummer()

        // THEN:
        assertEquals(nyesteSekvensnummer1.value + 10, nyesteSekvensnummer2.value)
    }
}