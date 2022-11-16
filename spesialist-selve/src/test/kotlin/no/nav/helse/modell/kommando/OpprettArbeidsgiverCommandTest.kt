package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.Arbeidsgiverinformasjonløsning
import no.nav.helse.mediator.meldinger.løsninger.HentPersoninfoløsning
import no.nav.helse.mediator.meldinger.løsninger.HentPersoninfoløsninger
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettArbeidsgiverCommandTest {
    private companion object {
        private const val NAVN = "Et eller annet fint da"
        private const val ORGNR = "123456789"
        private val BRANSJER = listOf("Spaghettikoding")
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = OpprettArbeidsgiverCommand(listOf(ORGNR), dao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `opprett arbeidsgiver`() {
        arbeidsgiverFinnesIkke()
        context.add(
            Arbeidsgiverinformasjonløsning(
                listOf(
                    Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(
                        ORGNR,
                        NAVN,
                        BRANSJER
                    )
                )
            )
        )
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER) }
    }

    @Test
    fun `forespør informasjon når personinfo mangeøer`() {
        val fnr = "12345678911"
        arbeidsgiverFinnesIkke(fnr)
        val command = OpprettArbeidsgiverCommand(listOf(fnr), dao)
        assertFalse(command.execute(context))
        context.behov().getValue("HentPersoninfoV2").also { behov ->
            assertEquals(listOf(fnr), behov["ident"])
        }
    }

    @Test
    fun `opprett person-arbeidsgiver`() {
        val fnr = "12345678911"
        arbeidsgiverFinnesIkke(fnr)
        context.add(HentPersoninfoløsninger(listOf(
            HentPersoninfoløsning(fnr, "LITEN", null, "TRANFLASKE", LocalDate.of(1970, 1, 1), Kjønn.Kvinne, Adressebeskyttelse.Ugradert)
        )))
        val command = OpprettArbeidsgiverCommand(listOf(fnr), dao)
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.upsertNavn(fnr, "LITEN TRANFLASKE") }
        verify(exactly = 1) { dao.upsertBransjer(fnr, listOf("Privatperson")) }
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        arbeidsgiverFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertArbeidsgiver(any(), any(), any()) }
    }

    private fun arbeidsgiverFinnes(orgnr: String = ORGNR) {
        every { dao.findArbeidsgiverByOrgnummer(orgnr) } returns 1
    }

    private fun arbeidsgiverFinnesIkke(orgnr: String = ORGNR) {
        every { dao.findArbeidsgiverByOrgnummer(orgnr) } returns null
    }
}
