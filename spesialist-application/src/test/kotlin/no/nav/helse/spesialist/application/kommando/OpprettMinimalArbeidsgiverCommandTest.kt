package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.spesialist.application.InMemoryArbeidsgiverRepository
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettMinimalArbeidsgiverCommandTest {
    private val context: CommandContext = CommandContext(UUID.randomUUID())
    private val arbeidsgiverRepository = InMemoryArbeidsgiverRepository()

    @Test
    fun `opprett arbeidsgiver`() {
        // Given:
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val command = OpprettMinimalArbeidsgiverCommand(
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverRepository = arbeidsgiverRepository
        )

        // When:
        val completed = command.execute(context)

        // Then:
        assertTrue(completed)
        assertEquals(1, arbeidsgiverRepository.alle().size)
        assertEquals(
            ArbeidsgiverIdentifikator.Organisasjonsnummer(organisasjonsnummer),
            arbeidsgiverRepository.alle().single().id()
        )
        assertEquals(
            null,
            arbeidsgiverRepository.alle().single().navn,
        )
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        // Given:
        val organisasjonsnummer = lagOrganisasjonsnummer()
        arbeidsgiverRepository.lagre(
            Arbeidsgiver.Factory.ny(
                identifikator = ArbeidsgiverIdentifikator.Organisasjonsnummer(
                    organisasjonsnummer
                )
            )
        )
        val command = OpprettMinimalArbeidsgiverCommand(
            organisasjonsnummer = organisasjonsnummer,
            arbeidsgiverRepository = arbeidsgiverRepository
        )

        // When:
        val completed = command.execute(context)

        // Then:
        assertTrue(completed)
        assertEquals(1, arbeidsgiverRepository.alle().size)
    }
}
