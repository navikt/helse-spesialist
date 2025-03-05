package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PgPeriodehistorikkDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagre periodehistorikk ved hjelp av oppgaveId`() {
        val behandlingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettSaksbehandler()
        opprettVedtaksperiode(spleisBehandlingId = behandlingId)
        opprettOppgave(behandlingId = behandlingId)

        val saksbehandler = SaksbehandlerDto(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT)
        val historikkinnslag = Historikkinnslag.fjernetFraPåVentInnslag(saksbehandler)

        periodehistorikkDao.lagreMedOppgaveId(historikkinnslag, oppgaveId)
        val result = periodehistorikkApiDao.finn(UTBETALING_ID)

        assertEquals(1, result.size)
    }
}
