package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderVergemålOgFullmaktTest : ApplicationTest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = VedtaksperiodeId(UUID.randomUUID())
    }

    private lateinit var fødselsnummer: String

    private val command by lazy {
        VurderVergemålOgFullmakt(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
        )
    }

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

    @BeforeEach
    fun settOppTestdata() {
        fødselsnummer =
            lagPerson()
                .also(sessionContext.personRepository::lagre)
                .id.value
        val vedtaksperiode =
            lagVedtaksperiode(id = VEDTAKSPERIODE_ID)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
            .also(sessionContext.behandlingRepository::lagre)
    }

    @Test
    fun `Ber om informasjon om vergemål hvis den mangler`() {
        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertEquals(setOf(Behov.Vergemål, Behov.Fullmakt), observer.behov.toSet())
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() {
        assertFalse(command.resume(commandContext, sessionContext, outbox))
        assertEquals(null, sessionContext.vergemålDao.harVergemål(fødselsnummer))
    }

    @Test
    fun `lagrer svar på vergemål ved løsning ingen vergemål`() {
        val ingenVergemål = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(ingenVergemål))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(fødselsnummer))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(fødselsnummer))
        assertEquals(0, observer.hendelser.size)
        assertEquals(0, sessionContext.varselRepository.alle().size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har vergemål`() {
        val harVergemål = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(harVergemål))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(true, sessionContext.vergemålDao.harVergemål(fødselsnummer))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(fødselsnummer))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fremtidsfullmakt`() {
        val harFullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true)
        commandContext.add(Vergemålløsning(harFullmakt))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(fødselsnummer))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(fødselsnummer))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `lagrer svar på vergemål ved løsning har fullmakt`() {
        val harFremtidsfullmakt = VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false)
        commandContext.add(Vergemålløsning(harFremtidsfullmakt))
        commandContext.add(Fullmaktløsning(true))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(false, sessionContext.vergemålDao.harVergemål(fødselsnummer))
        assertEquals(true, sessionContext.vergemålDao.harFullmakt(fødselsnummer))
        assertEquals(0, observer.hendelser.size)
    }

    @Test
    fun `legger til varsel ved vergemål`() {
        val harAlt = VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = true)
        commandContext.add(Vergemålløsning(harAlt))
        commandContext.add(Fullmaktløsning(false))
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(true, sessionContext.vergemålDao.harVergemål(fødselsnummer))
        assertEquals(false, sessionContext.vergemålDao.harFullmakt(fødselsnummer))
        assertEquals(0, observer.hendelser.size)
        assertEquals(
            "SB_EX_4",
            sessionContext.varselRepository
                .alle()
                .single()
                .kode,
        )
    }
}
