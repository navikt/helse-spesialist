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
    private val dao = VarselDao(dataSource)

    @Test
    fun `lagre varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertEquals(1, dao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        dao.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertEquals(2, dao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val definisjonId = UUID.randomUUID()
        dao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(dao.definisjonFor(definisjonId))
    }

    @Test
    fun `lagre flere varseldefinisjoner`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        dao.lagreDefinisjon(definisjonId1, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        dao.lagreDefinisjon(definisjonId2, "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(dao.definisjonFor(definisjonId1))
        assertNotNull(dao.definisjonFor(definisjonId2))
    }

    @Test
    fun `lagrer ikke definisjon dobbelt opp`() {
        val definisjonsId = UUID.randomUUID()
        dao.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        dao.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())

        val definisjoner = alleDefinisjoner()
        assertEquals(1, definisjoner.size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertEquals(AKTIV, dao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        dao.oppdaterStatus(VEDTAKSPERIODE, "EN_KODE", INAKTIV, "EN_IDENT")
        assertEquals(INAKTIV, dao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1)
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2)
        dao.oppdaterStatus(v1, "EN_KODE", INAKTIV, "EN_IDENT")
        assertEquals(INAKTIV, dao.finnVarselstatus(v1, "EN_KODE"))
        assertEquals(AKTIV, dao.finnVarselstatus(v2, "EN_KODE"))
    }

    @Test
    fun `status gir null dersom vi ikke finner varsel`() {
        assertNull(dao.finnVarselstatus(UUID.randomUUID(), "EN_KODE"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        val v1 = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        dao.lagreVarsel(varselId, "EN_KODE", opprettet, v1)
        assertEquals(Varsel(varselId, "EN_KODE", opprettet, v1), dao.alleVarslerFor(v1).first())
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