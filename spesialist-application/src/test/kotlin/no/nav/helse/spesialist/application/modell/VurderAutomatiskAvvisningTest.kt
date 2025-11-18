package no.nav.helse.spesialist.application.modell

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderAutomatiskAvvisningTest {
    private lateinit var context: CommandContext

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålDao, personDao, egenAnsattDao, godkjenningMediator, sykefraværstilfelle)
    }

    @Test
    fun `skal avvise ved vergemål dersom perioden kan avvises`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertAvvisning(lagCommand(kanAvvises = true), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom perioden ikke kan avvises`() {
        every { vergemålDao.harVergemål(fødselsnummer) } returns true
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    @Test
    fun `skal avvise ved utland dersom perioden kan avvises`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertAvvisning(lagCommand(kanAvvises = true), "Utland")
    }

    @Test
    fun `skal ikke avvise ved utland dersom perioden ikke kan avvises`() {
        every { personDao.finnEnhetId(fødselsnummer) } returns "0393"
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    private fun assertAvvisning(
        command: VurderAutomatiskAvvisning,
        forventetÅrsak: String,
    ) {
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            godkjenningMediator.automatiskAvvisning(
                context = context,
                begrunnelser = listOf(forventetÅrsak),
                behov = any()
            )
        }
    }

    private fun assertIkkeAvvisning(command: VurderAutomatiskAvvisning) {
        assertTrue(command.execute(context))
        verify(exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any()) }
    }

    private fun lagCommand(
        kanAvvises: Boolean = true,
        fødselsnummer: String = "12345678910",
    ) = VurderAutomatiskAvvisning(
        personDao = personDao,
        vergemålDao = vergemålDao,
        godkjenningMediator = godkjenningMediator,
        godkjenningsbehov = godkjenningsbehovData(
            fødselsnummer = fødselsnummer,
            kanAvvises = kanAvvises,
        ),
    )

    private companion object {
        private const val fødselsnummer = "12345678910"
    }
}
