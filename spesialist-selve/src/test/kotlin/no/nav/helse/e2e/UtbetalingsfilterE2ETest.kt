package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `Går gjennom begge filtreringer`() {
        val spleisBehandlingId = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        spesialistBehandlerGodkjenningsbehovTilOgMedUtbetalingsfilter(
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                periodetype = FORLENGELSE,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                spleisBehandlingId = spleisBehandlingId,
            )
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertGodkjenningsbehovIkkeBesvart()
        assertIkkeAvvistIUtbetalingsfilter()
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        val spleisBehandlingId = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        spesialistBehandlerGodkjenningsbehovTilOgMedUtbetalingsfilter(
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                spleisBehandlingId = spleisBehandlingId,
            )
        )
        assertVedtaksperiodeEksisterer(VEDTAKSPERIODE_ID)
        assertIkkeAvvistIUtbetalingsfilter()
    }

    // Dette er litt skjørt, men jeg finner ikke noen bedre måte å asserte at UtbetalingfilterCommand kjørte OK på
    private fun assertIkkeAvvistIUtbetalingsfilter() = assertSisteEtterspurteBehov("EgenAnsatt")

}
