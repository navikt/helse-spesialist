package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import java.util.UUID
import no.nav.helse.db.AvslagDao
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.mutation.Avslagsdata
import no.nav.helse.spesialist.api.graphql.mutation.Avslagshandling
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class AvslagDaoTest : DatabaseIntegrationTest() {

    private val nyDao = AvslagDao(dataSource)

    @Test
    fun `lagrer og finner avslag`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        val avslag = Avslag(handling = Avslagshandling.OPPRETT, data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse"))
        nyDao.lagreAvslag(OPPGAVE_ID, avslag.data!!, oid)

        val generasjonId = finnGenerasjonId(vedtaksperiodeId)

        val lagretAvslag = nyDao.finnAvslag(vedtaksperiodeId, generasjonId)
        assertNotNull(lagretAvslag)
    }

    @Test
    fun `invaliderer avslag`() {
        val oid = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson(vedtaksperiodeId = vedtaksperiodeId)
        nySaksbehandler(oid)
        val generasjonId = finnGenerasjonId(vedtaksperiodeId)
        val avslag = Avslag(handling = Avslagshandling.OPPRETT, data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse"))
        nyDao.lagreAvslag(OPPGAVE_ID, avslag.data!!, oid)
        nyDao.invaliderAvslag(OPPGAVE_ID)
        val lagretAvslag = nyDao.finnAvslag(VEDTAKSPERIODE, generasjonId)
        assertNull(lagretAvslag)
    }

    @Test
    fun `finner alle avslag for periode`() {
        val oid = UUID.randomUUID()
        nyPerson()
        nySaksbehandler(oid)
        val avslag = Avslag(handling = Avslagshandling.OPPRETT, data = Avslagsdata(Avslagstype.AVSLAG, "En individuell begrunelse"))
        val avslag2 = Avslag(handling = Avslagshandling.OPPRETT, data = Avslagsdata(Avslagstype.DELVIS_AVSLAG, "En individuell begrunelse delvis avslag retter skrivefeil"))
        nyDao.lagreAvslag(OPPGAVE_ID, avslag.data!!, oid)
        nyDao.lagreAvslag(OPPGAVE_ID, avslag2.data!!, oid)

        val lagredeAvslag: List<no.nav.helse.spesialist.api.graphql.schema.Avslag> =
            nyDao.finnAlleAvslag(VEDTAKSPERIODE, UTBETALING_ID).toList()

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
        saksbehandlerDao.opprettSaksbehandler(oid, "Navn Navnesen", "navn@navnesen.no", "Z999999")
    }

    private fun finnGenerasjonId(vedtaksperiodeId: UUID): Long =
        requireNotNull(
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf("SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ?", vedtaksperiodeId)
                        .map { it.long("id") }.asSingle
                )
            }
        )
}
