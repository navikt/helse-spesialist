package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `Går gjennom begge filtreringer`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremForbiUtbetalingsfilter(
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                periodetype = FORLENGELSE,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
            )
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertGodkjenningsbehovIkkeBesvart()
        assertIkkeAvvistIUtbetalingsfilter()
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremForbiUtbetalingsfilter(
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
            )
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertIkkeAvvistIUtbetalingsfilter()
    }

    // Dette er litt skjørt, men jeg finner ikke noen bedre måte å asserte at UtbetalingfilterCommand kjørte OK på
    private fun assertIkkeAvvistIUtbetalingsfilter() = assertSisteEtterspurteBehov("EgenAnsatt")

}
