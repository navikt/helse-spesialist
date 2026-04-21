package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class PgVedtakBegrunnelseDaoTest : AbstractDBIntegrationTest() {
    private val saksbehandler = opprettSaksbehandler()
    private val dao = daos.vedtakBegrunnelseDao

    @Test
    fun `lagrer og finner vedtaksbegrunnelse basert på oppgaveid`() {
        val oppgave = nyOppgaveForNyPerson()

        dao.lagreVedtakBegrunnelse(
            oppgaveId = oppgave.id.value,
            vedtakBegrunnelse =
                VedtakBegrunnelseFraDatabase(
                    type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                    tekst = "En individuell begrunelse",
                ),
            saksbehandlerOid = saksbehandler.id.value,
        )

        val lagretVedtakBegrunnelse = dao.finnVedtakBegrunnelse(oppgave.id.value)
        assertNotNull(lagretVedtakBegrunnelse)
        with(lagretVedtakBegrunnelse!!) {
            assertEquals(VedtakBegrunnelseTypeFraDatabase.AVSLAG, type)
            assertEquals("En individuell begrunelse", tekst)
        }
    }

    @Test
    fun `invaliderer vedtaksbegrunnelse`() {
        val oppgave = nyOppgaveForNyPerson()

        dao.lagreVedtakBegrunnelse(
            oppgaveId = oppgave.id.value,
            vedtakBegrunnelse =
                VedtakBegrunnelseFraDatabase(
                    type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                    tekst = "En individuell begrunelse",
                ),
            saksbehandlerOid = saksbehandler.id.value,
        )
        dao.invaliderVedtakBegrunnelse(oppgave.id.value)
        val lagretVedtakBegrunnelse = dao.finnVedtakBegrunnelse(oppgave.id.value)
        assertNull(lagretVedtakBegrunnelse)
    }

    @Test
    fun `finner alle vedtaksbegrunnelser for periode`() {
        val oppgave = nyOppgaveForNyPerson()

        dao.lagreVedtakBegrunnelse(
            oppgaveId = oppgave.id.value,
            vedtakBegrunnelse =
                VedtakBegrunnelseFraDatabase(
                    type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
                    tekst = "En individuell begrunelse",
                ),
            saksbehandlerOid = saksbehandler.id.value,
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = oppgave.id.value,
            vedtakBegrunnelse =
                VedtakBegrunnelseFraDatabase(
                    type = VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE,
                    tekst = "En individuell begrunelse delvis innvilgelse retter skrivefeil",
                ),
            saksbehandlerOid = saksbehandler.id.value,
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = oppgave.id.value,
            vedtakBegrunnelse =
                VedtakBegrunnelseFraDatabase(
                    type = VedtakBegrunnelseTypeFraDatabase.INNVILGELSE,
                    tekst = "En individuell begrunelse innvilgelse beholder skrivefeil",
                ),
            saksbehandlerOid = saksbehandler.id.value,
        )

        val lagredeAvslag = dao.finnAlleVedtakBegrunnelser(oppgave.vedtaksperiodeId.value, oppgave.utbetalingId)

        assertEquals(3, lagredeAvslag.size)
        with(lagredeAvslag[0]) {
            assertEquals(VedtakBegrunnelseTypeFraDatabase.INNVILGELSE, type)
            assertEquals("En individuell begrunelse innvilgelse beholder skrivefeil", begrunnelse)
            assertEquals(saksbehandler.ident.value, saksbehandlerIdent)
            assertFalse(invalidert)
        }
        with(lagredeAvslag[1]) {
            assertEquals(VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE, type)
            assertEquals("En individuell begrunelse delvis innvilgelse retter skrivefeil", begrunnelse)
            assertEquals(saksbehandler.ident.value, saksbehandlerIdent)
            assertFalse(invalidert)
        }
        with(lagredeAvslag[2]) {
            assertEquals(VedtakBegrunnelseTypeFraDatabase.AVSLAG, type)
            assertEquals("En individuell begrunelse", begrunnelse)
            assertEquals(saksbehandler.ident.value, saksbehandlerIdent)
            assertFalse(invalidert)
        }
    }
}
