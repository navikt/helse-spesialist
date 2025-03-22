package no.nav.helse.spesialist.e2etests

import no.nav.helse.modell.oppgave.Egenskap
import org.junit.jupiter.api.Test
import java.util.UUID

class ManglerInntektsmeldingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `legger til egenskap på oppgaven når varsel RV_IV_10 er til stede`() {
        sendSøknadSendt()
        assertIngenEtterspurteBehov()
        assertPersonEksisterer()
        val spleisBehandlingId = UUID.randomUUID()
        sendBehandlingOpprettet(spleisBehandlingId)
        assertIngenEtterspurteBehov()
        assertArbeidsgiverEksisterer(testPerson.orgnummer)
        assertVedtaksperiodeEksisterer(testPerson.vedtaksperiodeId1)

        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_IV_10"))
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingOpprettet()
        håndterVedtaksperiodeEndret()
        sendGodkjenningsbehov(spleisBehandlingId = spleisBehandlingId)
        håndterAvviksvurderingløsning()
        sendPersoninfoløsning()
        sendEnhetløsning()
        sendInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        sendArbeidsforholdløsning()
        sendEgenAnsattløsning()
        sendVergemålOgFullmaktløsning()
        sendÅpneGosysOppgaverløsning()
        sendRisikovurderingløsning()
        sendInntektløsning()

        // Then:
        assertHarOppgaveegenskap(Egenskap.MANGLER_IM)
    }
}
