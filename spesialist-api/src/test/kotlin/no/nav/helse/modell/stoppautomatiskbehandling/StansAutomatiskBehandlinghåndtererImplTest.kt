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
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StansAutomatiskBehandlinghåndtererImplTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val notatDao = mockk<NotatDao>(relaxed = true)
    private val dialogDao = mockk<DialogDao>(relaxed = true)

    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            stansAutomatiskBehandlingDao,
            oppgaveDao,
            notatDao,
            dialogDao,
        )

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        val fødselsnummer = lagFødselsnummer()
        val oid = UUID.randomUUID()
        stansAutomatiskBehandlinghåndterer.håndter(
            handling = OpphevStans(fødselsnummer, "begrunnelse"),
            legacySaksbehandler =
                LegacySaksbehandler(
                    epostadresse = "epost",
                    oid = oid,
                    navn = "navn",
                    ident = "ident",
                ),
        )

        verify(exactly = 1) { stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = fødselsnummer) }
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
