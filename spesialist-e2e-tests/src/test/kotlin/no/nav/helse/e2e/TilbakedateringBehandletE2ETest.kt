package no.nav.helse.e2e

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.person.vedtaksperiode.Periode
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import java.util.UUID

class TilbakedateringBehandletE2ETest : AbstractE2ETest() {
    @Test
    fun `fatter automatisk vedtak dersom åpen oppgave får inn godkjent tilbakedatering`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(
            regelverksvarsler = listOf("RV_SØ_3"),
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()

        assertGodkjenningsbehovIkkeBesvart()
        val oppgaveId = inspektør.oppgaveId()
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

        assertGodkjenningsbehovIkkeBesvart()
        val oppgaveId = inspektør.oppgaveId()
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
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = listOf("RV_SØ_3"))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterInntektløsning()
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.januar, 31.januar)))
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
    }

    @Test
    fun `fjern varsel om tilbakedatering på alle overlappende perioder i sykefraværstilfellet for ok-sykmelding`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = listOf("RV_SØ_3"))
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterInntektløsning()

        val vedtaksperiodeId2 = UUID.randomUUID()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling(vedtaksperiodeId = vedtaksperiodeId2)
        håndterAktivitetsloggNyAktivitet(
            varselkoder = listOf("RV_SØ_3"),
            vedtaksperiodeId = vedtaksperiodeId2,
        )

        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)
        assertVarsel("RV_SØ_3", vedtaksperiodeId2, Varsel.Status.AKTIV)

        håndterTilbakedateringBehandlet(perioder = listOf(Periode(1.januar, 31.januar)))
        assertVarsel("RV_SØ_3", VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
        assertVarsel("RV_SØ_3", vedtaksperiodeId2, Varsel.Status.INAKTIV)
    }

    @Test
    fun `Varselet skal bli liggende om det kommer en delvis overlappende godkjent tilbakedatering`() {
        val RV_SØ_3 = "RV_SØ_3"
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = listOf(RV_SØ_3))
        assertVarsel(RV_SØ_3, VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()
        håndterInntektløsning()

        // Ny sykmelding kommer til Nav. Per i dag tolker sparkel-tilbakedatert det som en sykmelding som er
        // tilbakedatert og godkjent hvis den ikke har merknader, og sender:
        håndterTilbakedateringBehandlet(perioder = listOf(Periode(fom = 25.januar, tom = 5.februar)))

        assertThrows<AssertionFailedError> {
            // TODO: Denne asserten skal ikke throwe
            assertVarsel(RV_SØ_3, VEDTAKSPERIODE_ID, Varsel.Status.AKTIV)
        }
        assertDoesNotThrow {
            // Dette er IKKE sånn vi vil ha det:
            assertVarsel(RV_SØ_3, VEDTAKSPERIODE_ID, Varsel.Status.INAKTIV)
        }
    }

    private fun assertVarsel(
        varselkode: String,
        vedtaksperiodeId: UUID,
        status: Varsel.Status,
    ) {
        val antallVarsler =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ? AND status = ?"
                requireNotNull(
                    session.run(
                        queryOf(
                            query,
                            varselkode,
                            vedtaksperiodeId,
                            status.name,
                        ).map { it.int(1) }.asSingle,
                    ),
                )
            }
        Assertions.assertEquals(1, antallVarsler)
    }
}
