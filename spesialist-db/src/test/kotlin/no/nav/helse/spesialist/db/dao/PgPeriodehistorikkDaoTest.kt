package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PgPeriodehistorikkDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagre periodehistorikk ved hjelp av oppgaveId`() {
        val saksbehandler = nyLegacySaksbehandler().let {
            SaksbehandlerDto(it.epostadresse, it.oid, it.navn, it.ident())
        }
        val oppgave = nyOppgaveForNyPerson()

        val historikkinnslag = Historikkinnslag.fjernetFraPåVentInnslag(saksbehandler)

        periodehistorikkDao.lagreMedOppgaveId(historikkinnslag, oppgave.id)
        val result = periodehistorikkApiDao.finn(oppgave.utbetalingId)

        assertEquals(1, result.size)
    }
}
