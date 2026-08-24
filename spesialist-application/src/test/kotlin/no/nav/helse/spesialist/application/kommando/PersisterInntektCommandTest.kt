package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class PersisterInntektCommandTest : ApplicationTest() {
    private companion object {
        private const val FNR = "12345678910"
    }

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    init {
        sessionContext.personRepository.lagre(lagPerson(id = Identitetsnummer.fraString(FNR)))
    }

    @Test
    fun `Sender behov om inntekt ikke er lagret fra før`() {
        val command = PersisterInntektCommand(FNR, LocalDate.now())

        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())
    }

    @Test
    fun `Fullfører dersom inntekt er lagret fra før`() {
        val skjæringtidspunkt = LocalDate.now()
        sessionContext.personDao.lagreInntekter(FNR, skjæringtidspunkt, inntekter())

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt)

        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Lagrer inntekter dersom det ikke finnes på skjæringstidspunkt for person`() {
        val skjæringtidspunkt = LocalDate.now()

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt)

        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())

        commandContext.add(løsning())
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(inntekter(), sessionContext.personDao.finnInntekter(FNR, skjæringtidspunkt))
    }

    @Test
    fun `Bryr oss ikke om løsning dersom vi har inntekter alt`() {
        val skjæringtidspunkt = LocalDate.now()
        sessionContext.personDao.lagreInntekter(FNR, skjæringtidspunkt, inntekter())

        val command = PersisterInntektCommand(FNR, skjæringtidspunkt)

        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(inntekter(), sessionContext.personDao.finnInntekter(FNR, skjæringtidspunkt))
    }

    private fun løsning(inntekter: List<Inntekter> = inntekter()) = Inntektløsning(inntekter)

    private fun inntekter() =
        listOf(
            Inntekter(
                årMåned = YearMonth.parse("2022-11"),
                inntektsliste = listOf(Inntekter.Inntekt(beløp = 20000.0, orgnummer = "123456789")),
            ),
        )
}
