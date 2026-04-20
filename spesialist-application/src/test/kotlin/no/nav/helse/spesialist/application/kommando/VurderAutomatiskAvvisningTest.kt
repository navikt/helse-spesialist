package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.db.PersonDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class VurderAutomatiskAvvisningTest : ApplicationTest() {
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())
    private val fødselsnummer = lagFødselsnummer()

    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)

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
        forventetBegrunnelse: String,
    ) {
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertUtgåendeHendelse<VedtaksperiodeAvvistAutomatisk>()
        assertUtgåendeHendelse<Godkjenningsbehovløsning> {
            assertEquals(listOf(forventetBegrunnelse), it.begrunnelser)
        }
    }

    private fun assertIkkeAvvisning(command: VurderAutomatiskAvvisning) {
        assertTrue(command.execute(commandContext, sessionContext, outbox))
        assertIkkeUtgåendeHendelse<VedtaksperiodeAvvistAutomatisk>()
        assertIkkeUtgåendeHendelse<Godkjenningsbehovløsning>()
    }

    private fun lagCommand(
        kanAvvises: Boolean = true,
        fødselsnummer: String = this.fødselsnummer,
    ) = VurderAutomatiskAvvisning(
        personDao = personDao,
        vergemålDao = vergemålDao,
        godkjenningsbehov =
            godkjenningsbehovData(
                fødselsnummer = fødselsnummer,
                kanAvvises = kanAvvises,
            ),
    )
}
