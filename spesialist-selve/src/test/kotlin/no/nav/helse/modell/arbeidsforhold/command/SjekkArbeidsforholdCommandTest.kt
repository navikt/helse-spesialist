package no.nav.helse.modell.arbeidsforhold.command

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDto
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SjekkArbeidsforholdCommandTest {
    val FNR = "0123456789"
    val ORGNUMMER = "98765432"
    val VEDTAKSPERIODE_ID = UUID.randomUUID()
    val arbeidsforholdDao = mockk<ArbeidsforholdDao>()
    val warningsDao = mockk<WarningDao>(relaxed=true)

    @BeforeEach
    fun setup() {
        clearMocks(arbeidsforholdDao, warningsDao)
    }

    @Test
    fun `Om inntektsmeldingen ikke har en arbeidsforholdId skal vi ikke lage warning, selv om aareg gir oss flere arbeidsforhold`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, null, arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Utvikler"),
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Produkteier")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 0) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId skal vi lage warning om aareg gir oss flere arbeidsforhold`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Utvikler"),
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Produkteier")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 1) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId og periodetype ikke er førstegangsbehandling, lager vi ikke warning`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FORLENGELSE, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Utvikler"),
            ArbeidsforholdDto(1, 1, 1.januar, null, 50, "Produkteier")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 0) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId skal vi ikke lage warning om aareg gir oss et arbeidsforhold`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 100, "Utvikler")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 0) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId skal vi ikke lage warning om arbeidsforholdet starter etter skjæringstidspunktet`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 100, "Utvikler"),
            ArbeidsforholdDto(1, 1, 17.januar, null, 100, "Utvikler")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 0) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId skal vi ikke lage warning om arbeidsforholdet slutter før skjæringstidspunktet`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, null, 100, "Utvikler"),
            ArbeidsforholdDto(1, 1, 31.januar, 15.januar, 100, "Utvikler")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 0) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Om inntektsmeldingen har en arbeidsforholdId og sluttdato er etter skjæringstidspunkt skal vi lage warning om aareg gir oss flere arbeidsforhold`() {
        val command = SjekkArbeidsforholdCommand(FNR, ORGNUMMER, VEDTAKSPERIODE_ID, Periodetype.FØRSTEGANGSBEHANDLING, 16.januar, "YEP", arbeidsforholdDao, warningsDao)

        every { arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER) } returns listOf(
            ArbeidsforholdDto(1, 1, 1.januar, 17.januar, 50, "Utvikler"),
            ArbeidsforholdDto(1, 1, 1.januar, 17.januar, 50, "Produkteier")
        )

        command.execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 1) { warningsDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }
}
