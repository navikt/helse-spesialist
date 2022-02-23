package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.modell.person.PersonDao.Utbetalingen
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `Går ikke gjennom første filtrering`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            arbeidsgiverNettoBeløp = 0,
            personNettoBeløp = 1
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Utbetalingsfilter: Fødselsdag passer ikke"))
        assertIkkeEtterspurtBehov("EgenAnsatt")
    }

    @Test
    fun `Går gjennom begge filtreringer`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            arbeidsgiverNettoBeløp = 0,
            personNettoBeløp = 1
        )
        assertVedtak(vedtaksperiodeId)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(), SAKSBEHANDLER_IDENT, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, true)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `Går gjennom første filtreringer, men fått warning før andre filtrering`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            arbeidsgiverNettoBeløp = 0,
            personNettoBeløp = 1,
            risikofunn = listOf("MjA@")
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Utbetalingsfilter: Vedtaksperioden har warnings"))
    }

    @Test
    fun `Går alltid gjennom uten personbeløp`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            arbeidsgiverNettoBeløp = 1,
            personNettoBeløp = 0
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(true, "Automatisk behandlet")
    }

    private fun behandleGodkjenningsbeov(
        fødselsnummer: String,
        arbeidsgiverNettoBeløp: Int?,
        personNettoBeløp: Int?,
        risikofunn: List<String> = emptyList()
    ) {
        FØDSELSNUMMER = fødselsnummer
        vedtaksperiode(
            utbetalingId = utbetalingId,
            vedtaksperiodeId = vedtaksperiodeId,
            snapshot = snapshot(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingen = Utbetalingen(
                    utbetalingId = utbetalingId,
                    arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                    personNettoBeløp = personNettoBeløp
                )
            ),
            kanAutomatiseres = risikofunn.isEmpty(),
            risikofunn = risikofunn
        )
    }

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER = "12020052345"
        private const val FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER = "31020052345"
    }
}
