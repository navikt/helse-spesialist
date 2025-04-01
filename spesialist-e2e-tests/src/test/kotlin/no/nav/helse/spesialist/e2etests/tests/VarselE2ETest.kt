package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class VarselE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ingen varsel`() {
        // Given:

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(emptyList())
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE"))

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(listOf("SB_RV_1"))
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(listOf("SB_EX_1"))
    }

    @Test
    fun `lager ikke duplikatvarsel ved flere svar på samme behov for åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()
        besvarBehovIgjen("ÅpneOppgaver")

        // Then:
        assertVarselkoder(listOf("SB_EX_1"))
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.antall = 0
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        assertVarselkoder(emptyList())
    }

    @Test
    fun `varsel dersom kall til gosys feilet`() {
        // Given:
        åpneOppgaverBehovLøser.oppslagFeilet = true

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(listOf("SB_EX_3"))
    }

    @Test
    fun `fjern varsel dersom kall til gosys ikke feiler lenger`() {
        // Given:
        åpneOppgaverBehovLøser.oppslagFeilet = true

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.oppslagFeilet = false
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        assertVarselkoder(emptyList())
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 0
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.antall = 1
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        assertVarselkoder(listOf("SB_EX_1", "SB_RV_1"))
    }

    @Test
    fun `legger til varsel dersom oppslag feiler når vi har fått beskjed om at gosys har endret seg`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.oppslagFeilet = true
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        assertVarselkoder(listOf("SB_EX_3", "SB_RV_1"))
    }

    @Test
    fun `lagrer varsler når vi mottar ny aktivitet i aktivitetsloggen`() {
        // Given:
        varseldefinisjonOpprettes("EN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(listOf("EN_KODE"))
    }

    @Test
    fun `lagrer varsler når vi mottar flere ny aktivitet i aktivitetsloggen`() {
        // Given:
        varseldefinisjonOpprettes("EN_KODE")
        varseldefinisjonOpprettes("EN_ANNEN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_ANNEN_KODE"))
            },
        )

        // Then:
        assertVarselkoder(listOf("EN_KODE", "EN_ANNEN_KODE"))
    }

    @Test
    fun `lagrer flere varsler når vi mottar flere nye aktiviteter i samme aktivitetslogg`() {
        // Given:
        varseldefinisjonOpprettes("EN_KODE")
        varseldefinisjonOpprettes("EN_ANNEN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE", "EN_ANNEN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(listOf("EN_KODE", "EN_ANNEN_KODE"))
    }

    @Test
    fun `gammelt avviksvarsel erstattes av nytt avviksvarsel`() {
        // Given:
        varseldefinisjonOpprettes("EN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(listOf("EN_KODE"))
    }

    @Test
    fun `varsler for ulike vedtaksperioder går ikke i beina på hverandre`() {
        // Given:
        varseldefinisjonOpprettes("EN_KODE")
        varseldefinisjonOpprettes("EN_ANNEN_KODE")
        leggTilVedtaksperiode()

        // When:
        personSenderSøknad()
        spleisForberederBehandling(
            vedtaksperiode = førsteVedtaksperiode(),
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
            }
        )
        spleisForberederBehandling(
            vedtaksperiode = andreVedtaksperiode(),
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_ANNEN_KODE"))
            }
        )
        spleisSenderGodkjenningsbehov(førsteVedtaksperiode())

        // Then:
        medPersonISpeil {
            assertVarselkoder(listOf("EN_KODE"), førsteVedtaksperiode())
            assertVarselkoder(listOf("EN_ANNEN_KODE"), andreVedtaksperiode())
        }
    }

    private fun assertVarselkoder(expected: List<String>) {
        medPersonISpeil { assertVarselkoder(expected, førsteVedtaksperiode()) }
    }
}
