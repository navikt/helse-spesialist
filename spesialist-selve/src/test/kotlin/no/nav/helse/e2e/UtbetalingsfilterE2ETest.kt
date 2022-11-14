package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.SAKSBEHANDLER_IDENT
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.Testdata._MODIFISERTBART_FØDSELSNUMMER
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `fødselsnummer passer ikke`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            personbeløp = 1500,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'"))
        assertIkkeEtterspurtBehov("EgenAnsatt")
    }

    @Test
    fun `Går gjennom begge filtreringer`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            personbeløp = 1500,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        sendSaksbehandlerløsningFraAPI(
            testRapid.inspektør.oppgaveId(),
            SAKSBEHANDLER_IDENT,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_OID,
            true
        )
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `overlappende utbetaling (aka delvis refusjon) går ikke gjennom`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            personbeløp = 1500,
            arbeidsgiverbeløp = 500,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Brukerutbetalingsfilter: Utbetalingen består av delvis refusjon"))
        assertIkkeEtterspurtBehov("EgenAnsatt")
    }

    @Test
    fun `Går gjennom første filtreringer, men fått warning før andre filtrering`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            personbeløp = 1500,
            risikofunn = listOf(
                Risikofunn(
                    kreverSupersaksbehandler = false,
                    kategori = listOf("Noe"),
                    beskrivele = "Noe"
                )
            )
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Brukerutbetalingsfilter: Vedtaksperioden har warnings"))
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        behandleGodkjenningsbehov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            arbeidsgiverbeløp = 500,
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(true, "Automatisk behandlet")
    }

    private fun behandleGodkjenningsbehov(
        fødselsnummer: String,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        personbeløp: Int = 0,
        arbeidsgiverbeløp: Int = 0,
        risikofunn: List<Risikofunn> = emptyList()
    ) {
        _MODIFISERTBART_FØDSELSNUMMER = fødselsnummer
        vedtaksperiode(
            fødselsnummer = fødselsnummer,
            utbetalingId = utbetalingId,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            snapshot = snapshot(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                personbeløp = personbeløp,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
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
