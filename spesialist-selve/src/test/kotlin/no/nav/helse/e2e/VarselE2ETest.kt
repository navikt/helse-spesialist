package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Område.Syk
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.varsel.Varselkode.SB_IK_1
import no.nav.helse.modell.varsel.Varselkode.SB_RV_1
import no.nav.helse.modell.varsel.Varselkode.SB_RV_2
import no.nav.helse.modell.varsel.Varselkode.SB_RV_3
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselE2ETest : AbstractE2ETest() {

    @Test
    fun `ingen varsel`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        assertIngenVarsel(SB_IK_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_RV_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_RV_3, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `varsel om vergemål`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(fullmakter = listOf(Fullmakt(områder = listOf(Syk), LocalDate.MIN, LocalDate.MAX)))
        assertVarsel(SB_IK_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(risikofunn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE", false)))
        assertVarsel(SB_RV_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `ingen varsler dersom ingen åpne oppgaver eller oppslagsfeil`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning()
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `lager ikke duplikatvarsel ved åpne gosys-oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 0)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, INAKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `varsel dersom kall til gosys feilet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 0, oppslagFeilet = true)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `fjern varsel dersom kall til gosys ikke feiler lenger`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 0, oppslagFeilet = true)
        håndterRisikovurderingløsning()
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 0)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, INAKTIV)
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 0)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel om manglende gosys-info`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        håndterRisikovurderingløsning()
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel dersom oppslag feiler når vi har fått beskjed om at gosys har endret seg`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(oppslagFeilet = false)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `varsel dersom teknisk feil ved sjekk av 8-4-knappetrykk`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            risikofunn = listOf(
                Risikofunn(
                    listOf("8-4", "EN_ANNEN_KATEGORI"),
                    "Klarte ikke gjøre automatisk 8-4-vurdering p.g.a. teknisk feil. Kan godkjennes hvis alt ser greit ut.",
                    false
                )
            )
        )
        assertVarsel(SB_RV_3, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel ved manuell stans av automatisk behandling - 8-4`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            risikofunn = listOf(
                Risikofunn(listOf("8-4", "EN_ANNEN_KATEGORI"), "EN_BESKRIVELSE", false)
            )
        )
        assertIngenVarsel(SB_RV_3, VEDTAKSPERIODE_ID)
        assertVarsel(SB_RV_2, VEDTAKSPERIODE_ID, AKTIV)
    }

    private fun assertVarsel(varselkode: Varselkode, vedtaksperiodeId: UUID, status: Varsel.Status) {
        val antallVarsler = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ? AND status = ?"
            requireNotNull(
                session.run(
                    queryOf(
                        query,
                        varselkode.name,
                        vedtaksperiodeId,
                        status.name
                    ).map { it.int(1) }.asSingle
                )
            )
        }
        assertEquals(1, antallVarsler)
    }

    private fun assertIngenVarsel(varselkode: Varselkode, vedtaksperiodeId: UUID) {
        val antallVarsler = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ?"
            requireNotNull(
                session.run(
                    queryOf(
                        query,
                        varselkode.name,
                        vedtaksperiodeId,
                    ).map { it.int(1) }.asSingle
                )
            )
        }
        assertEquals(0, antallVarsler)
    }
}
