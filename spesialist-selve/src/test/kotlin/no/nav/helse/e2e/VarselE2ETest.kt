package no.nav.helse.e2e

import AbstractE2ETestV2
import ToggleHelpers.disable
import ToggleHelpers.enable
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Område.Syk
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_BO_1
import no.nav.helse.modell.varsel.Varselkode.SB_BO_2
import no.nav.helse.modell.varsel.Varselkode.SB_BO_3
import no.nav.helse.modell.varsel.Varselkode.SB_BO_4
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.varsel.Varselkode.SB_IK_1
import no.nav.helse.modell.varsel.Varselkode.SB_RV_1
import no.nav.helse.modell.varsel.Varselkode.SB_RV_2
import no.nav.helse.modell.varsel.Varselkode.SB_RV_3
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarselE2ETest : AbstractE2ETestV2() {

    @BeforeEach
    fun før() {
        Toggle.VedtaksperiodeGenerasjoner.enable()
    }

    @AfterEach
    fun etter() {
        Toggle.VedtaksperiodeGenerasjoner.disable()
    }

    @Test
    fun `ingen varsel`() {
        fremTilSaksbehandleroppgave()
        assertIngenVarsel(SB_BO_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_BO_2, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_BO_3, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_BO_4, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_IK_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_RV_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_RV_3, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `varsel om beslutteroppgave ved varsel om lovvalg og medlemsskap`() {
        fremTilSaksbehandleroppgave(regelverksvarsler = listOf("Vurder lovvalg og medlemskap"))

        assertVarsel(SB_BO_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om beslutteroppgave ved overstyring av dager`() {
        fremTilSaksbehandleroppgave()
        håndterOverstyrTidslinje()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()

        assertVarsel(SB_BO_2, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om beslutteroppgave ved overstyring av inntekt`() {
        fremTilSaksbehandleroppgave()
        håndterOverstyrInntekt()
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()

        assertVarsel(SB_BO_3, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om beslutteroppgave ved overstyring av arbeidsforhold`() {
        fremTilSaksbehandleroppgave(andreArbeidsforhold = listOf(Testdata.ORGNR_GHOST))
        håndterOverstyrArbeidsforhold(organisasjonsnummer = Testdata.ORGNR_GHOST)
        håndterEgenansattløsning()
        håndterVergemålløsning()
        håndterDigitalKontaktinformasjonløsning()
        håndterÅpneOppgaverløsning()

        assertVarsel(SB_BO_4, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om vergemål`() {
        fremTilSaksbehandleroppgave(fullmakter = listOf(Fullmakt(områder = listOf(Syk), LocalDate.MIN, LocalDate.MAX)))
        assertVarsel(SB_IK_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `varsel om faresignaler ved risikovurdering`() {
        fremTilSaksbehandleroppgave(risikofunn = listOf(Risikofunn(listOf("EN_KATEGORI"), "EN_BESKRIVELSE", false)))
        assertVarsel(SB_RV_1, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `ingen varsler dersom ingen åpne oppgaver eller oppslagsfeil`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning()
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `lager varsel ved åpne gosys-oppgaver`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
        assertWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `fjern varsel om gosys-oppgave dersom det ikke finnes gosys-oppgave lenger`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 0)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
        assertInaktivWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 0)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antall = 1)
        assertVarsel(SB_EX_1, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_3, VEDTAKSPERIODE_ID)
        assertWarning("Det finnes åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel om manglende gosys-info`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        håndterRisikovurderingløsning()
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertWarning("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `legger til varsel dersom oppslag feiler når vi har fått beskjed om at gosys har endret seg`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(oppslagFeilet = false)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(oppslagFeilet = true)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
        assertIngenVarsel(SB_EX_1, VEDTAKSPERIODE_ID)
        assertWarning("Kunne ikke sjekke åpne oppgaver på sykepenger i Gosys", VEDTAKSPERIODE_ID)
    }

    @Test
    fun `varsel dersom teknisk feil ved sjekk av 8-4-knappetrykk`() {
        fremTilSaksbehandleroppgave(
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
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(listOf("8-4", "EN_ANNEN_KATEGORI"), "EN_BESKRIVELSE", false)
            )
        )
        assertIngenVarsel(SB_RV_3, VEDTAKSPERIODE_ID)
        assertVarsel(SB_RV_2, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `godkjenner varsler når periode blir godkjent`() {
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(listOf("8-4", "EN_ANNEN_KATEGORI"), "EN_BESKRIVELSE", false)
            ),
            fullmakter = listOf(Fullmakt(områder = listOf(Syk), LocalDate.MIN, LocalDate.MAX))
        )
        håndterSaksbehandlerløsning()
        assertVarsel(SB_IK_1, VEDTAKSPERIODE_ID, GODKJENT)
        assertVarsel(SB_RV_2, VEDTAKSPERIODE_ID, GODKJENT)
    }

    @Test
    fun `avviser varsler når periode blir avvist`() {
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(listOf("8-4", "EN_ANNEN_KATEGORI"), "EN_BESKRIVELSE", false)
            ),
            fullmakter = listOf(Fullmakt(områder = listOf(Syk), LocalDate.MIN, LocalDate.MAX))
        )
        håndterSaksbehandlerløsning(godkjent = false)
        assertVarsel(SB_IK_1, VEDTAKSPERIODE_ID, AVVIST)
        assertVarsel(SB_RV_2, VEDTAKSPERIODE_ID, AVVIST)
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

    private fun assertWarning(forventet: String, vedtaksperiodeId: UUID) {
        Assertions.assertTrue(sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId) and (inaktiv_fra is null)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("melding") }.asList
            )
        }.contains(forventet))
    }

    private fun assertInaktivWarning(forventet: String, vedtaksperiodeId: UUID) {
        Assertions.assertTrue(sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId) and (inaktiv_fra is not null)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("melding") }.asList
            )
        }.contains(forventet))
    }
}