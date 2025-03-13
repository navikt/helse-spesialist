package no.nav.helse.modell.stoppautomatiskbehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.DialogDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StansAutomatiskBehandlinghåndtererImplTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val notatDao = mockk<NotatDao>(relaxed = true)
    private val dialogDao = mockk<DialogDao>(relaxed = true)

    private companion object {
        private const val FNR = "12345678910"
    }

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        val oid = UUID.randomUUID()
        stansAutomatiskBehandlinghåndterer.håndter(
            handling = OpphevStans(FNR, "begrunnelse"),
            legacySaksbehandler =
                LegacySaksbehandler(
                    epostadresse = "epost",
                    oid = oid,
                    navn = "navn",
                    ident = "ident",
                    tilgangskontroll = { _, _ -> false },
                ),
        )

        verify(exactly = 1) { stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = FNR) }
        verify(exactly = 1) {
            notatDao.lagreForOppgaveId(
                oppgaveId = any(),
                tekst = "begrunnelse",
                saksbehandlerOid = oid,
                notatType = NotatType.OpphevStans,
                dialogRef = any(),
            )
        }
    }

    @Test
    fun `Kan stanses på nytt etter stans er opphevet`() {
        every { stansAutomatiskBehandlingDao.hentFor(FNR) } returns
                meldinger(
                    stans(StoppknappÅrsak.MEDISINSK_VILKAR),
                    opphevStans(),
                    stans(StoppknappÅrsak.AKTIVITETSKRAV),
                )
        val dataTilSpeil = stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(FNR)

        Assertions.assertTrue(dataTilSpeil.erUnntatt)
        Assertions.assertEquals(listOf(StoppknappÅrsak.AKTIVITETSKRAV.name), dataTilSpeil.arsaker)
    }

    private fun stans(vararg årsaker: StoppknappÅrsak) = "STOPP_AUTOMATIKK" to årsaker.toSet()

    private fun opphevStans() = "NORMAL" to emptySet<StoppknappÅrsak>()

    private fun meldinger(vararg statusOgÅrsaker: Pair<String, Set<StoppknappÅrsak>>) =
        statusOgÅrsaker.map {
            StansAutomatiskBehandlingFraDatabase(
                FNR,
                it.first,
                it.second,
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
            )
        }
}