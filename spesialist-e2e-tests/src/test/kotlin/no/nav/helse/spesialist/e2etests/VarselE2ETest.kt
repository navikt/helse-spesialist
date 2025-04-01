package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VarselE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ingen varsel`() {
        // Given:

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(emptySet())
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE"))

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(setOf("SB_RV_1"))
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(setOf("SB_EX_1"))
    }

    @Test
    fun `lager ikke duplikatvarsel ved flere svar på samme behov for åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()
        besvarBehovIgjen("ÅpneOppgaver")

        // Then:
        assertVarselkoder(setOf("SB_EX_1"))
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
        assertVarselkoder(emptySet())
    }

    @Test
    fun `varsel dersom kall til gosys feilet`() {
        // Given:
        åpneOppgaverBehovLøser.oppslagFeilet = true

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        assertVarselkoder(setOf("SB_EX_3"))
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
        assertVarselkoder(emptySet())
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
        assertVarselkoder(setOf("SB_EX_1", "SB_RV_1"))
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
        assertVarselkoder(setOf("SB_EX_3", "SB_RV_1"))
    }

    @Test
    fun `lagrer varsler når vi mottar ny aktivitet i aktivitetsloggen`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(setOf("EN_KODE"))
    }

    @Test
    fun `lagrer varsler når vi mottar flere ny aktivitet i aktivitetsloggen`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_ANNEN_KODE"))
            },
        )

        // Then:
        assertVarselkoder(setOf("EN_KODE", "EN_ANNEN_KODE"))
    }

    @Test
    fun `lagrer flere varsler når vi mottar flere nye aktiviteter i samme aktivitetslogg`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE", "EN_ANNEN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(setOf("EN_KODE", "EN_ANNEN_KODE"))
    }

    @Test
    fun `gammelt avviksvarsel erstattes av nytt avviksvarsel`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")

        // When:
        søknadOgGodkjenningbehovKommerInn(
            tilleggsmeldinger = {
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
                aktivitetsloggNyAktivitet(varselkoder = listOf("EN_KODE"))
            }
        )

        // Then:
        assertVarselkoder(setOf("EN_KODE"))
    }

    @Test
    fun `varsler for ulike vedtaksperioder går ikke i beina på hverandre`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")
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
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name)), hentVarselkoder(førsteVedtaksperiode()))
        assertEquals(setOf(Varsel("EN_ANNEN_KODE", AKTIV.name)), hentVarselkoder(andreVedtaksperiode()))
    }

    private fun assertVarselkoder(expected: Set<String>) {
        medPersonISpeil { assertVarselkoder(expected, førsteVedtaksperiode()) }
    }
}
