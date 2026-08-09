package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals

// finnNyesteSekvensnummer() leser MAX(sekvensnummer) på tvers av hele opptegnelse-tabellen, mens
// testene i denne modulen kjøres parallelt mot samme database (se junit-platform.properties).
// Uten isolering kan opptegnelser fra andre tester bli talt med og gjøre testene flaky.
@Isolated
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
        val hentedeOpptegnelser =
            repository.finnAlleForPersonEtter(
                opptegnelseId = Sekvensnummer(value = 0),
                personIdentitetsnummer = person.id,
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
