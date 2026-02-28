package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PgÅpneGosysOppgaverDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()

    @Test
    fun `lagre og lese med åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, 1, false, LocalDateTime.now()),
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(person.id.value)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(1, antallÅpneOppgaver)
        }
    }

    @Test
    fun `lagre og lese uten åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, 0, false, LocalDateTime.now()),
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(person.id.value)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(0, antallÅpneOppgaver)
        }
    }

    @Test
    fun `oppdaterer bruker uten åpne oppgaver til å ha åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, 0, false, LocalDateTime.now()),
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(person.id.value)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(0, antallÅpneOppgaver)
        }

        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, 1, false, LocalDateTime.now()),
        )
        val erDigitalSvar2 = åpneGosysOppgaverDao.antallÅpneOppgaver(person.id.value)

        assertNotNull(erDigitalSvar2)
        if (erDigitalSvar2 != null) {
            assertEquals(1, erDigitalSvar2)
        }
    }

    @Test
    fun `lagre og lese ved feil i oppslag mot oppgave(gosys)`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, null, true, LocalDateTime.now()),
        )

        assertNull(åpneGosysOppgaverDao.antallÅpneOppgaver(person.id.value))
    }

    @Test
    fun `mangler åpne oppgaver-sjekk`() {
        val person = opprettPerson()
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(person.id.value, 1, false, LocalDateTime.now()),
        )
        assertNull(åpneGosysOppgaverDao.antallÅpneOppgaver(lagFødselsnummer()))
    }
}
