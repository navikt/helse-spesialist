package no.nav.helse.db

import DatabaseIntegrationTest
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PgPeriodehistorikkDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `finner all periodehistorikk for en person`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettSaksbehandler()
        opprettVedtaksperiode()
        opprettOppgave()

        val saksbehandler = SaksbehandlerDto(SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_IDENT)
        val historikkinnslag = HistorikkinnslagDto.fjernetFraPåVentInnslag(saksbehandler)

        historikkinnslagRepository.lagreMedOppgaveId(historikkinnslag, oppgaveId)
        val result = periodehistorikkApiDao.hentForPerson(FNR)

        assertEquals(1, result.size)
        assertNotNull(result[UTBETALING_ID])
    }
}
