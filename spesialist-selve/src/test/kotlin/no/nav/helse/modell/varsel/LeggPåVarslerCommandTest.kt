package no.nav.helse.modell.varsel

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vergemal.VergemålDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LeggPåVarslerCommandTest {
    private lateinit var context: CommandContext

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, sykefraværstilfelle)
    }

    @Test
    fun `skal legge på varsel om vergemål`() {
        val slot = slot<Varsel>()
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertTrue(hentCommand().execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_4", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal legge på varsel for vergemål også ved revurdering`() {
        val slot = slot<Varsel>()
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertTrue(hentCommand().execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_4", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal legge på varsel om utland`() {
        val slot = slot<Varsel>()
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertTrue(hentCommand().execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_5", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal legge på varsel for utland også ved revurdering`() {
        val slot = slot<Varsel>()
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertTrue(hentCommand().execute(context))
        verify (exactly = 1) { sykefraværstilfelle.håndter(capture(slot), hendelseId) }
        assertEquals("SB_EX_5", slot.captured.toDto().varselkode)
    }

    private fun hentCommand() =
        LeggPåVarslerCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            hendelseId = hendelseId,
            sykefraværstilfelle = sykefraværstilfelle
        )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
    }

}