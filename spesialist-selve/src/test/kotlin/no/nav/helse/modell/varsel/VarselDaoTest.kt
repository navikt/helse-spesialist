package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIKLET
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VarselDaoTest : DatabaseIntegrationTest() {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    @Test
    fun `lagre varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(1, alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(2, alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "KODE_33", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_33", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(AKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "KODE_33"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "KODE_24", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_24", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.oppdaterStatus(VEDTAKSPERIODE, generasjonId, "KODE_24", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "KODE_24"))
    }

    @Test
    fun `Kan nullstille definisjon-ref og ident i oppdatering av varsel`() {
        val generasjonId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
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
        definisjonDao.lagreDefinisjon(definisjonId, "KODE_337", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonIdv1, v1, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.opprettFor(generasjonIdv2, v2, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_337", LocalDateTime.now(), v1, generasjonIdv1)
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_337", LocalDateTime.now(), v2, generasjonIdv2)
        varselDao.oppdaterStatus(v1, generasjonIdv1, "KODE_337", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(v1, "KODE_337"))
        assertEquals(AKTIV, varselDao.finnVarselstatus(v2, "KODE_337"))
    }

    @Test
    fun `avvikling av varsel`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "KODE_99", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_99", LocalDateTime.now(), vedtaksperiodeId, generasjonId)
        varselDao.avvikleVarsel("KODE_99", definisjonId)
        assertEquals(AVVIKLET, varselDao.finnVarselstatus(vedtaksperiodeId, "KODE_99"))
    }

    @Test
    fun `avvikler ikke varsel hvis varsel ikke har status aktiv`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "KODE_42", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        varselDao.lagreVarsel(UUID.randomUUID(), "KODE_42", LocalDateTime.now(), vedtaksperiodeId, generasjonId)

        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, "KODE_42", VURDERT, "EN_IDENT", definisjonId)
        varselDao.avvikleVarsel("KODE_42", definisjonId)
        assertEquals(VURDERT, varselDao.finnVarselstatus(vedtaksperiodeId, "KODE_42"))
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, "KODE_42", GODKJENT, "EN_IDENT", definisjonId)
        varselDao.avvikleVarsel("KODE_42", definisjonId)
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "KODE_42"))
        varselDao.oppdaterStatus(vedtaksperiodeId, generasjonId, "KODE_42", INAKTIV, "EN_IDENT", definisjonId)
        varselDao.avvikleVarsel("KODE_42", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "KODE_42"))
    }

    @Test
    fun `kan ikke overskrive med samme status`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonIdv1, v1, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1, generasjonIdv1)
        varselDao.oppdaterStatus(v1, generasjonIdv1, "EN_KODE", VURDERT, "saksbehandler 1", definisjonId)
        assertThrows<IllegalStateException> {
            varselDao.oppdaterStatus(v1, generasjonIdv1, "EN_KODE", VURDERT, "saksbehandler 2", definisjonId)
        }
    }

    @Test
    fun `status gir null dersom vi ikke finner varsel`() {
        assertNull(varselDao.finnVarselstatus(UUID.randomUUID(), "KODE_808"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, v1, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        varselDao.lagreVarsel(varselId, "EN_KODE", LocalDateTime.now(), v1, generasjonId)
        assertEquals(listOf(Varsel(varselId, "EN_KODE", opprettet, v1)), alleVarslerFor(v1))
    }

    @Test
    fun `flytter aktive varsler til neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonIdv1,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        generasjonDao.opprettFor(
            generasjonIdv2,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
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
