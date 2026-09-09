package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjon
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgVarseldefinisjonRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.varseldefinisjonRepository

    @Test
    fun `finn gjeldende varseldefinisjon for gitt kode`() {
        // given
        val kode = "EN_KODE"
        val nyTittel = "Ny varseldefinisjon"

        val gammelDefinisjon = lagVarseldefinisjon(tittel = "Gammel varseldefinisjon", kode = kode)
        sessionContext.varseldefinisjonRepository.lagre(gammelDefinisjon)

        val nyDefinisjon = lagVarseldefinisjon(tittel = nyTittel, kode = kode)
        sessionContext.varseldefinisjonRepository.lagre(nyDefinisjon)

        // when
        val gjeldende = repository.finnGjeldendeForOrNull(kode)

        // then
        assertNotNull(gjeldende)
        assertEquals(kode, gjeldende.kode)
        assertEquals(nyTittel, nyTittel)
    }

    @Test
    fun `lagre ny varseldefinisjon`() {
        // given
        val varseldefinisjon =
            lagVarseldefinisjon(
                kode = "EN_KODE",
                tittel = "En tittel",
                forklaring = "En forklaring",
                handling = "En handling",
                avviklet = false,
            )

        // when
        repository.lagre(varseldefinisjon)

        // then
        val funnet = repository.finnGjeldendeForOrNull(varseldefinisjon.kode)
        assertNotNull(funnet)
        assertEquals(varseldefinisjon.id, funnet.id)
        assertEquals(varseldefinisjon.kode, funnet.kode)
        assertEquals(varseldefinisjon.tittel, funnet.tittel)
        assertEquals(varseldefinisjon.forklaring, funnet.forklaring)
        assertEquals(varseldefinisjon.handling, funnet.handling)
        assertEquals(varseldefinisjon.avviklet, funnet.avviklet)
        assertEquals(varseldefinisjon.opprettet.truncatedTo(ChronoUnit.MILLIS), funnet.opprettet.truncatedTo(ChronoUnit.MILLIS))
    }

    @Test
    fun `lagre varseldefinisjon med nullable felt`() {
        // given
        val varseldefinisjon =
            lagVarseldefinisjon(
                kode = "EN_KODE",
                forklaring = null,
                handling = null,
            )

        // when
        repository.lagre(varseldefinisjon)

        // then
        val funnet = repository.finnGjeldendeForOrNull(varseldefinisjon.kode)
        assertNotNull(funnet)
        assertNull(funnet.forklaring)
        assertNull(funnet.handling)
    }

    @Test
    fun `lagre oppdaterer eksisterende varseldefinisjon i stedet for å duplisere`() {
        // given
        val id = UUID.randomUUID()
        val original =
            lagVarseldefinisjon(
                id = id,
                kode = "EN_KODE",
                tittel = "Original tittel",
                forklaring = "Original forklaring",
                handling = "Original handling",
                avviklet = false,
            )
        repository.lagre(original)

        // when
        val oppdatert =
            lagVarseldefinisjon(
                id = id,
                kode = "EN_KODE",
                tittel = "Oppdatert tittel",
                forklaring = "Oppdatert forklaring",
                handling = "Oppdatert handling",
                avviklet = true,
            )
        repository.lagre(oppdatert)

        // then
        val funnet = repository.finnGjeldendeForOrNull("EN_KODE")
        assertNotNull(funnet)
        assertEquals(original.id, funnet.id)
        assertEquals("Oppdatert tittel", funnet.tittel)
        assertEquals("Oppdatert forklaring", funnet.forklaring)
        assertEquals("Oppdatert handling", funnet.handling)
        assertEquals(true, funnet.avviklet)
    }

    @Test
    fun `lagre to varseldefinisjoner med ulik kode påvirker ikke hverandre`() {
        // given
        val førsteVarseldefinisjon = lagVarseldefinisjon(kode = "FORSTE_KODE", tittel = "Første tittel")
        val andreVarseldefinisjon = lagVarseldefinisjon(kode = "ANDRE_KODE", tittel = "Andre tittel")

        // when
        repository.lagre(førsteVarseldefinisjon)
        repository.lagre(andreVarseldefinisjon)

        // then
        val funnetFørste = repository.finnGjeldendeForOrNull("FORSTE_KODE")
        val funnetAndre = repository.finnGjeldendeForOrNull("ANDRE_KODE")
        assertNotNull(funnetFørste)
        assertNotNull(funnetAndre)
        assertEquals(førsteVarseldefinisjon.id, funnetFørste.id)
        assertEquals("Første tittel", funnetFørste.tittel)
        assertEquals(andreVarseldefinisjon.id, funnetAndre.id)
        assertEquals("Andre tittel", funnetAndre.tittel)
    }
}
