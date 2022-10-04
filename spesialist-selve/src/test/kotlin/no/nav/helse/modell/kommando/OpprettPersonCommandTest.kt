package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettPersonCommandTest {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne
        private val ADRESSEBESKYTTELSE = Adressebeskyttelse.Fortrolig

        private val objectMapper = jacksonObjectMapper()
    }

    private val dao = mockk<PersonDao>(relaxed = true)
    private val command = OpprettPersonCommand(FNR, AKTØR, dao)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `oppretter ikke person når person finnes fra før`() {
        personFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter ikke person når person er opprettet under påvente av informasjonsbehov`() {
        personFinnesIkke()
        context.add(HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE))
        context.add(HentEnhetløsning(ENHET_OSLO))
        assertFalse(command.execute(context))
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
        personFinnes()
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))
        assertTrue(command.resume(context))
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    @Test
    fun `oppretter person`() {
        val personinfoId = 4691337L
        every { dao.insertPersoninfo(any(), any(), any(), any(), any(), any()) } returns personinfoId

        personFinnesIkke()
        context.add(HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE))
        context.add(HentEnhetløsning(ENHET_OSLO))
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())

        verify(exactly = 1) { dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE) }
        verify(exactly = 1) { dao.insertPerson(FNR, AKTØR, personinfoId, any(), any()) }
    }

    @Test
    fun `ber om manglende informasjon`() {
        personFinnesIkke()
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare personinfo`() {
        personFinnesIkke()
        context.add(HentPersoninfoløsning(FNR, FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, Kjønn.Kvinne, Adressebeskyttelse.StrengtFortroligUtland))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare enhet`() {
        personFinnesIkke()
        context.add(HentEnhetløsning(ENHET_OSLO))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    @Test
    fun `kan ikke opprette person med bare infotrygdutbetalinger`() {
        personFinnesIkke()
        context.add(HentInfotrygdutbetalingerløsning(objectMapper.createObjectNode()))
        assertFalse(command.execute(context))
        assertHarBehov()
    }

    private fun assertHarBehov() {
        assertTrue(context.harBehov())
        assertEquals(listOf("HentPersoninfoV2", "HentEnhet", "HentInfotrygdutbetalinger"), context.behov().keys.toList())
        verify(exactly = 0) { dao.insertPerson(FNR, any(), any(), any(), any()) }
    }

    private fun personFinnes() {
        every { dao.findPersonByFødselsnummer(FNR) } returns 1
    }
    private fun personFinnesIkke() {
        every { dao.findPersonByFødselsnummer(FNR) } returns null
    }

}
