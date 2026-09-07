package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderVergemålOgFullmaktTest : ApplicationTest() {
    private val command =
        VurderVergemålOgFullmakt(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode1.id.value,
        )
    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()
            val hendelser = mutableListOf<String>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    @Test
    fun `Ber om informasjon om vergemål hvis den mangler`() {
        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertEquals(setOf(Behov.Vergemål, Behov.Fullmakt), observer.behov.toSet())
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() {
        assertFalse(command.resume(commandContext, sessionContext, outbox))
        assertEquals(null, sessionContext.vergemålDao.harVergemål(person.id.value))
    }

    @Test
    fun `lagrer svar på vergemål ved løsning ingen vergemål`() {
        val ingenVergemål = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(ingenVergemål))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(person.id.value))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(person.id.value))
        assertEquals(0, observer.hendelser.size)
        behandling1.assertAntallVarsler(0)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har vergemål`() {
        val harVergemål = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(harVergemål))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(true, sessionContext.vergemålDao.harVergemål(person.id.value))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(person.id.value))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fremtidsfullmakt`() {
        val harFullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true)
        commandContext.add(Vergemålløsning(harFullmakt))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(person.id.value))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(person.id.value))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fullmakt`() {
        val harFremtidsfullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(harFremtidsfullmakt))
        commandContext.add(Fullmaktløsning(true))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(person.id.value))
        assertEquals(true, sessionContext.vergemålDao.harFullmakt(person.id.value))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `legger til varsel ved vergemål`() {
        val harAlt = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = true)
        commandContext.add(Vergemålløsning(harAlt))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(true, sessionContext.vergemålDao.harVergemål(person.id.value))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(person.id.value))
        assertEquals(0, observer.hendelser.size)
        behandling1.assertAntallVarsler(1)
    }
}
