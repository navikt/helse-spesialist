package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OppdaterArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val observer = object : CommandContextObserver {
        val behov = mutableMapOf<String, Map<String, Any>>()
        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov[behov] = detaljer
        }

        override fun hendelse(hendelse: String) {}
    }

    @BeforeEach
    fun setup() {
        clearMocks(dao)
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
    }

    @Test
    fun `oppdaterer ikke når informasjonen er ny`() {
        every { dao.findNavnSistOppdatert(ORGNR) } returns LocalDate.now()
        every { dao.findBransjerSistOppdatert(ORGNR) } returns LocalDate.now()

        løsning(ORGNR).also(context::add)
        val command = OppdaterArbeidsgiverCommand(listOf(ORGNR), dao)

        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.upsertNavn(any(), any()) }
    }

    @Test
    fun `sender behov om oppdatering når informasjonen er gammel`() {
        val ghostOrgnr = "orgnr2"

        every { dao.findNavnSistOppdatert(ORGNR) } returns LocalDate.now().minusYears(1)
        every { dao.findBransjerSistOppdatert(ORGNR) } returns LocalDate.now().minusYears(1)

        løsning(ghostOrgnr).also(context::add)

        val command = OppdaterArbeidsgiverCommand(listOf(ORGNR, ghostOrgnr), dao)
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        verify(exactly = 0) { dao.upsertNavn(any(), any()) }
    }

    @Test
    fun `sender behov om løsning ikke inneholder etterspurt info`() {
        val orgnrMedUtdatertNavn = "111555111"
        val orgnrMedUtdatertBransje = "222777222"

        every { dao.findNavnSistOppdatert(orgnrMedUtdatertNavn) } returns LocalDate.now().minusYears(1)
        every { dao.findBransjerSistOppdatert(orgnrMedUtdatertBransje) } returns LocalDate.now().minusYears(1)

        val command = OppdaterArbeidsgiverCommand(listOf(ORGNR, orgnrMedUtdatertNavn, orgnrMedUtdatertBransje), dao)

        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertTrue(
            observer.behov.any {
                it.value.values.any { behovdetalj ->
                    (behovdetalj as List<*>).containsAll(listOf(orgnrMedUtdatertNavn, orgnrMedUtdatertBransje))
                }
            }) { "Ønsket orgnr mangler i behovet: ${observer.behov}" }

        verify(exactly = 0) { dao.upsertNavn(any(), any()) }
    }

    @Test
    fun `etterspør informasjon om løsning bare inneholder svar for et annet orgnr`() {
        val orgnrMedUtdatertNavn = "111555111"
        val orgnrMedOppdaterteData = "222777222"

        every { dao.findNavnSistOppdatert(orgnrMedUtdatertNavn) } returns LocalDate.now().minusYears(1)

        løsning(orgnrMedOppdaterteData).also(context::add)

        val command = OppdaterArbeidsgiverCommand(listOf(ORGNR, orgnrMedUtdatertNavn, orgnrMedOppdaterteData), dao)

        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
        assertTrue(
            observer.behov.any {
                it.value.values.any { behovdetalj ->
                    (behovdetalj as List<*>).containsAll(listOf(orgnrMedUtdatertNavn))
                }
            }) { "Ønsket orgnr $orgnrMedUtdatertNavn mangler i behovet: ${observer.behov}" }

        verify(exactly = 0) { dao.upsertNavn(any(), any()) }
    }

    @Test
    fun `opprett person-arbeidsgiver dersom den ikke finnes`() {
        val fnr = "12345678911"

        every { dao.findNavnSistOppdatert(fnr) } returns null

        val command = OppdaterArbeidsgiverCommand(listOf(fnr), dao)
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())

        assertTrue(
            observer.behov.any {
                it.value.values.any { behovdetalj ->
                    (behovdetalj as List<*>).containsAll(listOf(fnr))
                }
            }) { "Ønsket orgnr mangler i behovet: ${observer.behov}" }

        løsningPersoninfo(fnr).also(context::add)
        assertFalse(command.execute(context))
        every { dao.findNavnSistOppdatert(fnr) } returns LocalDate.now()
        every { dao.findBransjerSistOppdatert(fnr) } returns LocalDate.now()
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.upsertNavn(fnr, "LITEN TRANFLASKE") }
        verify(exactly = 1) { dao.upsertBransjer(fnr, listOf("Privatperson")) }
    }

    private fun løsning(vararg orgnumre: String) = Arbeidsgiverinformasjonløsning(orgnumre.map { arbeidsgiverinfo(it) })

    private fun arbeidsgiverinfo(orgnr: String) = Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(orgnr, "et irrelevant navn", emptyList())

    private fun løsningPersoninfo(fnr: String) = HentPersoninfoløsninger(listOf(personinfo(fnr)))
    private fun personinfo(fnr: String) = HentPersoninfoløsning(fnr, "LITEN", null, "TRANFLASKE", LocalDate.of(1970, 1, 1), Kjønn.Kvinne, Adressebeskyttelse.Ugradert)
}
