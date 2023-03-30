package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    @Test
    fun `lagre varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(1, alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(2, alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(AKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.oppdaterStatus(VEDTAKSPERIODE, generasjonId, "EN_KODE", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `Kan nullstille definisjon-ref og ident i oppdatering av varsel`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(varselId, "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.oppdaterStatus(VEDTAKSPERIODE, generasjonId, "EN_KODE", INAKTIV, "ident", definisjonId)

        assertDefinisjonFor(varselId, forventetFinnes = true)
        assertIdentFor(varselId, forventetFinnes = true)

        varselDao.oppdaterStatus(VEDTAKSPERIODE, generasjonId, "EN_KODE", AKTIV, null, null)
        assertDefinisjonFor(varselId, forventetFinnes = false)
        assertIdentFor(varselId, forventetFinnes = false)
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonIdv1, v1, UUID.randomUUID(),null,null)
        generasjonDao.opprettFor(generasjonIdv2, v2, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1, generasjonIdv1)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2, generasjonIdv2)
        varselDao.oppdaterStatus(v1, generasjonIdv1, "EN_KODE", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(v1, "EN_KODE"))
        assertEquals(AKTIV, varselDao.finnVarselstatus(v2, "EN_KODE"))
    }

    @Test
    fun `status gir null dersom vi ikke finner varsel`() {
        assertNull(varselDao.finnVarselstatus(UUID.randomUUID(), "EN_KODE"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, v1, UUID.randomUUID(),null,null)
        varselDao.lagreVarsel(varselId, "EN_KODE", LocalDateTime.now(), v1, generasjonId)
        assertEquals(listOf(Varsel(varselId, "EN_KODE", opprettet, v1)), alleVarslerFor(v1))
    }

    @Test
    fun `flytter aktive varsler til neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonIdv1, vedtaksperiodeId, UUID.randomUUID(),null,null)
        generasjonDao.opprettFor(generasjonIdv2, vedtaksperiodeId, UUID.randomUUID(),null,null)
        val varselId = UUID.randomUUID()
        varselDao.lagreVarsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, generasjonIdv1)
        varselDao.oppdaterGenerasjon(varselId, generasjonIdv1, generasjonIdv2)

        assertVarslerFor(generasjonIdv1, 0)
        assertVarslerFor(generasjonIdv2, 1)
    }

    private fun assertDefinisjonFor(varselId: UUID, forventetFinnes: Boolean) {
        @Language("PostgreSQL")
        val query = "SELECT definisjon_ref FROM selve_varsel WHERE unik_id = ?"
        val definisjon = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId).map { it.longOrNull("definisjon_ref") }.asSingle)
        }
        if (!forventetFinnes) return assertNull(definisjon)
        assertNotNull(definisjon)
    }

    private fun assertIdentFor(varselId: UUID, forventetFinnes: Boolean) {
        @Language("PostgreSQL")
        val query = "SELECT status_endret_ident FROM selve_varsel WHERE unik_id = ?"
        val statusEndretIdent = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId).map { it.stringOrNull("status_endret_ident") }.asSingle)
        }
        if (!forventetFinnes) return assertNull(statusEndretIdent)
        assertNotNull(statusEndretIdent)
    }

    private fun alleVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
        @Language("PostgreSQL")
        val query = "SELECT unik_id,kode,opprettet FROM selve_varsel WHERE vedtaksperiode_id = ?;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(query, vedtaksperiodeId).map {
                    Varsel(
                        it.uuid("unik_id"),
                        it.string("kode"),
                        it.localDateTime("opprettet"),
                        vedtaksperiodeId
                    )
                }.asList
            )
        }
    }

    private fun assertVarslerFor(generasjonId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            """SELECT count(1) FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id)"""

        val antall = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("generasjon_id" to generasjonId)).map { it.int(1) }.asSingle
            )
        }

        assertEquals(antall, forventetAntall)
    }

}