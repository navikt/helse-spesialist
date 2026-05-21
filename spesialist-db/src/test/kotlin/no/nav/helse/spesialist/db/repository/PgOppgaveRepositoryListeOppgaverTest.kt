package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals

@Isolated
class PgOppgaveRepositoryListeOppgaverTest : AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)

    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE oppgave CASCADE")
        dbQuery.execute("TRUNCATE selve_varsel CASCADE")
    }

    @Test
    fun `ekskluderer oppgaver uten varsler`() {
        opprettOppgaveUtenVarsler()

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `inkluderer oppgave med tillatt varsel`() {
        val (_, _, oppgave) = opprettOppgaveMedVarsel(
            varselkode = "SB_EX_1", // et vilkårlig varsel som IKKE er ekskludert
        )

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `ekskluderer oppgave med ekskludert varsel RV_MV_3`() {
        opprettOppgaveMedVarsel(varselkode = "RV_MV_3")

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `ekskluderer oppgave som har ett tillatt varsel og ett ekskludert varsel`() {
        val (_, behandling, _) = opprettOppgaveMedVarsel(varselkode = "SB_EX_1")
        opprettVarsel(behandling, "RV_MV_3") // Legg til ekskludert varsel

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `ekskluderer tildelt oppgave`() {
        val saksbehandler = lagSaksbehandler()
        val (_, _, oppgave) = opprettOppgaveMedVarsel(varselkode = "SB_EX_1")
        oppgave.tildelOgLagre(saksbehandler)

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `ekskluderer oppgave med PÅ_VENT egenskap`() {
        opprettOppgaveMedVarsel(
            varselkode = "SB_EX_1",
            egenskaper = setOf(Egenskap.SØKNAD, Egenskap.FORSTEGANGSBEHANDLING, Egenskap.PÅ_VENT),
        )

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `ekskluderer revurderinger`() {
        opprettOppgaveMedVarsel(
            varselkode = "SB_EX_1",
            egenskaper = setOf(Egenskap.REVURDERING),
        )

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `inkluderer oppgave med FORLENGELSE periodetype`() {
        val (_, _, oppgave) = opprettOppgaveMedVarsel(
            varselkode = "SB_EX_1",
            egenskaper = setOf(Egenskap.SØKNAD, Egenskap.FORLENGELSE),
            periodetype = Periodetype.FORLENGELSE,
        )

        val resultat = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }


    @Test
    fun `paginering fungerer korrekt`() {
        repeat(15) {
            opprettOppgaveMedVarsel(varselkode = "SB_EX_$it")
        }

        val side1 = repository.finnListeOppgaveProjeksjoner(sidetall = 1, sidestørrelse = 10)
        val side2 = repository.finnListeOppgaveProjeksjoner(sidetall = 2, sidestørrelse = 10)

        assertEquals(15, side1.totaltAntall)
        assertEquals(10, side1.elementer.size)
        assertEquals(15, side2.totaltAntall)
        assertEquals(5, side2.elementer.size)
    }

    private fun opprettOppgaveMedVarsel(
        varselkode: String,
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD, Egenskap.FORSTEGANGSBEHANDLING),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
    ): Triple<Vedtaksperiode, Behandling, Oppgave> {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver, periodetype = periodetype)
        val behandling = opprettBehandling(vedtaksperiode)
        val oppgave = opprettOppgave(vedtaksperiode, behandling, egenskaper = egenskaper)
        opprettVarsel(behandling, varselkode)
        return Triple(vedtaksperiode, behandling, oppgave)
    }

    private fun opprettOppgaveUtenVarsler(
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD, Egenskap.FORSTEGANGSBEHANDLING),
    ): Oppgave {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        return opprettOppgave(vedtaksperiode, behandling, egenskaper = egenskaper)
    }
}

