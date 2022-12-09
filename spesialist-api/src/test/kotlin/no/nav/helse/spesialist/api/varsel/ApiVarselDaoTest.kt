package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ApiVarselDaoTest: DatabaseIntegrationTest() {

    private val apiVarselDao = ApiVarselDao(dataSource)

    @Test
    fun `Tom liste ved manglende varsler`() {
        assertTrue(apiVarselDao.finnVarslerFor(PERIODE.id, UUID.randomUUID()).isEmpty())
    }

    @Test
    fun `Finner varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val varsler = apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `Finner siste definisjon for varsler når definisjonsRef ikke finnes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        opprettVarseldefinisjon("EN_NY_TITTEL")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val forventetVarsel = Varsel(varselId, "EN_NY_TITTEL", null, null, null)

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    @Test
    fun `Finner riktig definisjon for varsler når varselet har definisjonRef`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon("EN_TITTEL")
        opprettVarseldefinisjon("EN_NY_TITTEL")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, definisjonRef = definisjonRef)
        val forventetVarsel = Varsel(varselId, "EN_TITTEL", null, null, null)

        assertEquals(forventetVarsel, apiVarselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).single())
    }

    private fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        låstTidspunkt: LocalDateTime? = null
    ): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse) 
            VALUES (?, ?, ?, ?, ?, ?)
        """
        return requireNotNull(session.run(queryOf(query, vedtaksperiodeId, generasjonId, utbetalingId, UUID.randomUUID(), låstTidspunkt, UUID.randomUUID()).asUpdateAndReturnGeneratedKey))
    }

    private fun nyttVarsel(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID(), kode: String = "EN_KODE", generasjonRef: Long, definisjonRef: Long? = null) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_varsel(unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        session.run(queryOf(query, id, kode, vedtaksperiodeId, generasjonRef, definisjonRef, LocalDateTime.now(), null, null).asExecute)
    }
}