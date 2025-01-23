package no.nav.helse.modell.automatisering

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.godkjenningsbehovData
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.VergemålRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderAutomatiskAvvisningTest {
    private lateinit var context: CommandContext

    private val vergemålRepository = mockk<VergemålRepository>(relaxed = true)
    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålRepository, personRepository, egenAnsattDao, godkjenningMediator, sykefraværstilfelle)
    }

    @Test
    fun `skal avvise ved vergemål dersom perioden kan avvises`() {
        every { vergemålRepository.harVergemål(fødselsnummer) } returns true
        assertAvvisning(lagCommand(kanAvvises = true), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom perioden ikke kan avvises`() {
        every { vergemålRepository.harVergemål(fødselsnummer) } returns true
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    @Test
    fun `skal avvise dersom IM mangler`() {
        every { sykefraværstilfelle.harVarselOmManglendeInntektsmelding(any()) } returns true
        //testen gir for øyeblikket ikke mening da vi midlertidig slipper igjennom alle
        //assertAvvisning(lagCommand(fødselsnummer = "01111111111", kanAvvises = true), "Mangler inntektsmelding")
        assertIkkeAvvisning(lagCommand(fødselsnummer = "01111111111", kanAvvises = true))
    }

    @Test
    fun `skal ikke avvise dersom IM er mottatt`() {
        every { sykefraværstilfelle.harVarselOmManglendeInntektsmelding(any()) } returns false
        assertIkkeAvvisning(lagCommand(fødselsnummer = "01111111111", kanAvvises = true))
    }

    @Test
    fun `skal ikke avvise dersom IM mangler, men fødselsdato treffer toggle`() {
        every { sykefraværstilfelle.harVarselOmManglendeInntektsmelding(any()) } returns true
        assertIkkeAvvisning(lagCommand(fødselsnummer = "29111111111", kanAvvises = true))
    }

    @Test
    fun `skal avvise ved utland dersom perioden kan avvises`() {
        every { personRepository.finnEnhetId(fødselsnummer) } returns "0393"
        assertAvvisning(lagCommand(kanAvvises = true), "Utland")
    }

    @Test
    fun `skal ikke avvise ved utland dersom perioden ikke kan avvises`() {
        every { personRepository.finnEnhetId(fødselsnummer) } returns "0393"
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
                utbetaling = any(),
                behov = any()
            )
        }
    }

    private fun assertIkkeAvvisning(command: VurderAutomatiskAvvisning) {
        assertTrue(command.execute(context))
        verify(exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any(), any()) }
    }

    private fun lagCommand(
        kanAvvises: Boolean = true,
        fødselsnummer: String = "12345678910",
    ) = VurderAutomatiskAvvisning(
        personRepository = personRepository,
        vergemålRepository = vergemålRepository,
        godkjenningMediator = godkjenningMediator,
        utbetaling = Utbetaling(utbetalingId, 1000, 1000, Utbetalingtype.UTBETALING),
        godkjenningsbehov = godkjenningsbehovData(
            fødselsnummer = fødselsnummer,
            kanAvvises = kanAvvises,
        ),
        sykefraværstilfelle = sykefraværstilfelle
    )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val utbetalingId = UUID.randomUUID()
    }
}
