package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.modell.varsel.Varsel.Status
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_4
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretE2ETest : AbstractE2ETest() {

    @Test
    fun `ingen varsler dersom ingen åpne oppgaver eller oppslagsfeil`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning()
        assertIngenVarsel(SB_EX_4, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_4, VEDTAKSPERIODE_ID)
        assertWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 0)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, INAKTIV)
        assertIngenVarsel(SB_EX_4, VEDTAKSPERIODE_ID)
        assertInaktivWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning(antall = 0)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_4, VEDTAKSPERIODE_ID)
        assertWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel om manglende gosys-info`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        håndterRisikovurderingløsning()
        assertVarsel(SB_EX_4, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertWarning("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel dersom oppslag feiler når vi har fått beskjed om at gosys har endret seg`() {
        fremTilÅpneOppgaverBehov()
        håndterÅpneOppgaverløsning(oppslagFeilet = false)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        assertVarsel(SB_EX_4, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertWarning("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    private fun assertVarsel(varselkode: Varselkode, vedtaksperiodeId: UUID, status: Status) {
        val antallVarsler = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ? AND status = ?"
            requireNotNull(
                it.run(
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
        val antallVarsler = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ?"
            requireNotNull(
                it.run(
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

    private fun fremTilÅpneOppgaverBehov() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterGodkjenningsbehov()
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
    }
}