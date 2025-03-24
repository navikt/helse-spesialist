package no.nav.helse.spesialist.e2etests

import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.INAKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_3
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_RV_1
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class VarselE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ingen varsel`() {
        // Given:

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(emptySet(), hentVarselkoder())
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE"))

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(setOf(Varsel(SB_RV_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lager ikke duplikatvarsel ved flere svar på samme behov for åpne gosys-oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        behovLøserStub.besvarIgjen("ÅpneOppgaver")

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.antall = 0
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, INAKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `varsel dersom kall til gosys feilet`() {
        // Given:
        åpneOppgaverBehovLøser.oppslagFeilet = true

        // When:
        simulerFremTilOgMedGodkjenningsbehov()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_3.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `fjern varsel dersom kall til gosys ikke feiler lenger`() {
        // Given:
        åpneOppgaverBehovLøser.oppslagFeilet = true

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.oppslagFeilet = false
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_3.name, INAKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 0
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.antall = 1
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_1.name, AKTIV.name), Varsel(SB_RV_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `legger til varsel dersom oppslag feiler når vi har fått beskjed om at gosys har endret seg`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.oppslagFeilet = true
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        assertEquals(setOf(Varsel(SB_EX_3.name, AKTIV.name), Varsel(SB_RV_1.name, AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lagrer varsler når vi mottar ny aktivitet i aktivitetsloggen`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")

        // When:
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE"))
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)

        // Then:
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lagrer varsler når vi mottar flere ny aktivitet i aktivitetsloggen`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")

        // When:
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE"))
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_ANNEN_KODE"))
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)

        // Then:
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name), Varsel("EN_ANNEN_KODE", AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `lagrer flere varsler når vi mottar flere nye aktiviteter i samme aktivitetslogg`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")

        // When:
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE", "EN_ANNEN_KODE"))
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)

        // Then:
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name), Varsel("EN_ANNEN_KODE", AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `gammelt avviksvarsel erstattes av nytt avviksvarsel`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")

        // When:
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE"))
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE"))
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)

        // Then:
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name)), hentVarselkoder())
    }

    @Test
    fun `varsler for ulike vedtaksperioder går ikke i beina på hverandre`() {
        // Given:
        lagreVarseldefinisjon("EN_KODE")
        lagreVarseldefinisjon("EN_ANNEN_KODE")
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()

        // When:
        simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId1)
        simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId2)
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_KODE"), vedtaksperiodeId1)
        simulerPublisertAktivitetsloggNyAktivitetMelding(listOf("EN_ANNEN_KODE"), vedtaksperiodeId2)

        // Then:
        assertEquals(setOf(Varsel("EN_KODE", AKTIV.name)), hentVarselkoder(vedtaksperiodeId1))
        assertEquals(setOf(Varsel("EN_ANNEN_KODE", AKTIV.name)), hentVarselkoder(vedtaksperiodeId2))
    }
}
