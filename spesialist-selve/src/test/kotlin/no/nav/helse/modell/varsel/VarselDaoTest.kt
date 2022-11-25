package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    private val varselDao = VarselDao(dataSource)

    @Test
    fun `lagre varsel`() {
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID())
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(1, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID())
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(2, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagrer ikke varsler dobbelt`() {
        val unikId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID())
        generasjon.lagreVarsel(unikId, "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjon.lagreVarsel(unikId, "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(1, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val definisjonId = UUID.randomUUID()
        varselDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(varselDao.definisjonFor(definisjonId))
    }

    @Test
    fun `lagre flere varseldefinisjoner`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        varselDao.lagreDefinisjon(definisjonId1, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        varselDao.lagreDefinisjon(definisjonId2, "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(varselDao.definisjonFor(definisjonId1))
        assertNotNull(varselDao.definisjonFor(definisjonId2))
    }

    @Test
    fun `lagrer ikke definisjon dobbelt opp`() {
        val definisjonsId = UUID.randomUUID()
        varselDao.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        varselDao.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())

        val definisjoner = alleDefinisjoner()
        assertEquals(1, definisjoner.size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID())
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(AKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID())
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        varselDao.oppdaterStatus(VEDTAKSPERIODE, "EN_KODE", INAKTIV, "EN_IDENT")
        assertEquals(INAKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val generasjonv1 = generasjonDao.opprettFor(v1, UUID.randomUUID())
        val generasjonv2 = generasjonDao.opprettFor(v2, UUID.randomUUID())
        generasjonv1.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjonv2.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        varselDao.oppdaterStatus(v1, "EN_KODE", INAKTIV, "EN_IDENT")
        assertEquals(INAKTIV, varselDao.finnVarselstatus(v1, "EN_KODE"))
        assertEquals(AKTIV, varselDao.finnVarselstatus(v2, "EN_KODE"))
    }

    @Test
    fun `status gir null dersom vi ikke finner varsel`() {
        assertNull(varselDao.finnVarselstatus(UUID.randomUUID(), "EN_KODE"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        val v1 = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val generasjon = generasjonDao.opprettFor(v1, UUID.randomUUID())
        generasjon.lagreVarsel(varselId, "EN_KODE", opprettet, varselDao::lagreVarsel)
        assertEquals(listOf(Varsel(varselId, "EN_KODE", opprettet, v1)), varselDao.alleVarslerFor(v1))
    }

    private fun alleDefinisjoner(): List<Varseldefinisjon> {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM api_varseldefinisjon;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    query
                ).map {
                    Varseldefinisjon(
                        id = it.uuid("unik_id"),
                        kode = it.string("kode"),
                        tittel = it.string("tittel"),
                        forklaring = it.string("forklaring"),
                        handling = it.string("handling"),
                        avviklet = it.boolean("avviklet"),
                        opprettet = it.localDateTime("opprettet")
                    )
                }.asList
            )
        }
    }
}