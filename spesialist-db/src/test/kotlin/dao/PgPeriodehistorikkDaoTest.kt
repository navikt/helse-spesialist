package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PgPeriodehistorikkDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagre periodehistorikk ved hjelp av oppgaveId`() {
        val saksbehandler = opprettSaksbehandler()
        val oppgave = nyOppgaveForNyPerson()

        val historikkinnslag = Historikkinnslag.fjernetFraPåVentInnslag(saksbehandler)

        periodehistorikkDao.lagreMedOppgaveId(historikkinnslag, oppgave.id.value)
        val result = periodehistorikkApiDao.finn(oppgave.utbetalingId)

        assertEquals(1, result.size)
    }
}
