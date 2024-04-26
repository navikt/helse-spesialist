package no.nav.helse.modell.vergemal

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class VurderVergemålOgFullmaktTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.fromString("1cd0d9cb-62e8-4f16-b634-f2b9dab550b6")
    }

    private val vedtaksperiodeObserver =
        object : IVedtaksperiodeObserver {
            val opprettedeVarsler = mutableListOf<String>()

            override fun varselOpprettet(
                varselId: UUID,
                vedtaksperiodeId: UUID,
                generasjonId: UUID,
                varselkode: String,
                opprettet: LocalDateTime,
            ) {
                opprettedeVarsler.add(varselkode)
            }
        }

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val generasjon =
        Generasjon(UUID.randomUUID(), VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar).also {
            it.registrer(vedtaksperiodeObserver)
        }
    private val sykefraværstilfelle = Sykefraværstilfelle(FNR, 1.januar, listOf(generasjon), emptyList())

    private val command =
        VurderVergemålOgFullmakt(
            hendelseId = UUID.randomUUID(),
            vergemålDao = vergemålDao,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            sykefraværstilfelle = sykefraværstilfelle,
        )
    private lateinit var context: CommandContext

    private val ingenVergemål = Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = false)
    private val harVergemål = Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false)
    private val harFullmakt = Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = false)
    private val harFremtidsfullmakt = Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = true)
    private val harAlt = Vergemål(harVergemål = true, harFremtidsfullmakter = true, harFullmakter = true)
    private val harBeggeFullmatkstyper =
        Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = true)

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<String>()
            val hendelser = mutableListOf<String>()

            override fun behov(
                behov: String,
                ekstraKontekst: Map<String, Any>,
                detaljer: Map<String, Any>,
            ) {
                this.behov.add(behov)
            }

            override fun hendelse(hendelse: String) {
                hendelser.add(hendelse)
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
        assertEquals(listOf("Vergemål"), observer.behov)
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { vergemålDao.lagre(any(), any()) }
    }

    @Test
    fun `lagrer svar på vergemål ved løsning ingen vergemål`() {
        context.add(Vergemålløsning(FNR, ingenVergemål))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, ingenVergemål) }
        assertEquals(0, observer.hendelser.size)
        assertEquals(0, vedtaksperiodeObserver.opprettedeVarsler.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har vergemål`() {
        context.add(Vergemålløsning(FNR, harVergemål))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harVergemål) }
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fullmakt`() {
        context.add(Vergemålløsning(FNR, harFullmakt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFullmakt) }
        assertEquals(0, observer.hendelser.size)
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fremtidsfullmakt`() {
        context.add(Vergemålløsning(FNR, harFremtidsfullmakt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harFremtidsfullmakt) }
        assertEquals(0, observer.hendelser.size)
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
    }

    @Test
    fun `legger til varsel ved vergemål`() {
        context.add(Vergemålløsning(FNR, harAlt))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harAlt) }
        assertEquals(0, observer.hendelser.size)
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
    }

    @Test
    fun `legger kun til en varsel ved både fullmakt og fremtidsfullmakt`() {
        context.add(Vergemålløsning(FNR, harBeggeFullmatkstyper))
        assertTrue(command.resume(context))
        verify(exactly = 1) { vergemålDao.lagre(FNR, harBeggeFullmatkstyper) }
        assertEquals(0, observer.hendelser.size)
        assertEquals(1, vedtaksperiodeObserver.opprettedeVarsler.size)
    }
}
