package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class AutomatiseringE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `fatter automatisk vedtak`() {
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        // Given:
        søknadOgGodkjenningbehovKommerInn {
            // When:
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `fatter vedtak automatisk ved åpen oppgave i Speil men ikke lenger åpen oppgave i Gosys`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.antall = 0
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `tvinger automatisering hvis vedtaksperiodeId ligger i force_automatisering tabellen`() {
        // Given:
        tvingAutomatisering()

        // When: perioden har varsler, som ellers ville blokkert automatisk behandling
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `vedtaksperiode med nådd maksdato og refusjon fra AG er ikke automatiserbar`() {
        // Given:
        varseldefinisjonOpprettes("RV_OV_5")

        // When:
        val vedtaksperiode =
            søknadOgGodkjenningbehovKommerInn(
                tags = listOf("Innvilget", "ArbeidsgiverØnskerRefusjon"),
                maksdato = 1 des 2017,
            )

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
            assertVarselkoder(listOf("RV_OV_5"), vedtaksperiode)
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `periode med positiv revurdering skal automatisk godkjennes`() {
        søknadOgGodkjenningbehovKommerInn(utbetalingtype = Utbetalingtype.REVURDERING, personbeløp = 1)

        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `periode med negativ revurdering skal ikke automatisk godkjennes`() {
        søknadOgGodkjenningbehovKommerInn(utbetalingtype = Utbetalingtype.REVURDERING, personbeløp = -1)

        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `revurdering uten endringer i beløp kan automatisk godkjennes`() {
        søknadOgGodkjenningbehovKommerInn(
            utbetalingtype = Utbetalingtype.REVURDERING,
            arbeidsgiverbeløp = 0,
            personbeløp = 0,
        )

        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `periode med forlengelse av selvstendig næringsdrivende skal automatisk godkjennes`() {
        søknadOgGodkjenningbehovKommerInn(
            periodetype = Periodetype.FORLENGELSE,
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
            personbeløp = 500,
            arbeidsgiverbeløp = 0,
        )

        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `førstegangsbehandling av selvstendig næringsdrivende skal ikke automatisk godkjennes`() {
        søknadOgGodkjenningbehovKommerInn(
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
        )

        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `veileder har stanset automatisk behandling`() {
        // Given: personen finnes i spesialist (søknad kommet inn) og veileder stanser automatisk behandling
        personSenderSøknad()
        detPubliseresEnStansAutomatiskBehandlingMelding(årsaker = listOf("MEDISINSK_VILKAR"))

        // When:
        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode) {}
        spleisSenderGodkjenningsbehov(vedtaksperiode)

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }
}
