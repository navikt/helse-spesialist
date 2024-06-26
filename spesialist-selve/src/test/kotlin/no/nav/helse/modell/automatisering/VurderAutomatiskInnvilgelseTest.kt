package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderAutomatiskInnvilgelseTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private const val orgnummer = "123456789"
        private val hendelseId = UUID.randomUUID()
        private val periodeType = Periodetype.FORLENGELSE
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
    private val command =
        VurderAutomatiskInnvilgelse(
            fødselsnummer,
            vedtaksperiodeId,
            null,
            hendelseId,
            automatisering,
            """{ "@event_name": "behov" }""",
            GodkjenningMediator(
                vedtakDao = mockk(relaxed = true),
                opptegnelseDao = mockk(relaxed = true),
                oppgaveDao = mockk(relaxed = true),
                utbetalingDao = mockk(relaxed = true),
                meldingDao = mockk(relaxed = true),
                generasjonDao = mockk(relaxed = true),
            ),
            Utbetaling(utbetalingId, 1000, 1000, Utbetalingtype.UTBETALING),
            periodeType,
            Sykefraværstilfelle(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1.januar,
                gjeldendeGenerasjoner = listOf(generasjon),
                skjønnsfastatteSykepengegrunnlag = emptyList(),
            ),
            orgnummer,
        )

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val hendelser = mutableListOf<String>()

            override fun behov(
                behov: String,
                ekstraKontekst: Map<String, Any>,
                detaljer: Map<String, Any>,
            ) {}

            override fun hendelse(hendelse: String) {
                hendelser.add(hendelse)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
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

        val løsning =
            observer.hendelser
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
