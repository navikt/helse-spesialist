package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtakBegrunnelseDaoTest : DatabaseIntegrationTest() {

    private val dao = VedtakBegrunnelseDao(dataSource)

    @Test
    fun `lagrer og finner vedtaksbegrunnelse`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
            begrunnelse = "En individuell begrunelse",
            saksbehandlerOid = oid
        )

        val generasjonId = finnGenerasjonId(vedtaksperiodeId)

        val lagretVedtakBegrunnelse = dao.finnVedtakBegrunnelse(vedtaksperiodeId, generasjonId)
        assertNotNull(lagretVedtakBegrunnelse)
    }

    @Test
    fun `lagrer og finner vedtaksbegrunnelse i transaksjon`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
            begrunnelse = "En individuell begrunelse",
            saksbehandlerOid = oid
        )

        val generasjonId = finnGenerasjonId(vedtaksperiodeId)

        val lagretVedtakBegrunnelse = VedtakBegrunnelseDao(dataSource).finnVedtakBegrunnelse(vedtaksperiodeId, generasjonId)
        assertNotNull(lagretVedtakBegrunnelse)
    }

    @Test
    fun `invaliderer vedtaksbegrunnelse`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        val generasjonId = finnGenerasjonId(vedtaksperiodeId)
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
            begrunnelse = "En individuell begrunelse",
            saksbehandlerOid = oid
        )
        dao.invaliderVedtakBegrunnelse(OPPGAVE_ID)
        val lagretVedtakBegrunnelse = dao.finnVedtakBegrunnelse(VEDTAKSPERIODE, generasjonId)
        assertNull(lagretVedtakBegrunnelse)
    }

    @Test
    fun `finner alle vedtaksbegrunnelser for periode`() {
        val oid = UUID.randomUUID()
        nyPerson()
        nySaksbehandler(oid)
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = VedtakBegrunnelseTypeFraDatabase.AVSLAG,
            begrunnelse = "En individuell begrunelse",
            saksbehandlerOid = oid
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = VedtakBegrunnelseTypeFraDatabase.DELVIS_AVSLAG,
            begrunnelse = "En individuell begrunelse delvis avslag retter skrivefeil",
            saksbehandlerOid = oid
        )

        val lagredeAvslag = dao.finnAlleVedtakBegrunnelser(VEDTAKSPERIODE, UTBETALING_ID)

        assertEquals(2, lagredeAvslag.size)
        assertEquals(VedtakBegrunnelseTypeFraDatabase.AVSLAG, lagredeAvslag.last().type)
        assertEquals(VedtakBegrunnelseTypeFraDatabase.DELVIS_AVSLAG, lagredeAvslag.first().type)
        assertEquals("En individuell begrunelse", lagredeAvslag.last().begrunnelse)
        assertEquals("En individuell begrunelse delvis avslag retter skrivefeil", lagredeAvslag.first().begrunnelse)
        assertEquals(SAKSBEHANDLER_IDENT, lagredeAvslag.last().saksbehandlerIdent)
        assertEquals(SAKSBEHANDLER_IDENT, lagredeAvslag.first().saksbehandlerIdent)
        assertFalse(lagredeAvslag.last().invalidert)
        assertFalse(lagredeAvslag.first().invalidert)
    }


    private fun nySaksbehandler(oid: UUID = UUID.randomUUID()) {
        saksbehandlerDao.opprettEllerOppdater(oid, "Navn Navnesen", "navn@navnesen.no", "Z999999")
    }

    private fun finnGenerasjonId(vedtaksperiodeId: UUID): Long =
        requireNotNull(
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf("SELECT id FROM behandling WHERE vedtaksperiode_id = ?", vedtaksperiodeId)
                        .map { it.long("id") }.asSingle
                )
            }
        )
}
