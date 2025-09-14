package no.nav.helse.modell.stoppautomatiskbehandling

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StansAutomatiskBehandlinghåndtererImplTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(stansAutomatiskBehandlingDao)

    @Test
    fun `Kan stanses på nytt etter stans er opphevet`() {
        val fødselsnummer = lagFødselsnummer()
        every { stansAutomatiskBehandlingDao.hentFor(fødselsnummer) } returns
                meldinger(
                    fødselsnummer,
                    stans(StoppknappÅrsak.MEDISINSK_VILKAR),
                    opphevStans(),
                    stans(StoppknappÅrsak.AKTIVITETSKRAV),
                )
        val dataTilSpeil = stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer)

        assertTrue(dataTilSpeil.erUnntatt)
        assertEquals(listOf(StoppknappÅrsak.AKTIVITETSKRAV.name), dataTilSpeil.arsaker)
    }

    private fun stans(vararg årsaker: StoppknappÅrsak) = "STOPP_AUTOMATIKK" to årsaker.toSet()

    private fun opphevStans() = "NORMAL" to emptySet<StoppknappÅrsak>()

    private fun meldinger(fødselsnummer: String, vararg statusOgÅrsaker: Pair<String, Set<StoppknappÅrsak>>) =
        statusOgÅrsaker.map {
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer,
                it.first,
                it.second,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
            )
        }
}
