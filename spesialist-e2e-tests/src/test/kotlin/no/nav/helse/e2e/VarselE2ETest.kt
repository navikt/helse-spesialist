package no.nav.helse.e2e

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.INAKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_3
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class VarselE2ETest : AbstractE2ETest() {

    @Test
    fun `varsel dersom kall til gosys feilet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 0, oppslagFeilet = true)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, AKTIV)
    }

    @Test
    fun `fjern varsel dersom kall til gosys ikke feiler lenger`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 0, oppslagFeilet = true)
        håndterRisikovurderingløsning()
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 0)
        assertVarsel(SB_EX_3, VEDTAKSPERIODE_ID, INAKTIV)
    }

    @Test
    fun `legger til varsel om gosys-oppgave når vi får beskjed om at gosys har fått oppgaver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 0)
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false)
        håndterInntektløsning()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 1)
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

    private fun assertVarsel(
        varselkode: Varselkode,
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
                            varselkode.name,
                            vedtaksperiodeId,
                            status.name,
                        ).map { it.int(1) }.asSingle,
                    ),
                )
            }
        assertEquals(1, antallVarsler)
    }

    private fun assertIngenVarsel(
        varselkode: Varselkode,
        vedtaksperiodeId: UUID,
    ) {
        val antallVarsler =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT count(*) FROM selve_varsel WHERE kode = ? AND vedtaksperiode_id = ?"
                requireNotNull(
                    session.run(
                        queryOf(
                            query,
                            varselkode.name,
                            vedtaksperiodeId,
                        ).map { it.int(1) }.asSingle,
                    ),
                )
            }
        assertEquals(0, antallVarsler)
    }
}
