package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersisterInntektCommandTest {
    private companion object {
        private const val FNR = "12345678910"
    }

    private val personDao = mockk<PersonDao>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(personDao)
    }

    @Test
    fun `Sender behov om inntekt ikke er lagret fra før`() {
        every { personDao.findInntekter(any(), any()) } returns null

        val context = CommandContext(UUID.randomUUID())
        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
    }

    @Test
    fun `Fullfører dersom inntekt er lagret fra før`() {
        every { personDao.findInntekter(any(), any()) } returns inntekter()

        val context = CommandContext(UUID.randomUUID())
        val command = PersisterInntektCommand(FNR, LocalDate.now(), personDao)

        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
    }

    @Test
    fun `Lagrer inntekter dersom det ikke finnes på skjæringstidspunkt for person`() {
        val skjæringtidspunkt = LocalDate.now()

        every { personDao.findInntekter(FNR, skjæringtidspunkt) } returns null

        val context = CommandContext(UUID.randomUUID())
        val command = PersisterInntektCommand(FNR, skjæringtidspunkt, personDao)

        assertFalse(command.execute(context))
        assertTrue(context.harBehov())

        context.add(løsning())
        assertTrue(command.resume(context))
        verify(exactly = 1) { personDao.insertInntekter(FNR, skjæringtidspunkt, inntekter()) }
    }

    private fun løsning(inntekter: List<Inntekter> = inntekter()) = Inntektløsning(inntekter)

    private fun inntekter() = listOf(
        Inntekter(
            årMåned = YearMonth.parse("2022-11"),
            inntektsliste = listOf(Inntekter.Inntekt(beløp = 20000.0, orgnummer = "123456789"))
        )
    )
}
