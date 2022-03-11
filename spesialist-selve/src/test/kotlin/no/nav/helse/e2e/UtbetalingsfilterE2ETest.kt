package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterE2ETest : AbstractE2ETest() {

    @Test
    fun `fødselsnummer passer ikke`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, null),
                Triple(2.januar, 500, null),
                Triple(3.januar, 500, null)
            ),
            personOppdragLinjer = listOf(1.januar..3.januar),
            arbeidsgiverOppdragLinjer = emptyList()
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
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, null),
                Triple(2.januar, 500, null),
                Triple(3.januar, 500, null)
            ),
            personOppdragLinjer = listOf(1.januar..3.januar),
            arbeidsgiverOppdragLinjer = emptyList()
        )
        assertVedtak(vedtaksperiodeId)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(), SAKSBEHANDLER_IDENT, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, true)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `overlappende utbetaling (aka delvis refusjon) går ikke gjennom`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            periodeFom = 1.januar,
            periodeTom = 3.januar,
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, 500),
                Triple(2.januar, 500, null),
                Triple(3.januar, 500, null)
            ),
            personOppdragLinjer = listOf(1.januar..3.januar),
            arbeidsgiverOppdragLinjer = listOf(1.januar..1.januar)
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Utbetalingsfilter: Utbetalingen består av delvis refusjon"))
        assertIkkeEtterspurtBehov("EgenAnsatt")
    }

    @Test
    fun `tidligere delvis refusjon går gjennom`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            periodeFom = 2.januar,
            periodeTom = 3.januar,
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, 500),
                Triple(2.januar, 500, null),
                Triple(3.januar, 500, null)
            ),
            personOppdragLinjer = listOf(1.januar..3.januar),
            arbeidsgiverOppdragLinjer = listOf(1.januar..1.januar)
        )
        assertVedtak(vedtaksperiodeId)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(), SAKSBEHANDLER_IDENT, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, true)
        assertGodkjenningsbehovløsning(true, SAKSBEHANDLER_IDENT)
    }

    @Test
    fun `Går gjennom første filtreringer, men fått warning før andre filtrering`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_GÅR_GJENNOM_FILTER,
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, null),
                Triple(2.januar, 500, null),
                Triple(3.januar, 500, null)
            ),
            personOppdragLinjer = listOf(1.januar..3.januar),
            arbeidsgiverOppdragLinjer = emptyList(),
            risikofunn = listOf(Risikofunn(
                kreverSupersaksbehandler = false,
                kategori = listOf("Noe"),
                beskrivele = "Noe"
            ))
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(false, "Automatisk behandlet")
        assertVedtaksperiodeAvvist("FORLENGELSE", listOf("Utbetalingsfilter: Vedtaksperioden har warnings"))
    }

    @Test
    fun `går gjennom selv med en gammel personutbetaling`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            utbetalingstidslinje = listOf(
                Triple(1.januar, 500, null), // representerer en tidligere personutbetaling
                Triple(2.januar, 500, null), // representerer en tidligere personutbetaling
                Triple(3.januar, 500, null), // representerer en tidligere personutbetaling
                Triple(4.januar, null, 500),
                Triple(5.januar, null, 500),
                Triple(6.januar, null, 500)
            ),
            personOppdragLinjer = emptyList(),
            arbeidsgiverOppdragLinjer = listOf(4.januar..6.januar)
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(true, "Automatisk behandlet")
    }

    @Test
    fun `går gjennom uten personutbetaling`() {
        behandleGodkjenningsbeov(
            fødselsnummer = FØDSELSNUMMER_SOM_IKKE_GÅR_GJENNOM_FILTER,
            utbetalingstidslinje = listOf(
                Triple(1.januar, null, 500),
                Triple(2.januar, null, 500),
                Triple(3.januar, null, 500)
            ),
            personOppdragLinjer = emptyList(),
            arbeidsgiverOppdragLinjer = listOf(1.januar..3.januar)
        )
        assertVedtak(vedtaksperiodeId)
        assertGodkjenningsbehovløsning(true, "Automatisk behandlet")
    }

    private fun behandleGodkjenningsbeov(
        fødselsnummer: String,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        utbetalingstidslinje: List<Triple<LocalDate, Int?, Int?>> = emptyList(),
        personOppdragLinjer: List<ClosedRange<LocalDate>> = emptyList(),
        arbeidsgiverOppdragLinjer: List<ClosedRange<LocalDate>> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList()
    ) {
        FØDSELSNUMMER = fødselsnummer
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
                utbetalingstidslinje = utbetalingstidslinje,
                personOppdragLinjer = personOppdragLinjer,
                arbeidsgiverOppdragLinjer = arbeidsgiverOppdragLinjer,
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
