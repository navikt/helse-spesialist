package no.nav.helse.modell.stoppautomatiskbehandling

import TilgangskontrollForTestHarIkkeTilgang
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.OpphevStans
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID

class StansAutomatiskBehandlingMediatorTest {
    private val stansAutomatiskBehandlingDao = mockk<StansAutomatiskBehandlingDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)

    private companion object {
        private const val FNR = "12345678910"
    }

    private val mediator =
        StansAutomatiskBehandlingMediator(
            stansAutomatiskBehandlingDao,
            periodehistorikkDao,
            oppgaveDao,
            utbetalingDao,
            notatMediator,
        )

    @Test
    fun `Lagrer melding og periodehistorikk når stoppknapp-mleding håndteres`() {
        mediator.håndter(
            fødselsnummer = FNR,
            status = "STOPP_AUTOMATIKK",
            årsaker = setOf("MEDISINSK_VILKAR"),
            opprettet = now(),
            originalMelding = "{}",
            kilde = "ISYFO",
        )

        verify(exactly = 1) {
            stansAutomatiskBehandlingDao.lagre(
                fødselsnummer = FNR,
                status = "STOPP_AUTOMATIKK",
                årsaker = setOf("MEDISINSK_VILKAR"),
                opprettet = any(),
                originalMelding = "{}",
                kilde = "ISYFO",
            )
        }
        verify(exactly = 1) {
            periodehistorikkDao.lagre(
                historikkType = STANS_AUTOMATISK_BEHANDLING,
                saksbehandlerOid = null,
                utbetalingId = any(),
                notatId = null,
            )
        }
    }

    @Test
    fun `Lagrer melding og notat når stans oppheves fra speil`() {
        val oid = UUID.randomUUID()
        mediator.håndter(
            handling = OpphevStans(FNR, "begrunnelse"),
            saksbehandler =
                Saksbehandler(
                    epostadresse = "epost",
                    oid = oid,
                    navn = "navn",
                    ident = "ident",
                    tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
                ),
        )

        verify(exactly = 1) {
            stansAutomatiskBehandlingDao.lagre(
                fødselsnummer = FNR,
                status = "NORMAL",
                årsaker = emptySet(),
                opprettet = any(),
                originalMelding = null,
                kilde = "SPEIL",
            )
        }
        verify(exactly = 1) {
            notatMediator.lagreForOppgaveId(
                oppgaveId = any(),
                tekst = "begrunnelse",
                saksbehandler_oid = oid,
                notatType = NotatType.OpphevStans,
            )
        }
    }

    @Test
    fun `Melding med status STOPP_AUTOMATIKK gjør at personen skal unntas fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hent(FNR) } returns
            meldinger(
                stans("MEDISINSK_VILKAR", "MANGLENDE_MEDVIRKNING"),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertTrue(mediator.erUnntatt(FNR))
        assertTrue(dataTilSpeil.erUnntatt)
        assertEquals(listOf("MEDISINSK_VILKAR", "MANGLENDE_MEDVIRKNING"), dataTilSpeil.arsaker)
    }

    @Test
    fun `Melding med status NORMAL gjør at personen ikke lenger er unntatt fra automatisering`() {
        every { stansAutomatiskBehandlingDao.hent(FNR) } returns
            meldinger(
                stans("MEDISINSK_VILKAR"),
                opphevStans(),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertFalse(mediator.erUnntatt(FNR))
        assertFalse(dataTilSpeil.erUnntatt)
        assertEquals(emptyList<String>(), dataTilSpeil.arsaker)
    }

    @Test
    fun `Kan stanses på nytt etter stans er opphevet`() {
        every { stansAutomatiskBehandlingDao.hent(FNR) } returns
            meldinger(
                stans("MEDISINSK_VILKAR"),
                opphevStans(),
                stans("AKTIVITETSKRAV"),
            )
        val dataTilSpeil = mediator.unntattFraAutomatiskGodkjenning(FNR)

        assertTrue(mediator.erUnntatt(FNR))
        assertTrue(dataTilSpeil.erUnntatt)
        assertEquals(listOf("AKTIVITETSKRAV"), dataTilSpeil.arsaker)
    }

    private fun stans(vararg årsaker: String) = "STOPP_AUTOMATIKK" to årsaker.toSet()

    private fun opphevStans() = "NORMAL" to emptySet<String>()

    private fun meldinger(vararg statusOgÅrsaker: Pair<String, Set<String>>) =
        statusOgÅrsaker.map { StansAutomatiskBehandlingFraDatabase(FNR, it.first, it.second, now()) }
}
