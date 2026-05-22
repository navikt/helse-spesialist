package no.nav.helse.spesialist.db.repository

import java.time.LocalDate
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Isolated
class PgOppgaveRepositoryFiltreringTest : AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)

    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE oppgave CASCADE")
        dbQuery.execute("TRUNCATE totrinnsvurdering CASCADE")
    }

    @Test
    fun `filtrerer på minstEnAvEgenskapene - inkluderer oppgave med matchende egenskap`() {
        val oppgave = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.HASTER))

        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(setOf(Egenskap.HASTER)),
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på minstEnAvEgenskapene - ekskluderer oppgave uten matchende egenskap`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD))

        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(setOf(Egenskap.HASTER)),
        )

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `filtrerer på minstEnAvEgenskapene - matcher minst én av flere egenskaper`() {
        val oppgave = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.RETUR))

        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(setOf(Egenskap.HASTER, Egenskap.RETUR)),
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på minstEnAvEgenskapene - flere grupper fungerer som AND`() {
        val oppgaveMedBegge = nyOppgaveForNyPerson(
            oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.HASTER, Egenskap.RETUR),
        )
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.HASTER))

        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(
                setOf(Egenskap.HASTER),
                setOf(Egenskap.RETUR),
            ),
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgaveMedBegge.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på ingenAvEgenskapene - ekskluderer oppgave med ekskludert egenskap`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.FORTROLIG_ADRESSE))

        val resultat = finnOppgaveProjeksjoner(
            ingenAvEgenskapene = setOf(Egenskap.FORTROLIG_ADRESSE),
        )

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `filtrerer på ingenAvEgenskapene - inkluderer oppgave uten ekskludert egenskap`() {
        val oppgave = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD))

        val resultat = finnOppgaveProjeksjoner(
            ingenAvEgenskapene = setOf(Egenskap.FORTROLIG_ADRESSE),
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på erTildelt = true - kun tildelte oppgaver`() {
        val saksbehandler = lagSaksbehandler()
        val tildeltOppgave = nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        nyOppgaveForNyPerson() // Ikke tildelt

        val resultat = finnOppgaveProjeksjoner(erTildelt = true)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(tildeltOppgave.id.value, resultat.elementer.single().id)
        assertNotNull(resultat.elementer.single().tildeltTilOid)
    }

    @Test
    fun `filtrerer på erTildelt = false - kun utildelte oppgaver`() {
        val saksbehandler = lagSaksbehandler()
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler)
        val utildeltOppgave = nyOppgaveForNyPerson()

        val resultat = finnOppgaveProjeksjoner(erTildelt = false)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(utildeltOppgave.id.value, resultat.elementer.single().id)
        assertNull(resultat.elementer.single().tildeltTilOid)
    }

    @Test
    fun `filtrerer på tildeltTilOid - kun oppgaver tildelt spesifikk saksbehandler`() {
        val saksbehandler1 = lagSaksbehandler()
        val saksbehandler2 = lagSaksbehandler()
        val oppgaveTilSaksbehandler1 = nyOppgaveForNyPerson().tildelOgLagre(saksbehandler1)
        nyOppgaveForNyPerson().tildelOgLagre(saksbehandler2)

        val resultat = finnOppgaveProjeksjoner(tildeltTilOid = saksbehandler1.id)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgaveTilSaksbehandler1.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på erPåVent = true - kun oppgaver på vent`() {
        val saksbehandler = lagSaksbehandler()
        val påVentOppgave = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))
            .tildelOgLagre(saksbehandler)
            .leggPåVentOgLagre(saksbehandler)
        nyOppgaveForNyPerson()

        val resultat = finnOppgaveProjeksjoner(erPåVent = true)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(påVentOppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på erPåVent = false - kun oppgaver som ikke er på vent`() {
        val saksbehandler = lagSaksbehandler()
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))
            .tildelOgLagre(saksbehandler)
            .leggPåVentOgLagre(saksbehandler)
        val ikkePåVentOppgave = nyOppgaveForNyPerson()

        val resultat = finnOppgaveProjeksjoner(erPåVent = false)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(ikkePåVentOppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `filtrerer på ikkeSendtTilBeslutterAvOid - ekskluderer beslutter-oppgaver sendt av saksbehandler`() {
        val saksbehandler = opprettSaksbehandler()
        val beslutter = opprettSaksbehandler()

        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        val oppgave = opprettOppgave(vedtaksperiode, behandling, egenskaper = setOf(Egenskap.SØKNAD, Egenskap.BESLUTTER))
            .tildelOgLagre(beslutter, brukerroller = setOf(Brukerrolle.Beslutter))

        nyTotrinnsvurdering(person.id.value, oppgave)
            .sendTilBeslutterOgLagre(saksbehandler)

        val resultat = finnOppgaveProjeksjoner(
            ikkeSendtTilBeslutterAvOid = saksbehandler.id,
        )

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `filtrerer på ikkeSendtTilBeslutterAvOid - inkluderer beslutter-oppgaver sendt av andre`() {
        val saksbehandler1 = opprettSaksbehandler()
        val saksbehandler2 = opprettSaksbehandler()
        val beslutter = opprettSaksbehandler()

        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        val oppgave = opprettOppgave(vedtaksperiode, behandling, egenskaper = setOf(Egenskap.SØKNAD, Egenskap.BESLUTTER))
            .tildelOgLagre(beslutter, brukerroller = setOf(Brukerrolle.Beslutter))

        nyTotrinnsvurdering(person.id.value, oppgave)
            .sendTilBeslutterOgLagre(saksbehandler1)

        val resultat = finnOppgaveProjeksjoner(
            ikkeSendtTilBeslutterAvOid = saksbehandler2.id,
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `kombinerer flere filtreringsparametere`() {
        val saksbehandler = lagSaksbehandler()

        // Oppgave som matcher alle kriterier
        val matchendeOppgave = nyOppgaveForNyPerson(
            oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.HASTER),
        ).tildelOgLagre(saksbehandler)

        // Oppgave som ikke matcher (mangler HASTER)
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD))
            .tildelOgLagre(saksbehandler)

        // Oppgave som ikke matcher (ikke tildelt)
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.HASTER))

        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(setOf(Egenskap.HASTER)),
            erTildelt = true,
        )

        assertEquals(1, resultat.totaltAntall)
        assertEquals(matchendeOppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `returnerer tom liste når ingen oppgaver matcher`() {
        val resultat = finnOppgaveProjeksjoner(
            minstEnAvEgenskapene = listOf(setOf(Egenskap.HASTER)),
        )

        assertEquals(0, resultat.totaltAntall)
        assertEquals(emptyList(), resultat.elementer)
    }

    @Test
    fun `returnerer påVentId for oppgaver på vent`() {
        val saksbehandler = lagSaksbehandler()
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))
            .tildelOgLagre(saksbehandler)
            .leggPåVentOgLagre(saksbehandler)

        val resultat = finnOppgaveProjeksjoner(erPåVent = true)

        assertEquals(1, resultat.totaltAntall)
        assertNotNull(resultat.elementer.single().påVentId)
    }

    @Test
    fun `ekskluderer oppgave med ekskludert varsel`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        opprettOppgave(vedtaksperiode, behandling)
        opprettVarsel(behandling, "RV_MV_3")

        val resultat = finnOppgaveProjeksjoner(ekskluderVarsler = setOf("RV_MV_3"))

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `inkluderer oppgave uten ekskludert varsel`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        opprettVarsel(behandling, "SB_EX_1")

        val resultat = finnOppgaveProjeksjoner(ekskluderVarsler = setOf("RV_MV_3"))

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `ekskluderer oppgave som er nyere enn behandlingOpprettetTom`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        settBehandlingOpprettet(behandling, LocalDate.now())
        opprettOppgave(vedtaksperiode, behandling)

        val resultat = finnOppgaveProjeksjoner(behandlingOpprettetTom = LocalDate.now().minusDays(1))

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `inkluderer oppgave der behandlingen er opprettet samme dag som behandlingOpprettetTom`() {
        val dato = LocalDate.now()

        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        settBehandlingOpprettet(behandling, dato)
        val oppgave = opprettOppgave(vedtaksperiode, behandling)

        val resultat = finnOppgaveProjeksjoner(behandlingOpprettetTom = dato)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `ekskluderer oppgave som er eldre enn behandlingOpprettetFom`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        settBehandlingOpprettet(behandling, LocalDate.now().minusDays(2))
        opprettOppgave(vedtaksperiode, behandling)

        val resultat = finnOppgaveProjeksjoner(behandlingOpprettetFom = LocalDate.now())

        assertEquals(0, resultat.totaltAntall)
    }

    @Test
    fun `inkluderer oppgave der behandlingen er opprettet samme dag som behandlingOpprettetFom`() {
        val dato = LocalDate.now()

        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        settBehandlingOpprettet(behandling, dato)
        val oppgave = opprettOppgave(vedtaksperiode, behandling)

        val resultat = finnOppgaveProjeksjoner(behandlingOpprettetFom = dato)

        assertEquals(1, resultat.totaltAntall)
        assertEquals(oppgave.id.value, resultat.elementer.single().id)
    }

    @Test
    fun `ekskluderer oppgave uten varsler når ekskluderVarsler er satt`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        opprettOppgave(vedtaksperiode, behandling) // ingen varsler

        val resultat = finnOppgaveProjeksjoner(ekskluderVarsler = setOf("RV_MV_3"))

        assertEquals(0, resultat.totaltAntall)
    }

    private fun settBehandlingOpprettet(behandling: Behandling, dato: LocalDate) {
        val tidspunkt = dato.atStartOfDay()
        dbQuery.update(
            "UPDATE behandling SET opprettet_tidspunkt = :tidspunkt WHERE unik_id = :id",
            "tidspunkt" to tidspunkt,
            "id" to behandling.id.value,
        )
    }

    private fun finnOppgaveProjeksjoner(
        minstEnAvEgenskapene: List<Set<Egenskap>> = emptyList(),
        ingenAvEgenskapene: Set<Egenskap> = emptySet(),
        erTildelt: Boolean? = null,
        tildeltTilOid: no.nav.helse.spesialist.domain.SaksbehandlerOid? = null,
        erPåVent: Boolean? = null,
        ikkeSendtTilBeslutterAvOid: no.nav.helse.spesialist.domain.SaksbehandlerOid? = null,
        sidetall: Int = 1,
        sidestørrelse: Int = 10,
        ekskluderVarsler: Set<String> = emptySet(),
        behandlingOpprettetFom: LocalDate? = null,
        behandlingOpprettetTom: LocalDate? = null,
    ) = repository.finnOppgaveProjeksjoner(
        minstEnAvEgenskapene = minstEnAvEgenskapene,
        ingenAvEgenskapene = ingenAvEgenskapene,
        erTildelt = erTildelt,
        tildeltTilOid = tildeltTilOid,
        erPåVent = erPåVent,
        ikkeSendtTilBeslutterAvOid = ikkeSendtTilBeslutterAvOid,
        sorterPå = SorteringsnøkkelForDatabase.OPPRETTET,
        sorteringsrekkefølge = Sorteringsrekkefølge.STIGENDE,
        sidetall = sidetall,
        sidestørrelse = sidestørrelse,
        ekskluderVarsler = ekskluderVarsler,
        behandlingOpprettetFom = behandlingOpprettetFom,
        behandlingOpprettetTom = behandlingOpprettetTom,
    )
}

