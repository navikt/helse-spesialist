import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.MeldingssenderV2
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.TestRapidHelpers.løsning
import no.nav.helse.TestRapidHelpers.løsningOrNull
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.api.Arbeidsgiver
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.SubsumsjonDto
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.AVVENTER_ARBEIDSGIVERKVITTERING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.graphql.HentSnapshot
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractE2ETestV2 : AbstractDatabaseTest() {
    private lateinit var utbetalingId: UUID
    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val testRapid = TestRapid()
    private val meldingssenderV2 = MeldingssenderV2(testRapid)
    private lateinit var sisteMeldingId: UUID
    private val dataSource = AbstractDatabaseTest.dataSource
    private val testMediator = TestMediator(testRapid, snapshotClient, dataSource)

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        lagVarseldefinisjoner()
    }

    protected fun Int.oppgave(vedtaksperiodeId: UUID): Long {
        require(this > 0) { "Forventet oppgaveId for vedtaksperiodeId=$vedtaksperiodeId må være større enn 0" }
        @Language("PostgreSQL")
        val query = "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)"
        val oppgaveIder = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.long("id") }.asList)
        }
        assertTrue(oppgaveIder.size >= this) { "Forventer at det finnes minimum $this antall oppgaver for vedtaksperiodeId=$vedtaksperiodeId. Fant ${oppgaveIder.size} oppgaver." }
        return oppgaveIder[this - 1]
    }

    private fun nyUtbetalingId(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    protected fun fremTilÅpneOppgaver(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
    ) {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = vedtaksperiodeId)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot(
            versjon = snapshotversjon,
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            regelverksvarsler = regelverksvarsler
        )
        håndterGodkjenningsbehov(
            andreArbeidsforhold = andreArbeidsforhold,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            harOppdatertMetainfo = harOppdatertMetadata
        )
        if (!harOppdatertMetadata) {
            håndterPersoninfoløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterEnhetløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterInfotrygdutbetalingerløsning(vedtaksperiodeId = vedtaksperiodeId)
            if (andreArbeidsforhold.isNotEmpty()) håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterArbeidsforholdløsning(vedtaksperiodeId = vedtaksperiodeId)
        }
        verify { snapshotClient.hentSnapshot(FØDSELSNUMMER) }

        håndterEgenansattløsning()
        håndterVergemålløsning(fullmakter = fullmakter)
    }

    private fun forlengelseFremTilÅpneOppgaver(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        harOppdatertMetadata: Boolean = true
    ) {
        if (erRevurdering(vedtaksperiodeId)) {
            håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId)
        } else {
            håndterSøknad()
            håndterVedtaksperiodeOpprettet(vedtaksperiodeId = vedtaksperiodeId)
        }
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot(
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            regelverksvarsler = regelverksvarsler
        )
        håndterGodkjenningsbehov(
            andreArbeidsforhold = andreArbeidsforhold,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            harOppdatertMetainfo = harOppdatertMetadata
        )
        verify { snapshotClient.hentSnapshot(FØDSELSNUMMER) }

        håndterEgenansattløsning()
        håndterVergemålløsning(fullmakter = fullmakter)
    }

    protected fun fremTilSaksbehandleroppgave(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harRisikovurdering: Boolean = false,
        harOppdatertMetadata: Boolean = false,
        kanGodkjennesAutomatisk: Boolean = false,
        snapshotversjon: Int = 1
    ) {
        fremTilÅpneOppgaver(fom, tom, skjæringstidspunkt, andreArbeidsforhold, regelverksvarsler, fullmakter, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId, harOppdatertMetadata = harOppdatertMetadata, snapshotversjon = snapshotversjon)
        håndterÅpneOppgaverløsning()
        if (!harRisikovurdering) håndterRisikovurderingløsning(kanGodkjennesAutomatisk = kanGodkjennesAutomatisk, risikofunn = risikofunn, vedtaksperiodeId = vedtaksperiodeId)
        if (!harOppdatertMetadata) håndterInntektløsning()
    }

    protected fun forlengelseFremTilSaksbehandleroppgave(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = true,
    ) {
        forlengelseFremTilÅpneOppgaver(fom, tom, skjæringstidspunkt, andreArbeidsforhold, regelverksvarsler, fullmakter, vedtaksperiodeId, utbetalingId, harOppdatertMetadata)
        håndterÅpneOppgaverløsning()
        if (erRevurdering(vedtaksperiodeId)) return
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false, risikofunn = risikofunn, vedtaksperiodeId = vedtaksperiodeId)
    }

    protected fun forlengVedtak(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        harOppdatertMetadata: Boolean = true
    ) {
        forlengelseFremTilSaksbehandleroppgave(fom, tom, skjæringstidspunkt, andreArbeidsforhold, regelverksvarsler, fullmakter, risikofunn, vedtaksperiodeId, utbetalingId, harOppdatertMetadata)
        håndterSaksbehandlerløsning(vedtaksperiodeId = vedtaksperiodeId)
        håndterVedtakFattet(vedtaksperiodeId = vedtaksperiodeId)
    }
    protected fun nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false
    ) {
        fremTilSaksbehandleroppgave(
            fom,
            tom,
            skjæringstidspunkt,
            andreArbeidsforhold,
            regelverksvarsler,
            fullmakter,
            risikofunn,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            harOppdatertMetadata = harOppdatertMetadata
        )
        håndterSaksbehandlerløsning(vedtaksperiodeId = vedtaksperiodeId)
        håndterVedtakFattet(vedtaksperiodeId = vedtaksperiodeId)
    }

    protected fun håndterSøknad(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        sisteMeldingId = meldingssenderV2.sendSøknadSendt(aktørId, fødselsnummer, organisasjonsnummer)
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
        assertPersonEksisterer(fødselsnummer, aktørId)
        assertArbeidsgiverEksisterer(organisasjonsnummer)
    }

    protected fun håndterVedtaksperiodeOpprettet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            forrigeTilstand = "START"
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
    }

    protected fun håndterVedtaksperiodeEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        forårsaketAvId: UUID = UUID.randomUUID(),
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forrigeTilstand = if (erRevurdering) "AVVENTER_SIMULERING_REVURDERING" else "AVVENTER_SIMULERING",
            gjeldendeTilstand = if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
            forårsaketAvId = forårsaketAvId
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterVedtaksperiodeNyUtbetaling(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
    ) {
        if (!this::utbetalingId.isInitialized || utbetalingId != this.utbetalingId) nyUtbetalingId(utbetalingId)
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeNyUtbetaling(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterAktivitetsloggNyAktivitet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        varselkoder: List<String> = emptyList(),
    ) {
        varselkoder.forEach {
            lagVarseldefinisjon(it)
        }
        sisteMeldingId = meldingssenderV2.sendAktivitetsloggNyAktivitet(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            varselkoder = varselkoder
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterEndretSkjermetinfo(
        fødselsnummer: String = FØDSELSNUMMER,
        skjermet: Boolean
    ) {
        sisteMeldingId = meldingssenderV2.sendEndretSkjermetinfo(fødselsnummer, skjermet)
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterGosysOppgaveEndret(fødselsnummer: String = FØDSELSNUMMER) {
        sisteMeldingId = meldingssenderV2.sendGosysOppgaveEndret(fødselsnummer)
        assertEtterspurteBehov("ÅpneOppgaver")
    }

    protected fun håndterUtbetalingOpprettet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
    ) {
        sisteMeldingId = meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            type = utbetalingtype,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterUtbetalingForkastet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        sisteMeldingId = meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            type = "UTBETALING",
            forrigeStatus = NY,
            gjeldendeStatus = FORKASTET
        )
        assertIngenEtterspurteBehov()
    }

    private fun håndterUtbetalingUtbetalt(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        sisteMeldingId = meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            type = "UTBETALING",
            forrigeStatus = AVVENTER_ARBEIDSGIVERKVITTERING,
            gjeldendeStatus = UTBETALT
        )
        assertIngenEtterspurteBehov()
        assertUtgåendeMelding("oppgave_oppdatert")
    }

    protected fun håndterUtbetalingAnnullert(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        @Suppress("SqlResolve")
        fun fagsystemidFor(utbetalingId: UUID, tilArbeidsgiver: Boolean): String {
            val fagsystemidtype = if (tilArbeidsgiver) "arbeidsgiver" else "person"
            return sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query =
                    "SELECT fagsystem_id FROM utbetaling_id ui INNER JOIN oppdrag o on o.id = ui.${fagsystemidtype}_fagsystem_id_ref WHERE ui.utbetaling_id = ?"
                requireNotNull(session.run(queryOf(query, utbetalingId).map { it.string("fagsystem_id") }.asSingle)) {
                    "Forventet å finne med ${fagsystemidtype}FagsystemId for utbetalingId=$utbetalingId"
                }
            }
        }

        sisteMeldingId = meldingssenderV2.sendUtbetalingAnnullert(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            epost = SAKSBEHANDLER_EPOST,
            arbeidsgiverFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = true),
            personFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = false),
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterGodkjenningsbehov(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        harOppdatertMetainfo: Boolean = false,
        andreArbeidsforhold: List<String> = emptyList(),
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)

        val alleArbeidsforhold = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT a.orgnummer FROM arbeidsgiver a INNER JOIN vedtak v on a.id = v.arbeidsgiver_ref INNER JOIN person p on p.id = v.person_ref WHERE p.fodselsnummer = ?"
            session.run(queryOf(query, fødselsnummer.toLong()).map { it.string("orgnummer") }.asList)
        }

        håndterUtbetalingOpprettet(utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING")
        håndterVedtaksperiodeEndret()
        sisteMeldingId = meldingssenderV2.sendGodkjenningsbehov(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            utbetalingId,
            periodeFom = fom,
            periodeTom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold
        )
        when {
            !harOppdatertMetainfo -> assertEtterspurteBehov("HentPersoninfoV2")
            !andreArbeidsforhold.all { it in alleArbeidsforhold } -> assertEtterspurteBehov("Arbeidsgiverinformasjon")
            else -> assertEtterspurteBehov("EgenAnsatt")
        }
    }

    protected fun håndterPersoninfoløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert
    ) {
        assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId = meldingssenderV2.sendPersoninfoløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, adressebeskyttelse)
    }

    protected fun håndterEnhetløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        assertEtterspurteBehov("HentEnhet")
        sisteMeldingId = meldingssenderV2.sendEnhetløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterInfotrygdutbetalingerløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID
    ) {
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
        sisteMeldingId = meldingssenderV2.sendInfotrygdutbetalingerløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterArbeidsgiverinformasjonløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        arbeidsgiverinformasjonJson: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson>? = null
    ) {
        assertEtterspurteBehov("Arbeidsgiverinformasjon")
        sisteMeldingId = meldingssenderV2.sendArbeidsgiverinformasjonløsning(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            arbeidsgiverinformasjonJson
        )
    }

    protected fun håndterArbeidsforholdløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        assertEtterspurteBehov("Arbeidsforhold")
        sisteMeldingId = meldingssenderV2.sendArbeidsforholdløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterEgenansattløsning(aktørId: String = AKTØR, fødselsnummer: String = FØDSELSNUMMER) {
        assertEtterspurteBehov("EgenAnsatt")
        sisteMeldingId = meldingssenderV2.sendEgenAnsattløsning(aktørId, fødselsnummer, false)
    }

    protected fun håndterVergemålløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        fullmakter: List<Fullmakt> = emptyList(),
    ) {
        assertEtterspurteBehov("Vergemål")
        sisteMeldingId = meldingssenderV2.sendVergemålløsning(aktørId, fødselsnummer, fullmakter)
    }

    protected fun håndterInntektløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        assertEtterspurteBehov("InntekterForSykepengegrunnlag")
        sisteMeldingId = meldingssenderV2.sendInntektløsning(aktørId, fødselsnummer)
    }

    protected fun håndterÅpneOppgaverløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
    ) {
        assertEtterspurteBehov("ÅpneOppgaver")
        sisteMeldingId = meldingssenderV2.sendÅpneGosysOppgaverløsning(aktørId, fødselsnummer, antall, oppslagFeilet)
    }

    protected fun håndterRisikovurderingløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        kanGodkjennesAutomatisk: Boolean = true,
        risikofunn: List<Risikofunn> = emptyList(),
    ) {
        assertEtterspurteBehov("Risikovurdering")
        sisteMeldingId = meldingssenderV2.sendRisikovurderingløsning(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            kanGodkjennesAutomatisk,
            risikofunn
        )
    }

    protected fun håndterSaksbehandlerløsning(
        fødselsnummer: String = FØDSELSNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        godkjent: Boolean = true,
    ) {
        fun oppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?);"
            requireNotNull(session.run(queryOf(query, vedtaksperiodeId).map { it.long(1) }.asSingle))
        }

        fun godkjenningsbehovIdFor(vedtaksperiodeId: UUID): UUID = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT id FROM hendelse h INNER JOIN vedtaksperiode_hendelse vh on h.id = vh.hendelse_ref WHERE vh.vedtaksperiode_id = ? AND h.type = 'GODKJENNING';"
            requireNotNull(session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("id") }.asSingle))
        }

        val oppgaveId = oppgaveIdFor(vedtaksperiodeId)
        val godkjenningsbehovId = godkjenningsbehovIdFor(vedtaksperiodeId)
        sisteMeldingId = meldingssenderV2.sendSaksbehandlerløsning(
            fødselsnummer,
            oppgaveId = oppgaveId,
            godkjenningsbehovId = godkjenningsbehovId,
            godkjent = godkjent
        )
        if (godkjent) assertUtgåendeMelding("vedtaksperiode_godkjent")
        else assertUtgåendeMelding("vedtaksperiode_avvist")
        assertUtgåendeBehovløsning("Godkjenning")
    }

    protected fun håndterVedtakFattet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        håndterUtbetalingUtbetalt(aktørId, fødselsnummer, organisasjonsnummer)
        sisteMeldingId = meldingssenderV2.sendVedtakFattet(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterAdressebeskyttelseEndret(aktørId: String = AKTØR, fødselsnummer: String = FØDSELSNUMMER, harOppdatertMetadata: Boolean = true) {
        if (!harOppdatertMetadata) assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId = meldingssenderV2.sendAdressebeskyttelseEndret(aktørId, fødselsnummer)
    }

    protected fun håndterOppdaterPersonsnapshot(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        snapshotSomSkalHentes: GraphQLClientResponse<HentSnapshot.Result>
    ) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotSomSkalHentes
        sisteMeldingId = meldingssenderV2.sendOppdaterPersonsnapshot(aktørId, fødselsnummer)
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
    }

    protected fun håndterOverstyrTidslinje(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_tidslinje") {
            sisteMeldingId = meldingssenderV2.sendOverstyrTidslinje(aktørId, fødselsnummer, organisasjonsnummer)
        }
    }

    protected fun håndterOverstyrInntektOgRefusjon(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        arbeidsgivere: List<Arbeidsgiver> = listOf(
            Arbeidsgiver(
                organisasjonsnummer = ORGNR,
                månedligInntekt = 25000.0,
                fraMånedligInntekt = 25001.0,
                forklaring = "testbortforklaring",
                subsumsjon = SubsumsjonDto("8-28", "LEDD_1", "BOKSTAV_A"),
                refusjonsopplysninger = null,
                fraRefusjonsopplysninger = null,
                begrunnelse = "en begrunnelse")
        )

    ) {
        håndterOverstyring(aktørId, fødselsnummer, ORGNR, "overstyr_inntekt_og_refusjon") {
            sisteMeldingId = meldingssenderV2.sendOverstyrtInntektOgRefusjon(aktørId, fødselsnummer, skjæringstidspunkt, arbeidsgivere)
        }
    }

    protected fun håndterOverstyrArbeidsforhold(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_arbeidsforhold") {
            sisteMeldingId = meldingssenderV2.sendOverstyrtArbeidsforhold(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold
            )
        }
    }

    private fun håndterOverstyring(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        overstyringHendelse: String,
        overstyringBlock: () -> Unit,
    ) {
        overstyringBlock()
        val sisteOverstyring = testRapid.inspektør.hendelser(overstyringHendelse).last()
        val hendelseId = UUID.fromString(sisteOverstyring["@id"].asText())
        håndterOverstyringIgangsatt(aktørId, fødselsnummer, hendelseId)
        håndterUtbetalingForkastet(aktørId, fødselsnummer, organisasjonsnummer)
    }

    private fun håndterOverstyringIgangsatt(aktørId: String, fødselsnummer: String, kildeId: UUID) {
        sisteMeldingId = meldingssenderV2.sendOverstyringIgangsatt(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            berørtePerioder = listOf(
                mapOf(
                    "vedtaksperiodeId" to "$VEDTAKSPERIODE_ID"
                )
            ),
            kilde = kildeId
        )
    }

    private fun erRevurdering(vedtaksperiodeId: UUID): Boolean {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT true FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = true ORDER BY id DESC"
            session.run(queryOf(query, vedtaksperiodeId).map { it.boolean(1) }.asSingle) ?: false
        }
    }

    protected fun assertAutomatiskGodkjent() {
        val løsning = testRapid.inspektør.løsning("Godkjenning")
        assertTrue(løsning.path("godkjent").isBoolean)
        assertTrue(løsning.path("godkjent").booleanValue())
        assertNotNull(løsning.path("godkjenttidspunkt").asLocalDateTime())
    }

    protected fun assertIkkeAutomatiskGodkjent() {
        val løsning = testRapid.inspektør.løsningOrNull("Godkjenning")
        assertNull(løsning)
    }

    protected fun assertSaksbehandleroppgave(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        oppgavestatus: Oppgavestatus,
    ) {
        @Language("PostgreSQL")
        val query = "SELECT status FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)"
        val oppgavestatuser = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { enumValueOf<Oppgavestatus>(it.string("status")) }.asList)
        }
        assertEquals(1, oppgavestatuser.size)
        assertEquals(oppgavestatus, oppgavestatuser.single())
    }

    protected fun assertSnapshot(forventet: GraphQLClientResponse<HentSnapshot.Result>, vedtaksperiodeId: UUID) {
        val forventetPersonsnapshot = forventet.data?.person
        val personsnaphot = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                "SELECT data FROM snapshot WHERE id = (SELECT snapshot_ref FROM vedtak WHERE vedtaksperiode_id=?)"
            it.run(
                queryOf(query, vedtaksperiodeId).map { row ->
                    objectMapper.readValue<GraphQLPerson>(row.string("data"))
                }.asSingle
            )
        }
        assertEquals(forventetPersonsnapshot, personsnaphot)
    }

    protected fun assertSnapshotversjon(vedtaksperiodeId: UUID, forventetVersjon: Int) {
        val versjon = sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                "SELECT versjon FROM snapshot WHERE id = (SELECT snapshot_ref FROM vedtak WHERE vedtaksperiode_id=?)"
            it.run(
                queryOf(query, vedtaksperiodeId).map { row ->
                    row.int("versjon")
                }.asSingle
            )
        }
        assertEquals(forventetVersjon, versjon)
    }

    protected fun assertVarsler(vedtaksperiodeId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = ?"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertAvhukedeVarsler(vedtaksperiodeId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = ? AND status = 'GODKJENT'"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertSkjermet(fødselsnummer: String = FØDSELSNUMMER, skjermet: Boolean?) {
        assertEquals(skjermet, EgenAnsattDao(dataSource).erEgenAnsatt(fødselsnummer))
    }

    protected fun assertAdressebeskyttelse(fødselsnummer: String = FØDSELSNUMMER, adressebeskyttelse: Adressebeskyttelse?) {
        assertEquals(adressebeskyttelse, PersonDao(dataSource).findAdressebeskyttelse(fødselsnummer))
    }

    protected fun assertNyttSnapshot(block: () -> Unit) {
        clearMocks(snapshotClient, answers = false, verificationMarks = false)
        block()
        verify(exactly = 1) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }
    }

    private fun assertVedtaksperiodeEksisterer(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertVedtaksperiodeEksistererIkke(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    private fun assertPersonEksisterer(fødselsnummer: String, aktørId: String) {
        assertEquals(1, person(fødselsnummer, aktørId)) { "Person med fødselsnummer=$fødselsnummer og aktørId=$aktørId finnes ikke i databasen" }
    }

    protected fun assertPersonEksistererIkke(fødselsnummer: String, aktørId: String) {
        assertEquals(0, person(fødselsnummer, aktørId))
    }

    private fun assertArbeidsgiverEksisterer(organisasjonsnummer: String) {
        assertEquals(1, arbeidsgiver(organisasjonsnummer)) { "Arbeidsgiver med organisasjonsnummer=$organisasjonsnummer finnes ikke i databasen" }
    }

    protected fun assertUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse, sisteMeldingId)
        assertEquals(1, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
        }
    }

    private fun assertSisteUtgåendeMelding(hendelse: String) {
        val sisteMelding = testRapid.inspektør.hendelser().last()
        assertEquals(sisteMelding, hendelse) {
            "Siste utgående melding: $sisteMelding"
        }
    }

    private fun assertIngenUtgåendeMeldinger() {
        val meldinger = testRapid.inspektør.hendelser(sisteMeldingId)
        assertEquals(0, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
        }
    }

    protected fun assertIkkeUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse)
        assertEquals(0, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
        }
    }

    private fun assertUtgåendeBehovløsning(behov: String) {
        val løsning = testRapid.inspektør.løsningOrNull(behov)
        assertNotNull(løsning)
    }

    protected fun assertInnholdIBehov(behov: String, block: (JsonNode) -> Unit) {
        val etterspurtBehov = testRapid.inspektør.behov(behov).last()
        block(etterspurtBehov)
    }

    private fun assertEtterspurteBehov(vararg behov: String) {
        val etterspurteBehov = testRapid.inspektør.behov(sisteMeldingId)
        assertEquals(behov.toList(), etterspurteBehov) {
            val ikkeEtterspurt = behov.toSet() - etterspurteBehov.toSet()
            "Følgende behov ble ikke etterspurt: $ikkeEtterspurt\nEtterspurte behov: $etterspurteBehov\n"
        }
    }

    protected fun assertIngenEtterspurteBehov() {
        assertEquals(emptyList<String>(), testRapid.inspektør.behov(sisteMeldingId))
    }

    protected fun assertSisteEtterspurteBehov(behov: String) {
        val sisteEtterspurteBehov = testRapid.inspektør.behov().last()
        assertEquals(sisteEtterspurteBehov, behov)
    }

    protected fun assertUtbetaling(arbeidsgiverbeløp: Int, personbeløp: Int) {
        assertEquals(arbeidsgiverbeløp, finnbeløp("arbeidsgiver"))
        assertEquals(personbeløp, finnbeløp("person"))
    }

    protected fun assertOverstyringer(vedtaksperiodeId: UUID, vararg forventedeOverstyringstyper: OverstyringType) {
        val typer = testMediator.overstyringstyperForVedtaksperiode(vedtaksperiodeId)
        assertEquals(forventedeOverstyringstyper.toSet(), typer.toSet()) {
            val ikkeEtterspurt = typer.toSet() - forventedeOverstyringstyper.toSet()
            "Følgende typer finnes ikke: $ikkeEtterspurt\nForventede typer: $forventedeOverstyringstyper\n"
        }
    }

    protected fun assertTotrinnsvurdering(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = "SELECT er_totrinnsoppgave FROM oppgave WHERE id = ?"
        val erToTrinnsvurdering = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, oppgaveId).map { it.boolean("er_totrinnsoppgave") }.asSingle)
        } ?: throw IllegalStateException("Finner ikke oppgave med id $oppgaveId")
        assertTrue(erToTrinnsvurdering) {
            "Forventer at oppgaveId=$oppgaveId krever totrinnsvurdering"
        }
    }

    private fun finnbeløp(type: String): Int? {
        @Suppress("SqlResolve")
        @Language("PostgreSQL")
        val query = "SELECT ${type}beløp FROM utbetaling_id WHERE utbetaling_id = ?"
        return sessionOf(dataSource).use {
            it.run(queryOf(query, utbetalingId).map { it.intOrNull("${type}beløp") }.asSingle)
        }
    }

    protected fun person(fødselsnummer: String, aktørId: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM person WHERE fodselsnummer = ? AND aktor_id = ?"
            requireNotNull(
                session.run(queryOf(query, fødselsnummer.toLong(), aktørId.toLong()).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    protected fun arbeidsgiver(organisasjonsnummer: String): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM arbeidsgiver WHERE orgnummer = ?"
            requireNotNull(
                session.run(queryOf(query, organisasjonsnummer.toLong()).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = ?"
            requireNotNull(
                session.run(queryOf(query, vedtaksperiodeId).map { row -> row.int(1) }.asSingle)
            )
        }
    }

    private fun lagVarseldefinisjoner() {
        val varselkoder = Varselkode.values()
        varselkoder.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                    varselkode,
                    "En tittel for varselkode=${varselkode}",
                    "En forklaring for varselkode=${varselkode}",
                    "En handling for varselkode=${varselkode}",
                    false,
                    LocalDateTime.now()
                ).asUpdate)
        }
    }
}

