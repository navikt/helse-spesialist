package no.nav.helse.e2e

import AbstractE2ETestV2
import AbstractE2ETestV2.Mottaker.ARBEIDSGIVER
import AbstractE2ETestV2.Mottaker.BEGGE
import AbstractE2ETestV2.Mottaker.SYKMELDT
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Testdata._MODIFISERTBART_FØDSELSNUMMER
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterE2ETest : AbstractE2ETestV2() {

    @Test
    fun `fødselsnummer passer ikke`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            periodetype = Periodetype.FORLENGELSE,
            mottaker = SYKMELDT,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true)
        assertVedtaksperiodeAvvist(
            Periodetype.FORLENGELSE.name,
            listOf("Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'")
        )
    }

    @Test
    fun `Går gjennom begge filtreringer`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            periodetype = Periodetype.FORLENGELSE,
            mottaker = SYKMELDT,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovIkkeBesvart()
        assertIkkeAvvistIUtbetalingsfilter()
    }

    @Test
    fun `overlappende utbetaling (aka delvis refusjon) går ikke gjennom`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            periodetype = Periodetype.FORLENGELSE,
            mottaker = BEGGE,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovBesvart(godkjent = false, automatiskBehandlet = true)
        assertVedtaksperiodeAvvist(
            "FORLENGELSE",
            listOf("Brukerutbetalingsfilter: Utbetalingen består av delvis refusjon")
        )
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            periodetype = Periodetype.FORLENGELSE,
            mottaker = ARBEIDSGIVER,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertIkkeAvvistIUtbetalingsfilter()
    }

    private fun behandleGodkjenningsbehov(
        fødselsnummer: String,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        periodetype: Periodetype,
        mottaker: Mottaker,
    ) {
        _MODIFISERTBART_FØDSELSNUMMER = fødselsnummer
        fremForbiUtbetalingsfilter(
            fom = periodeFom,
            tom = periodeTom,
            periodetype = periodetype,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            mottaker = mottaker,
        )
    }

    // Dette er litt skjørt, men jeg finner ikke noen bedre måte å asserte at UtbetalingfilterCommand kjørte OK på
    private fun assertIkkeAvvistIUtbetalingsfilter() = assertSisteEtterspurteBehov("EgenAnsatt")

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER = "12020052345"
        private const val FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER = "31020052345"
    }
}
