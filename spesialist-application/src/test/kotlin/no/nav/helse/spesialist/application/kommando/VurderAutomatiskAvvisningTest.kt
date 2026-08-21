package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.melding.VedtaksperiodeAvvistAutomatisk
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class VurderAutomatiskAvvisningTest : ApplicationTest() {
    private companion object {
        private const val UTLANDSENHET = 393
        private const val ANNEN_ENHET = 100
    }

    private val commandContext: CommandContext = CommandContext(UUID.randomUUID())
    private val fødselsnummer = lagFødselsnummer()

    @Test
    fun `skal avvise ved vergemål dersom perioden kan avvises`() {
        lagrePerson(enhet = ANNEN_ENHET)
        lagreVergemål(harVergemål = true)
        assertAvvisning(lagCommand(kanAvvises = true), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom perioden ikke kan avvises`() {
        lagrePerson(enhet = ANNEN_ENHET)
        lagreVergemål(harVergemål = true)
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    @Test
    fun `skal avvise ved utland dersom perioden kan avvises`() {
        lagrePerson(enhet = UTLANDSENHET)
        assertAvvisning(lagCommand(kanAvvises = true), "Utland")
    }

    @Test
    fun `skal ikke avvise ved utland dersom perioden ikke kan avvises`() {
        lagrePerson(enhet = UTLANDSENHET)
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

    private fun lagrePerson(enhet: Int) {
        sessionContext.personRepository.lagre(
            lagPerson(id = Identitetsnummer.fraString(fødselsnummer), enhet = enhet),
        )
    }

    private fun lagreVergemål(harVergemål: Boolean) {
        sessionContext.vergemålDao.lagre(
            fødselsnummer = fødselsnummer,
            vergemålOgFremtidsfullmakt = VergemålOgFremtidsfullmakt(harVergemål = harVergemål, harFremtidsfullmakter = false),
            fullmakt = false,
        )
    }

    private fun lagCommand(
        kanAvvises: Boolean = true,
        fødselsnummer: String = this.fødselsnummer,
    ) = VurderAutomatiskAvvisning(
        godkjenningsbehov =
            godkjenningsbehovData(
                fødselsnummer = fødselsnummer,
                kanAvvises = kanAvvises,
            ),
    )
}
