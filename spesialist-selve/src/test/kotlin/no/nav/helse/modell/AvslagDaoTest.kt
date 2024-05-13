package no.nav.helse.modell

import DatabaseIntegrationTest
import java.util.UUID
import no.nav.helse.db.AvslagDao
import no.nav.helse.spesialist.api.graphql.mutation.Avslag
import no.nav.helse.spesialist.api.graphql.mutation.Avslagstype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class AvslagDaoTest : DatabaseIntegrationTest() {

    private val nyDao = AvslagDao(dataSource)

    @Test
    fun `lagrer og finner avslag`() {
        val oid = UUID.randomUUID()
        nyPerson()
        nySaksbehandler(oid)
        val avslag = Avslag(Avslagstype.AVSLAG, "En individuell begrunelse")
        nyDao.lagreAvslag(OPPGAVE_ID, 1L, avslag,  oid)

        val lagretAvslag = nyDao.finnAvslag(VEDTAKSPERIODE, 1L)
        assertNotNull(lagretAvslag)
//        assertEquals(Avslagstype.AVSLAG, lagretAvslag?.type)
    }

    @Test
    fun `finner alle Avslag for periode`() {
        val oid = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        nyPerson(generasjonId = generasjonId)
        nySaksbehandler(oid)
        val avslag = Avslag(Avslagstype.AVSLAG, "En individuell begrunelse")
        val avslag2 = Avslag(Avslagstype.DELVIS_AVSLAG, "En individuell begrunelse delvis avslag retter skrivefeil")
        nyDao.lagreAvslag(OPPGAVE_ID, 1L, avslag,  oid)
        nyDao.lagreAvslag(OPPGAVE_ID, 1L, avslag2,  oid)

        val lagredeAvslag: List<no.nav.helse.spesialist.api.graphql.schema.Avslag> = nyDao.finnAlleAvslag(VEDTAKSPERIODE, generasjonId).toList()

        assertEquals(2, lagredeAvslag.size)
        assertEquals(Avslagstype.AVSLAG, lagredeAvslag.last().type)
        assertEquals(Avslagstype.DELVIS_AVSLAG, lagredeAvslag.first().type)
        assertEquals("En individuell begrunelse", lagredeAvslag.last().begrunnelse)
        assertEquals("En individuell begrunelse delvis avslag retter skrivefeil", lagredeAvslag.first().begrunnelse)
        assertEquals(SAKSBEHANDLER_IDENT, lagredeAvslag.last().saksbehandlerIdent)
        assertEquals(SAKSBEHANDLER_IDENT, lagredeAvslag.first().saksbehandlerIdent)
    }


    private fun nySaksbehandler(oid: UUID = UUID.randomUUID()) {
        saksbehandlerDao.opprettSaksbehandler(oid, "Navn Navnesen", "navn@navnesen.no", "Z999999")
    }
}
