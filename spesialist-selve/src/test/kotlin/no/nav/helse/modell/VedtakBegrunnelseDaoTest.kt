package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.mutation.Avslagsdata
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
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
            type = "AVSLAG",
            begrunnelse = "En individuell begrunelse",
            saksbehandlerOid = oid
        )

        val generasjonId = finnGenerasjonId(vedtaksperiodeId)

        val lagretAvslag = dao.finnVedtakBegrunnelse(vedtaksperiodeId, generasjonId)
        assertNotNull(lagretAvslag)
    }

    @Test
    fun `lagrer og finner avslag i transaksjon`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        val avslag = Avslag(
            handling = Avslagshandling.OPPRETT,
            data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse")
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = avslag.data!!.type.toString(),
            begrunnelse = avslag.data!!.begrunnelse,
            saksbehandlerOid = oid
        )

        val generasjonId = finnGenerasjonId(vedtaksperiodeId)

        val lagretAvslag = VedtakBegrunnelseDao(dataSource).finnVedtakBegrunnelse(vedtaksperiodeId, generasjonId)
        assertNotNull(lagretAvslag)
    }

    @Test
    fun `invaliderer avslag`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        val generasjonId = finnGenerasjonId(vedtaksperiodeId)
        val avslag = Avslag(
            handling = Avslagshandling.OPPRETT,
            data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse")
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = avslag.data!!.type.toString(),
            begrunnelse = avslag.data!!.begrunnelse,
            saksbehandlerOid = oid
        )
        dao.invaliderAvslag(OPPGAVE_ID)
        val lagretAvslag = dao.finnVedtakBegrunnelse(VEDTAKSPERIODE, generasjonId)
        assertNull(lagretAvslag)
    }

    @Test
    fun `finner alle avslag for periode`() {
        val oid = UUID.randomUUID()
        nyPerson()
        nySaksbehandler(oid)
        val avslag = Avslag(
            handling = Avslagshandling.OPPRETT,
            data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse")
        )
        val avslag2 = Avslag(
            handling = Avslagshandling.OPPRETT,
            data = Avslagsdata(Avslagstype.DELVIS_AVSLAG, "En individuell begrunelse delvis avslag retter skrivefeil")
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = avslag.data!!.type.toString(),
            begrunnelse = avslag.data!!.begrunnelse,
            saksbehandlerOid = oid
        )
        dao.lagreVedtakBegrunnelse(
            oppgaveId = OPPGAVE_ID,
            type = avslag2.data!!.type.toString(),
            begrunnelse = avslag2.data!!.begrunnelse,
            saksbehandlerOid = oid
        )

        val lagredeAvslag: List<no.nav.helse.spesialist.api.graphql.schema.Avslag> =
            dao.finnAlleAvslag(VEDTAKSPERIODE, UTBETALING_ID).toList()

        assertEquals(2, lagredeAvslag.size)
        assertEquals(Avslagstype.AVSLAG, lagredeAvslag.last().type)
        assertEquals(Avslagstype.DELVIS_AVSLAG, lagredeAvslag.first().type)
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
