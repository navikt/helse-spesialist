package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

internal class PgStansAutomatiskLegacyBehandlingDaoTest : AbstractDBIntegrationTest() {
    private val fødselsnummer = lagFødselsnummer()

    @Test
    fun `kan lagre fra iSyfo`() {
        lagreFraISyfo(fødselsnummer)

        assertEquals(fødselsnummer, data<String>(fødselsnummer, "fødselsnummer"))
        assertEquals("STOPP_AUTOMATIKK", data<String>(fødselsnummer, "status"))
        assertEquals(setOf("MEDISINSK_VILKAR", "AKTIVITETSKRAV"), data<Set<String>>(fødselsnummer, "årsaker"))
        assertEquals("ISYFO", data<String>(fødselsnummer, "kilde"))
    }

    @Test
    fun `kan lagre fra speil`() {
        stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer)

        assertEquals(fødselsnummer, data<String>(fødselsnummer, "fødselsnummer"))
        assertEquals("NORMAL", data<String>(fødselsnummer, "status"))
        assertEquals(emptySet<String>(), data<Set<String>>(fødselsnummer, "årsaker"))
        assertEquals("SPEIL", data<String>(fødselsnummer, "kilde"))
    }

    @Test
    fun `kan hente rader for gitt fødselsnummer`() {
        lagreFraISyfo(fødselsnummer = fødselsnummer)
        lagreFraISyfo(fødselsnummer = fødselsnummer)
        lagreFraISyfo(fødselsnummer = "01987654321")
        val rader = stansAutomatiskBehandlingDao.hentFor(fødselsnummer)

        assertEquals(2, rader.size)
    }

    private fun lagreFraISyfo(fødselsnummer: String) =
        stansAutomatiskBehandlingDao.lagreFraISyfo(
            StansAutomatiskBehandlingMelding(
                id = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                kilde = "ISYFO",
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf(MEDISINSK_VILKAR, AKTIVITETSKRAV),
                opprettet = now(),
                originalMelding = "{}",
                json = "{}",
            ),
        )

    private inline fun <reified T> data(
        fnr: String,
        kolonne: String,
    ): T =
        dbQuery.single(
            "select $kolonne from stans_automatisering where fødselsnummer = :fnr",
            "fnr" to fnr,
        ) { row ->
            when (T::class) {
                Set::class -> row.array<String>(1).toSet() as T
                Boolean::class -> row.boolean(1) as T
                LocalDateTime::class -> row.localDateTime(1) as T
                String::class -> row.string(1) as T
                else -> error("Mangler mapping for ${T::class}")
            }
        }
}
