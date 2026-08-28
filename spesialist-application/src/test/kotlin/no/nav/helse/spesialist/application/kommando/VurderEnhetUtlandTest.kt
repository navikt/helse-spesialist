package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderEnhetUtlandTest : ApplicationTest() {
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())

    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @Test
    fun `skal legge på varsel om utland`() {
        val person = lagPerson(enhet = 393).also(sessionContext.personRepository::lagre)
        val slot = slot<LegacyVarsel>()
        assertTrue(hentCommand(person).execute(commandContext, sessionContext, outbox))
        verify(exactly = 1) { sykefraværstilfelle.håndter(capture(slot)) }
        assertEquals("SB_EX_5", slot.captured.toDto().varselkode)
    }

    @Test
    fun `skal legge på varsel for utland også ved revurdering`() {
        val person = lagPerson(enhet = 393).also(sessionContext.personRepository::lagre)
        val slot = slot<LegacyVarsel>()
        assertTrue(hentCommand(person).execute(commandContext, sessionContext, outbox))
        verify(exactly = 1) { sykefraværstilfelle.håndter(capture(slot)) }
        assertEquals("SB_EX_5", slot.captured.toDto().varselkode)
    }

    private fun hentCommand(person: Person) =
        VurderEnhetUtland(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle,
        )

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
