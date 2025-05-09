package no.nav.helse.spesialist.application.modell

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderVergemålOgFullmaktTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.fromString("1cd0d9cb-62e8-4f16-b634-f2b9dab550b6")
    }

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val legacyBehandling =
        LegacyBehandling(UUID.randomUUID(), VEDTAKSPERIODE_ID, 1 jan 2018, 31 jan 2018, 1 jan 2018)
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, 1 jan 2018, listOf(legacyBehandling))

    private val command =
        VurderVergemålOgFullmakt(
            fødselsnummer = FNR,
            vergemålDao = vergemålDao,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()
            val hendelser = mutableListOf<String>()

            override fun behov(behov: Behov, commandContextId: UUID) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(vergemålDao)
    }

    @Test
    fun `Ber om informasjon om vergemål hvis den mangler`() {
        assertFalse(command.execute(context))
        assertEquals(setOf(Behov.Vergemål, Behov.Fullmakt), observer.behov.toSet())
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { vergemålDao.lagre(any(), any(), any()) }
    }

    @Test
    fun `lagrer svar på vergemål ved løsning ingen vergemål`() {
        val ingenVergemål = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        context.add(Vergemålløsning(ingenVergemål))
        context.add(Fullmaktløsning(false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, ingenVergemål, false) }
        assertEquals(0, observer.hendelser.size)
        legacyBehandling.inspektør {
            assertEquals(0, varsler.size)
        }
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har vergemål`() {
        val harVergemål = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false)
        context.add(Vergemålløsning(harVergemål))
        context.add(Fullmaktløsning(false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harVergemål, false) }
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fremtidsfullmakt`() {
        val harFullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true)
        context.add(Vergemålløsning(harFullmakt))
        context.add(Fullmaktløsning(false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFullmakt, false) }
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fullmakt`() {
        val harFremtidsfullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        context.add(Vergemålløsning(harFremtidsfullmakt))
        context.add(Fullmaktløsning(true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFremtidsfullmakt, true) }
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `legger til varsel ved vergemål`() {
        val harAlt = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = true)
        context.add(Vergemålløsning(harAlt))
        context.add(Fullmaktløsning(false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harAlt, false) }
        assertEquals(0, observer.hendelser.size)
        legacyBehandling.inspektør {
            assertEquals(1, varsler.size)
        }
    }
}
