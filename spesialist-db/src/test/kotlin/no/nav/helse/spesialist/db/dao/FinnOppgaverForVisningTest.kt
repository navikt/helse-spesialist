package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.EgenskapForDatabase.DELVIS_REFUSJON
import no.nav.helse.db.EgenskapForDatabase.EN_ARBEIDSGIVER
import no.nav.helse.db.EgenskapForDatabase.FLERE_ARBEIDSGIVERE
import no.nav.helse.db.EgenskapForDatabase.FORSTEGANGSBEHANDLING
import no.nav.helse.db.EgenskapForDatabase.HASTER
import no.nav.helse.db.EgenskapForDatabase.PÅ_VENT
import no.nav.helse.db.EgenskapForDatabase.REVURDERING
import no.nav.helse.db.EgenskapForDatabase.RISK_QA
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.db.EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
import no.nav.helse.db.EgenskapForDatabase.UTLAND
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.Kategori
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.TestMelding
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.util.UUID

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class FinnOppgaverForVisningTest : AbstractDBIntegrationTest() {

    private val legacySaksbehandler = nyLegacySaksbehandler()

    @BeforeEach
    fun setupDaoTest() {
        val testhendelse = TestMelding(HENDELSE_ID, UUID.randomUUID(), FNR)
        godkjenningsbehov(testhendelse.id)
        CommandContext(UUID.randomUUID()).opprett(commandContextDao, testhendelse.id)
        dbQuery.execute("truncate oppgave restart identity cascade")
    }

    @Test
    fun `Finn oppgave for visning`() {
        val aktørId = lagAktørId()
        val fornavn = lagFornavn()
        val mellomnavn = lagEtternavn()
        val etternavn = lagEtternavn()
        val oppgave = nyOppgaveForNyPerson(
            aktørId = aktørId,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn
        ).tildelOgLagre(legacySaksbehandler)

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(1, oppgaver.size)
        val førsteOppgave = oppgaver.first()
        assertEquals(oppgave.id, førsteOppgave.id)
        assertEquals(aktørId, førsteOppgave.aktørId)
        assertEquals(oppgave.egenskaper.map { it.name }, førsteOppgave.egenskaper.map { it.name })
        assertEquals(fornavn, førsteOppgave.navn.fornavn)
        assertEquals(mellomnavn, førsteOppgave.navn.mellomnavn)
        assertEquals(etternavn, førsteOppgave.navn.etternavn)
        assertEquals(false, førsteOppgave.påVent)
        assertEquals(
            SaksbehandlerFraDatabase(
                epostadresse = legacySaksbehandler.epostadresse,
                oid = legacySaksbehandler.oid,
                navn = legacySaksbehandler.navn,
                ident = legacySaksbehandler.ident(),
            ),
            førsteOppgave.tildelt,
        )
        assertEquals(oppgave.vedtaksperiodeId, førsteOppgave.vedtaksperiodeId)
    }

    @Test
    fun `Finn oppgave som ligger på vent for visning`() {
        val aktørId = lagAktørId()
        val fornavn = lagFornavn()
        val mellomnavn = lagEtternavn()
        val etternavn = lagEtternavn()
        val oppgave = nyOppgaveForNyPerson(
            aktørId = aktørId,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn
        ).tildelOgLagre(legacySaksbehandler)
            .leggPåVentOgLagre(legacySaksbehandler)

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(1, oppgaver.size)
        val førsteOppgave = oppgaver.first()
        assertEquals(oppgave.id, førsteOppgave.id)
        assertEquals(aktørId, førsteOppgave.aktørId)
        assertEquals(oppgave.egenskaper.map { it.name }, førsteOppgave.egenskaper.map { it.name })
        assertEquals(fornavn, førsteOppgave.navn.fornavn)
        assertEquals(mellomnavn, førsteOppgave.navn.mellomnavn)
        assertEquals(etternavn, førsteOppgave.navn.etternavn)
        assertEquals(true, førsteOppgave.påVent)
        assertEquals(
            SaksbehandlerFraDatabase(
                epostadresse = legacySaksbehandler.epostadresse,
                oid = legacySaksbehandler.oid,
                navn = legacySaksbehandler.navn,
                ident = legacySaksbehandler.ident(),
            ),
            førsteOppgave.tildelt,
        )
        assertEquals(oppgave.vedtaksperiodeId, førsteOppgave.vedtaksperiodeId)
        assertNotNull(førsteOppgave.paVentInfo)
        assertEquals(legacySaksbehandler.ident(), førsteOppgave.paVentInfo?.saksbehandler)
        assertEquals(1, førsteOppgave.paVentInfo?.kommentarer?.size)
    }

    @Test
    fun `Finn oppgaver med bestemte egenskaper`() {
        nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.RISK_QA, FORTROLIG_ADRESSE))
        val oppgave3 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.RISK_QA))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Kategori.Ukategorisert to listOf(RISK_QA),
                        Kategori.Oppgavetype to listOf(SØKNAD),
                    ),
            )
        assertEquals(2, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave3, oppgave2)
    }

    @Test
    fun `Finner ikke oppgaver som ikke har alle de gitte egenskapene`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD))
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.RISK_QA))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Kategori.Ukategorisert to listOf(RISK_QA),
                        Kategori.Oppgavetype to listOf(SØKNAD),
                    ),
            )
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Finn oppgaver for visning med offset og limit`() {
        nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
        nyOppgaveForNyPerson()

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), 1, 2)
        assertEquals(2, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave3, oppgave2)
    }

    @Test
    fun `Tar kun med oppgaver som avventer saksbehandler`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        nyOppgaveForNyPerson()
            .avventSystemOgLagre(legacySaksbehandler)
        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(2, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave1, oppgave2)
    }

    @Test
    fun `Sorterer oppgaver på opprettet stigende`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave1.id, oppgave2.id, oppgave3.id), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprettet fallende`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave3.id, oppgave2.id, oppgave1.id), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprinneligSøknadsdato stigende`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave1.id, oppgave2.id, oppgave3.id), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprinneligSøknadsdato fallende`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave3.id, oppgave2.id, oppgave1.id), oppgaver.map { it.id })
    }

    @Test
    fun `Finn oppgaver med bestemte egenskaper -- selvstendig`() {
        nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson(organisasjonsnummer = "SELVSTENDIG", oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Kategori.Inntektsforhold to listOf(EgenskapForDatabase.SELVSTENDIG_NÆRINGSDRIVENDE),
                        Kategori.Oppgavetype to listOf(SØKNAD),
                    ),
            )
        assertEquals(1, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave2)
    }

    @Test
    fun `Sorterer oppgaver på tildeling stigende`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(nyLegacySaksbehandler(navn = "Beate Beatesen"))
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
            .tildelOgLagre(nyLegacySaksbehandler(navn = "Arne Arnesen"))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave3.id, oppgave1.id, oppgave2.id), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på tildeling fallende`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(nyLegacySaksbehandler(navn = "Arne Arnesen"))
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
            .tildelOgLagre(nyLegacySaksbehandler(navn = "Beate Beatesen"))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgave3.id, oppgave1.id, oppgave2.id), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på tildeling stigende først, deretter opprettet fallende`() {
        val legacySaksbehandler1 = nyLegacySaksbehandler(navn = "Arne Arnesen")
        val legacySaksbehandler2 = nyLegacySaksbehandler(navn = "Beate Beatesen")
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler1)
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler2)
        val oppgave4 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler2)
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, true),
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, false),
                    ),
            )
        assertEquals(4, oppgaver.size)
        assertEquals(listOf(oppgave1.id, oppgave4.id, oppgave3.id, oppgave2.id), oppgaver.map { it.id })
    }

    @Test
    fun `Tar kun med oppgaver som saksbehandler har tilgang til`() {
        val oppgave1 = nyOppgaveForNyPerson()
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(BESLUTTER))
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.RISK_QA, FORTROLIG_ADRESSE))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("BESLUTTER", "RISK_QA"),
                UUID.randomUUID(),
            )
        assertEquals(1, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave1)
    }

    @Test
    fun `Ekskluderer ukategoriserte egenskaper`() {
        val oppgave1 = nyOppgaveForNyPerson()
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(BESLUTTER))
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.RISK_QA, FORTROLIG_ADRESSE))
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.UTLAND, Egenskap.HASTER))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("BESLUTTER", "RISK_QA") + Egenskap.alleUkategoriserteEgenskaper.map(Egenskap::toString),
                UUID.randomUUID(),
            )
        assertEquals(1, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave1)
    }

    @Test
    fun `Får kun oppgaver som er tildelt hvis tildelt er satt til true`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
        nyOppgaveForNyPerson()
        nyOppgaveForNyPerson()

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = true)
        assertEquals(1, oppgaver.size)
        assertEquals(1, oppgaver.first().filtrertAntall)
        oppgaver.assertIderSamsvarerMed(oppgave1)
    }

    @Test
    fun `Får kun oppgaver som ikke er tildelt hvis tildelt er satt til false`() {
        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = false)
        assertEquals(2, oppgaver.size)
        assertEquals(2, oppgaver.first().filtrertAntall)
        oppgaver.assertIderSamsvarerMed(oppgave2, oppgave3)
    }

    @Test
    fun `Får både tildelte og ikke tildelte oppgaver hvis tildelt er satt til null`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson()

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = null)
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
        oppgaver.assertIderSamsvarerMed(oppgave1, oppgave2, oppgave3)
    }

    @Test
    fun `Tar med alle oppgaver som saksbehandler har tilgang til`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
        val oppgave2 = nyOppgaveForNyPerson()
        val oppgave3 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.RISK_QA, FORTROLIG_ADRESSE))
        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
        oppgaver.assertIderSamsvarerMed(oppgave1, oppgave2, oppgave3)
    }

    @Test
    fun `Tar kun med oppgaver som er tildelt saksbehandler når dette bes om`() {
        val oppgave1 = nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
        val oppgave2 = nyOppgaveForNyPerson()
            .leggPåVentOgLagre(legacySaksbehandler)
            .tildelOgLagre(legacySaksbehandler)
        val oppgave3 = nyOppgaveForNyPerson()

        val oppgaverSomErTildelt =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = legacySaksbehandler.oid,
                egneSaker = true,
                egneSakerPåVent = false,
            )
        val oppgaverSomErTildeltOgPåvent =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = legacySaksbehandler.oid,
                egneSaker = true,
                egneSakerPåVent = true,
            )
        val alleOppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = legacySaksbehandler.oid,
                egneSaker = false,
                egneSakerPåVent = false,
            )
        assertEquals(1, oppgaverSomErTildelt.size)
        assertEquals(1, oppgaverSomErTildeltOgPåvent.size)
        assertEquals(3, alleOppgaver.size)
        oppgaverSomErTildelt.assertIderSamsvarerMed(oppgave1)
        oppgaverSomErTildeltOgPåvent.assertIderSamsvarerMed(oppgave2)
        alleOppgaver.assertIderSamsvarerMed(oppgave1, oppgave2, oppgave3)
    }

    @Test
    fun `Saksbehandler får ikke med oppgaver hen har sendt til beslutter selv om hen har beslutter-tilgang`() {
        val fødselsnummer: String = lagFødselsnummer()
        opprettPerson(fødselsnummer = fødselsnummer)
        val beslutter = nyLegacySaksbehandler()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = lagFødselsnummer())
            .tildelOgLagre(legacySaksbehandler)
            .sendTilBeslutterOgLagre(beslutter)

        nyTotrinnsvurdering(fødselsnummer, oppgave)
            .sendTilBeslutterOgLagre(legacySaksbehandler)

        val oppgaver = oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = legacySaksbehandler.oid
        )
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Saksbehandler får ikke med oppgaver med egenskap STRENGT_FORTROLIG_ADRESSE`() {
        nyOppgaveForNyPerson(oppgaveegenskaper = setOf(STRENGT_FORTROLIG_ADRESSE))
        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("STRENGT_FORTROLIG_ADRESSE"),
                saksbehandlerOid = legacySaksbehandler.oid,
            )
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Oppgaver blir filtrert riktig`() {
        val oppgave1 = nyOppgaveForNyPerson(
            oppgaveegenskaper =
                setOf(
                    Egenskap.SØKNAD,
                    Egenskap.HASTER,
                    Egenskap.UTLAND,
                    Egenskap.FORSTEGANGSBEHANDLING,
                    Egenskap.DELVIS_REFUSJON,
                    Egenskap.FLERE_ARBEIDSGIVERE,
                ),
        )
        nyOppgaveForNyPerson(
            oppgaveegenskaper =
                setOf(
                    Egenskap.REVURDERING,
                    Egenskap.HASTER,
                    Egenskap.UTLAND,
                    Egenskap.FORSTEGANGSBEHANDLING,
                    Egenskap.DELVIS_REFUSJON,
                    Egenskap.FLERE_ARBEIDSGIVERE,
                ),
        )
        nyOppgaveForNyPerson(
            oppgaveegenskaper = setOf(Egenskap.REVURDERING, Egenskap.HASTER),
        )
        nyOppgaveForNyPerson(
            oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.UTLAND),
        )
        val oppgave4 = nyOppgaveForNyPerson(
            oppgaveegenskaper = setOf(Egenskap.UTBETALING_TIL_SYKMELDT, Egenskap.EN_ARBEIDSGIVER),
        )

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper = mapOf(Kategori.Oppgavetype to listOf(SØKNAD, REVURDERING)),
        ).let {
            assertEquals(4, it.size)
        }

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper = mapOf(
                Kategori.Oppgavetype to listOf(SØKNAD, REVURDERING),
                Kategori.Mottaker to listOf(FLERE_ARBEIDSGIVERE)
            ),
        ).let {
            assertEquals(2, it.size)
        }

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper = mapOf(Kategori.Ukategorisert to listOf(HASTER, UTLAND)),
        ).let {
            assertEquals(2, it.size)
            assertTrue(it.any { it.id == oppgave1.id })
        }

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper =
                mapOf(
                    Kategori.Ukategorisert to listOf(HASTER, UTLAND),
                    Kategori.Oppgavetype to listOf(SØKNAD),
                    Kategori.Periodetype to listOf(FORSTEGANGSBEHANDLING),
                    Kategori.Mottaker to listOf(DELVIS_REFUSJON),
                    Kategori.Inntektskilde to listOf(FLERE_ARBEIDSGIVERE),
                ),
        ).let {
            assertEquals(1, it.size)
            assertEquals(oppgave1.id, it.first().id)
        }

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper =
                mapOf(
                    Kategori.Mottaker to listOf(UTBETALING_TIL_SYKMELDT),
                    Kategori.Inntektskilde to listOf(EN_ARBEIDSGIVER),
                ),
        ).let {
            assertEquals(1, it.size)
            assertEquals(oppgave4.id, it.first().id)
        }

        oppgaveApiDao.finnOppgaverForVisning(
            ekskluderEgenskaper = emptyList(),
            saksbehandlerOid = UUID.randomUUID(),
            grupperteFiltrerteEgenskaper = emptyMap(),
        ).let {
            assertEquals(5, it.size)
        }
    }

    @Test
    fun `Grupperte filtrerte egenskaper fungerer sammen med ekskluderte egenskaper`() {
        val oppgave1 = nyOppgaveForNyPerson(
            oppgaveegenskaper =
                setOf(
                    Egenskap.SØKNAD,
                    Egenskap.FORSTEGANGSBEHANDLING,
                    Egenskap.DELVIS_REFUSJON,
                    Egenskap.FLERE_ARBEIDSGIVERE,
                    Egenskap.PÅ_VENT,
                ),
        )
        nyOppgaveForNyPerson(
            oppgaveegenskaper =
                setOf(
                    Egenskap.SØKNAD,
                    Egenskap.HASTER,
                    Egenskap.UTLAND,
                    Egenskap.FORSTEGANGSBEHANDLING,
                    Egenskap.DELVIS_REFUSJON,
                    Egenskap.FLERE_ARBEIDSGIVERE,
                    Egenskap.PÅ_VENT,
                ),
        )

        val oppgaver =
            oppgaveApiDao.finnOppgaverForVisning(
                ekskluderEgenskaper = Egenskap.alleUkategoriserteEgenskaper.map(Egenskap::toString),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Kategori.Oppgavetype to listOf(SØKNAD),
                        Kategori.Periodetype to listOf(FORSTEGANGSBEHANDLING),
                        Kategori.Mottaker to listOf(DELVIS_REFUSJON),
                        Kategori.Inntektskilde to listOf(FLERE_ARBEIDSGIVERE),
                        Kategori.Status to listOf(PÅ_VENT),
                    ),
            )

        assertEquals(1, oppgaver.size)
        assertEquals(oppgave1.id, oppgaver.first().id)
    }

    private fun Collection<OppgaveFraDatabaseForVisning>.assertIderSamsvarerMed(vararg oppgaver: Oppgave) {
        assertEquals(oppgaver.map { it.id }.toSet(), map { it.id }.toSet())
    }

}
