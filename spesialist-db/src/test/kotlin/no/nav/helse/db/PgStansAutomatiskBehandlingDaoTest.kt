package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

internal class PgStansAutomatiskBehandlingDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `kan lagre fra iSyfo`() {
        lagreFraISyfo()

        assertEquals(FNR, data<String>(FNR, "fødselsnummer"))
        assertEquals("STOPP_AUTOMATIKK", data<String>(FNR, "status"))
        assertEquals(setOf("MEDISINSK_VILKAR", "AKTIVITETSKRAV"), data<Set<String>>(FNR, "årsaker"))
        assertEquals("ISYFO", data<String>(FNR, "kilde"))
    }

    @Test
    fun `kan lagre fra speil`() {
        stansAutomatiskBehandlingDao.lagreFraSpeil(FNR)

        assertEquals(FNR, data<String>(FNR, "fødselsnummer"))
        assertEquals("NORMAL", data<String>(FNR, "status"))
        assertEquals(emptySet<String>(), data<Set<String>>(FNR, "årsaker"))
        assertEquals("SPEIL", data<String>(FNR, "kilde"))
    }

    @Test
    fun `kan hente rader for gitt fødselsnummer`() {
        lagreFraISyfo(fødselsnummer = FNR)
        lagreFraISyfo(fødselsnummer = FNR)
        lagreFraISyfo(fødselsnummer = "01987654321")
        val rader = stansAutomatiskBehandlingDao.hentFor(FNR)

        assertEquals(2, rader.size)
    }

    private fun lagreFraISyfo(fødselsnummer: String = FNR) =
        stansAutomatiskBehandlingDao.lagreFraISyfo(
            StansAutomatiskBehandlingMelding(
                id = UUID.randomUUID(),
                fødselsnummer = fødselsnummer,
                kilde = "ISYFO",
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf(MEDISINSK_VILKAR, AKTIVITETSKRAV),
                opprettet = now(),
                originalMelding = "{}",
                json = "{}"
            ),
        )

    private inline fun <reified T> data(
        fnr: String,
        kolonne: String,
    ): T = dbQuery.single(
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
    }!!
}
