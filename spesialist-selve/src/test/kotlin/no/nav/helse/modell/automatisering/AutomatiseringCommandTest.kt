package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class AutomatiseringCommandTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private val hendelseId = UUID.randomUUID()
        private val periodeType = Periodetype.FORLENGELSE
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val command =
        AutomatiseringCommand(
            fødselsnummer,
            vedtaksperiodeId,
            hendelseId,
            automatisering,
            """{ "@event_name": "behov" }""",
            GodkjenningMediator(
                vedtakDao = mockk(relaxed = true),
                opptegnelseDao = mockk(relaxed = true)
            ),
            Utbetaling(utbetalingId, 1000, 1000, Utbetalingtype.UTBETALING),
            periodeType,
            Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()),
            1.januar
        )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `kaller automatiser utfør og returnerer true`() {
        assertTrue(command.execute(context))
        verify {
            automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `publiserer godkjenningsmelding ved automatisert godkjenning`() {
        every {
            automatisering.utfør(any(), any(), any(), any(), any(), any(), any(), captureLambda())
        } answers {
            arg<() -> Unit>(7).invoke()
        }

        assertTrue(command.execute(context))

        val løsning = context.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") }

        assertNotNull(løsning)
        if (løsning != null) {
            val automatiskBehandling: Boolean =
                løsning["@løsning"]["Godkjenning"]["automatiskBehandling"].booleanValue()

            assertTrue(automatiskBehandling)
        }
    }
}
