package no.nav.helse.modell.stoppknapp

import DatabaseIntegrationTest
import no.nav.helse.modell.stoppautomatiskbehandling.Kilde
import no.nav.helse.modell.stoppautomatiskbehandling.Kilde.ISYFO
import no.nav.helse.modell.stoppautomatiskbehandling.Status
import no.nav.helse.modell.stoppautomatiskbehandling.Status.STOPP_AUTOMATIKK
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak.MEDISINSK_VILKAR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.SECONDS

internal class StansAutomatiskBehandlingDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `kan lagre`() {
        lagre()

        assertEquals(FNR, data<String>(FNR, "fødselsnummer"))
        assertEquals(STOPP_AUTOMATIKK, data<Status>(FNR, "status"))
        assertEquals(setOf(MEDISINSK_VILKAR, AKTIVITETSKRAV), data<Set<Årsak>>(FNR, "årsaker"))
        assertTrue(SECONDS.between(now(), data<LocalDateTime>(FNR, "opprettet")) < 5)
        assertEquals(ISYFO, data<Kilde>(FNR, "kilde"))
    }

    @Test
    fun `kan hente rader for gitt fødselsnummer`() {
        lagre(fødselsnummer = FNR)
        lagre(fødselsnummer = FNR)
        lagre(fødselsnummer = "01987654321")
        val rader = stansAutomatiskBehandlingDao.hent(FNR)

        assertEquals(2, rader.size)
    }

    private fun lagre(fødselsnummer: String = FNR) =
        stansAutomatiskBehandlingDao.lagre(
            fødselsnummer = fødselsnummer,
            status = STOPP_AUTOMATIKK,
            årsaker = setOf(MEDISINSK_VILKAR, AKTIVITETSKRAV),
            opprettet = now(),
            originalMelding = "{}",
            kilde = ISYFO,
        )

    private inline fun <reified T> data(
        fnr: String,
        kolonne: String,
    ): T =
        query(
            "select $kolonne from stans_automatisering where fødselsnummer = :fnr",
            "fnr" to fnr,
        ).single { row ->
            when (T::class) {
                Set::class -> row.array<String>(1).map { enumValueOf<Årsak>(it) }.toSet() as T
                Boolean::class -> row.boolean(1) as T
                LocalDateTime::class -> row.localDateTime(1) as T
                String::class -> row.string(1) as T
                Status::class -> enumValueOf<Status>(row.string(1)) as T
                Kilde::class -> enumValueOf<Kilde>(row.string(1)) as T

                else -> error("Mangler mapping for ${T::class}")
            }
        }!!
}
