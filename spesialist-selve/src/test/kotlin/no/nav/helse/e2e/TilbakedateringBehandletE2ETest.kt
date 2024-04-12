package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class TilbakedateringBehandletE2ETest : AbstractE2ETest() {
    @Test
    fun `fatter automatisk vedtak dersom åpen oppgave får inn godkjent tilbakedatering`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(
            regelverksvarsler = listOf("RV_SØ_3"),
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()

        assertGodkjenningsbehovIkkeBesvart()
        val oppgaveId = inspektør.oppgaveId().toInt()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
        assertHarOppgaveegenskap(oppgaveId, Egenskap.TILBAKEDATERT)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.januar, 31.januar)))
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
        assertHarIkkeOppgaveegenskap(oppgaveId, Egenskap.TILBAKEDATERT)
    }

    @Test
    fun `fatter ikke automatisk vedtak dersom ingen av periodene i sykmeldingen er innenfor vedtaksperiodens fom og tom`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(
            regelverksvarsler = listOf("RV_SØ_3"),
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()

        assertGodkjenningsbehovIkkeBesvart()
        val oppgaveId = inspektør.oppgaveId().toInt()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
        assertHarOppgaveegenskap(oppgaveId, Egenskap.TILBAKEDATERT)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.februar, 28.februar)))
        assertGodkjenningsbehovIkkeBesvart()
        assertHarOppgaveegenskap(oppgaveId, Egenskap.TILBAKEDATERT)
    }

    @Test
    fun `fjern varsel om tilbakedatering dersom tilbakedatert sykmelding er godkjent`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = listOf("RV_SØ_3"),)
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        håndterInntektløsning()
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.januar, 31.januar)))
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
    }

    @Test
    fun `fjern varsel om tilbakedatering på alle overlappende perioder i sykefraværstilfellet for ok-sykmelding`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = listOf("RV_SØ_3"),)
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()
        håndterInntektløsning()

        val vedtaksperiodeId2 = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId2)
        håndterAktivitetsloggNyAktivitet(varselkoder = listOf("RV_SØ_3"), vedtaksperiodeId = vedtaksperiodeId2
        )

        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)
        assertVarsel("RV_SØ_3", vedtaksperiodeId2, Varsel.Status.AKTIV)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.januar, 31.januar)))
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
        assertVarsel("RV_SØ_3", vedtaksperiodeId2, Varsel.Status.INAKTIV)
    }

    private fun assertVarsel(varselkode: String, vedtaksperiodeId: UUID, status: Varsel.Status) {
        val antallVarsler = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ? AND status = ?"
            requireNotNull(
                session.run(
                    queryOf(
                        query,
                        varselkode,
                        vedtaksperiodeId,
                        status.name
                    ).map { it.int(1) }.asSingle
                )
            )
        }
        Assertions.assertEquals(1, antallVarsler)
    }
}
