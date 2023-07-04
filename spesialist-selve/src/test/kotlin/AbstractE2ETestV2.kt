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
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.TestRapidHelpers.sisteBehov
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.ENHET_OSLO
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver
import no.nav.helse.modell.overstyring.Subsumsjon
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.SENDT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALING_FEILET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail

internal abstract class AbstractE2ETestV2 : AbstractDatabaseTest() {
    private lateinit var utbetalingId: UUID
    internal val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val testRapid = TestRapid()
    internal val inspektør get() = testRapid.inspektør
    private val meldingssenderV2 = MeldingssenderV2(testRapid)
    protected lateinit var sisteMeldingId: UUID
    protected lateinit var sisteGodkjenningsbehovId: UUID
    internal val dataSource = AbstractDatabaseTest.dataSource
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

    protected fun automatiskGodkjent(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID
    ) {
        fremTilÅpneOppgaver(
            fom,
            tom,
            skjæringstidspunkt,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning(vedtaksperiodeId = vedtaksperiodeId)
    }

    protected fun fremTilVergemål(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = FØDSELSNUMMER,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        fremForbiUtbetalingsfilter(
            fom,
            tom,
            skjæringstidspunkt,
            periodetype,
            fødselsnummer,
            andreArbeidsforhold,
            regelverksvarsler,
            vedtaksperiodeId,
            utbetalingId,
            harOppdatertMetadata = harOppdatertMetadata,
            snapshotversjon = snapshotversjon,
            enhet = enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp
        )
        håndterEgenansattløsning()
    }

    protected fun fremTilÅpneOppgaver(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = FØDSELSNUMMER,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        fremTilVergemål(fom, tom, skjæringstidspunkt, periodetype, fødselsnummer, andreArbeidsforhold, regelverksvarsler, vedtaksperiodeId, utbetalingId, harOppdatertMetadata, snapshotversjon, enhet, arbeidsgiverbeløp = arbeidsgiverbeløp, personbeløp = personbeløp)
        håndterVergemålløsning(fullmakter = fullmakter)
    }

    protected fun fremForbiUtbetalingsfilter(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = FØDSELSNUMMER,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        håndterSøknad(fødselsnummer = fødselsnummer)
        håndterVedtaksperiodeOpprettet(vedtaksperiodeId = vedtaksperiodeId)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot(
            versjon = snapshotversjon,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )
        if (regelverksvarsler.isNotEmpty()) håndterAktivitetsloggNyAktivitet(varselkoder = regelverksvarsler)
        håndterGodkjenningsbehov(
            andreArbeidsforhold = andreArbeidsforhold,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            harOppdatertMetainfo = harOppdatertMetadata,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp
        )
        if (!harOppdatertMetadata) {
            håndterPersoninfoløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterEnhetløsning(vedtaksperiodeId = vedtaksperiodeId, enhet = enhet)
            håndterInfotrygdutbetalingerløsning(vedtaksperiodeId = vedtaksperiodeId)
            if (andreArbeidsforhold.isNotEmpty()) håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = vedtaksperiodeId)
            håndterArbeidsforholdløsning(vedtaksperiodeId = vedtaksperiodeId)
        }
        verify { snapshotClient.hentSnapshot(fødselsnummer) }
    }

    private fun forlengelseFremTilÅpneOppgaver(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
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
            utbetalingId = utbetalingId
        )
        håndterGodkjenningsbehov(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            harOppdatertMetainfo = harOppdatertMetadata,
            andreArbeidsforhold = andreArbeidsforhold,
        )
        verify { snapshotClient.hentSnapshot(FØDSELSNUMMER) }

        håndterEgenansattløsning()
        håndterVergemålløsning(fullmakter = fullmakter)
    }

    protected fun fremTilSaksbehandleroppgave(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        fødselsnummer: String = FØDSELSNUMMER,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        enhet: String = ENHET_OSLO,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harRisikovurdering: Boolean = false,
        harOppdatertMetadata: Boolean = false,
        kanGodkjennesAutomatisk: Boolean = false,
        snapshotversjon: Int = 1,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        fremTilÅpneOppgaver(fom, tom, skjæringstidspunkt, periodetype, fødselsnummer, andreArbeidsforhold, regelverksvarsler, fullmakter, vedtaksperiodeId, utbetalingId, harOppdatertMetadata = harOppdatertMetadata, snapshotversjon = snapshotversjon, enhet = enhet, arbeidsgiverbeløp = arbeidsgiverbeløp, personbeløp = personbeløp)
        håndterÅpneOppgaverløsning()
        if (!harRisikovurdering) håndterRisikovurderingløsning(kanGodkjennesAutomatisk = kanGodkjennesAutomatisk, risikofunn = risikofunn, vedtaksperiodeId = vedtaksperiodeId)
        if (!erFerdigstilt(sisteGodkjenningsbehovId)) håndterInntektløsning()
    }

    protected fun forlengelseFremTilSaksbehandleroppgave(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = true,
    ) {
        forlengelseFremTilÅpneOppgaver(
            fom,
            tom,
            skjæringstidspunkt,
            andreArbeidsforhold,
            fullmakter,
            vedtaksperiodeId,
            utbetalingId,
            harOppdatertMetadata
        )
        håndterÅpneOppgaverløsning()
        if (erRevurdering(vedtaksperiodeId)) return
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false, risikofunn = risikofunn, vedtaksperiodeId = vedtaksperiodeId)
    }

    protected fun forlengVedtak(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        harOppdatertMetadata: Boolean = true
    ) {
        forlengelseFremTilSaksbehandleroppgave(
            fom,
            tom,
            skjæringstidspunkt,
            andreArbeidsforhold,
            fullmakter,
            risikofunn,
            vedtaksperiodeId,
            utbetalingId,
            harOppdatertMetadata
        )
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
            skjæringstidspunkt = skjæringstidspunkt,
            andreArbeidsforhold = andreArbeidsforhold,
            regelverksvarsler = regelverksvarsler,
            fullmakter = fullmakter,
            risikofunn = risikofunn,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            harOppdatertMetadata = harOppdatertMetadata
        )
        håndterSaksbehandlerløsning(vedtaksperiodeId = vedtaksperiodeId)
        håndterVedtakFattet(vedtaksperiodeId = vedtaksperiodeId)
    }

    protected fun håndterVarseldefinisjonerEndret(vararg varselkoder: Triple<UUID, String, String>) {
        sisteMeldingId = meldingssenderV2.sendVarseldefinisjonerEndret(varselkoder.toList())
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
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeOpprettet(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
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
        forrigeTilstand: String? = null,
        gjeldendeTilstand: String? = null
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forrigeTilstand = forrigeTilstand ?: if (erRevurdering) "AVVENTER_SIMULERING_REVURDERING" else "AVVENTER_SIMULERING",
            gjeldendeTilstand = gjeldendeTilstand ?: if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
            forårsaketAvId = forårsaketAvId
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterVedtaksperiodeReberegnet(
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
            forrigeTilstand = if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
            gjeldendeTilstand = if (erRevurdering) "AVVENTER_HISTORIKK_REVURDERING" else "AVVENTER_HISTORIKK",
            forårsaketAvId = forårsaketAvId
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterVedtaksperiodeForkastet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        sisteMeldingId = meldingssenderV2.sendVedtaksperiodeForkastet(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
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
            utbetalingId = this.utbetalingId
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

    protected fun håndterSykefraværstilfeller(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        tilfeller: List<Map<String, Any>>,
    ) {
        sisteMeldingId = meldingssenderV2.sendSykefraværstilfeller(
            aktørId,
            fødselsnummer,
            tilfeller
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
        utbetalingId: UUID = UTBETALING_ID,
    ) {
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            utbetalingId = this.utbetalingId
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterUtbetalingErstattet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        utbetalingId: UUID = UTBETALING_ID,
    ) {
        håndterUtbetalingForkastet(aktørId, fødselsnummer, organisasjonsnummer, utbetalingId = this.utbetalingId)
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(aktørId, fødselsnummer, organisasjonsnummer, utbetalingtype, arbeidsgiverbeløp, personbeløp, utbetalingId = this.utbetalingId)
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterUtbetalingEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        forrigeStatus: Utbetalingsstatus = NY,
        gjeldendeStatus: Utbetalingsstatus = IKKE_UTBETALT,
        opprettet: LocalDateTime = LocalDateTime.now(),
        utbetalingId: UUID = UTBETALING_ID
    ) {
        nyUtbetalingId(utbetalingId)
        sisteMeldingId = meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = this.utbetalingId,
            type = utbetalingtype,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            forrigeStatus = forrigeStatus,
            gjeldendeStatus = gjeldendeStatus,
            opprettet = opprettet
        )
    }

    protected fun håndterUtbetalingFeilet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = Utbetalingtype.ANNULLERING.toString(),
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        utbetalingId: UUID = UTBETALING_ID
    ) {
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            gjeldendeStatus = UTBETALING_FEILET,
            utbetalingId = this.utbetalingId
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterUtbetalingForkastet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        forrigeStatus: Utbetalingsstatus = NY,
        utbetalingId: UUID = UTBETALING_ID
    ) {
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            forrigeStatus = forrigeStatus,
            gjeldendeStatus = FORKASTET,
            utbetalingId = this.utbetalingId
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterUtbetalingUtbetalt(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingId: UUID = UTBETALING_ID
    ) {
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            forrigeStatus = SENDT,
            gjeldendeStatus = UTBETALT,
            utbetalingId = this.utbetalingId
        )
        assertIngenEtterspurteBehov()
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

    protected fun håndterGodkjenningsbehovUtenValidering(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        andreArbeidsforhold: List<String> = emptyList(),
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        håndterUtbetalingOpprettet(utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING", utbetalingId = utbetalingId, arbeidsgiverbeløp = arbeidsgiverbeløp, personbeløp = personbeløp)
        håndterVedtaksperiodeEndret(vedtaksperiodeId = vedtaksperiodeId)
        sisteMeldingId = sendGodkjenningsbehov(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            utbetalingId,
            periodeFom = fom,
            periodeTom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold
        )
        sisteGodkjenningsbehovId = sisteMeldingId
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
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        harOppdatertMetainfo: Boolean = false,
        andreArbeidsforhold: List<String> = emptyList(),
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0
    ) {
        val alleArbeidsforhold = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT a.orgnummer FROM arbeidsgiver a INNER JOIN vedtak v on a.id = v.arbeidsgiver_ref INNER JOIN person p on p.id = v.person_ref WHERE p.fodselsnummer = ?"
            session.run(queryOf(query, fødselsnummer.toLong()).map { it.string("orgnummer") }.asList)
        }
        håndterGodkjenningsbehovUtenValidering(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            andreArbeidsforhold = andreArbeidsforhold,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp
        )

        when {
            !harOppdatertMetainfo -> assertEtterspurteBehov("HentPersoninfoV2")
            !andreArbeidsforhold.all { it in alleArbeidsforhold } -> assertEtterspurteBehov("Arbeidsgiverinformasjon")
            else -> assertEtterspurteBehov("EgenAnsatt")
        }
    }

    internal fun sendGodkjenningsbehov(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = periodeFom,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
    ) = meldingssenderV2.sendGodkjenningsbehov(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingId = utbetalingId,
        periodeFom = periodeFom,
        periodeTom = periodeTom,
        skjæringstidspunkt = skjæringstidspunkt,
        periodetype = periodetype,
        orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
    )

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

    protected fun håndterPersoninfoløsningUtenValidering(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert
    ) {
        sisteMeldingId = meldingssenderV2.sendPersoninfoløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, adressebeskyttelse)
    }

    protected fun håndterEnhetløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        enhet: String = "0301" // Oslo
    ) {
        assertEtterspurteBehov("HentEnhet")
        sisteMeldingId = meldingssenderV2.sendEnhetløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, enhet)
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
        val erKompositt = testRapid.inspektør.sisteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2") != null
        if (erKompositt) {
            assertEtterspurteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2")
            sisteMeldingId = meldingssenderV2.sendArbeidsgiverinformasjonKompositt(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
            return
        }
        assertEtterspurteBehov("Arbeidsgiverinformasjon")
        sisteMeldingId = meldingssenderV2.sendArbeidsgiverinformasjonløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, arbeidsgiverinformasjonJson)
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

    protected fun håndterEgenansattløsning(aktørId: String = AKTØR, fødselsnummer: String = FØDSELSNUMMER, erEgenAnsatt: Boolean = false) {
        assertEtterspurteBehov("EgenAnsatt")
        sisteMeldingId = meldingssenderV2.sendEgenAnsattløsning(aktørId, fødselsnummer, erEgenAnsatt)
    }

    protected fun håndterVergemålløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        vergemål: List<Vergemål> = emptyList(),
        fremtidsfullmakter: List<Vergemål> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
    ) {
        assertEtterspurteBehov("Vergemål")
        sisteMeldingId = meldingssenderV2.sendVergemålløsning(aktørId, fødselsnummer, vergemål, fremtidsfullmakter, fullmakter)
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
        kommentar: String? = null,
        begrunnelser: List<String> = emptyList(),
    ) {
        fun oppgaveIdFor(vedtaksperiodeId: UUID): Long = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) ORDER BY id DESC LIMIT 1;"
            requireNotNull(session.run(queryOf(query, vedtaksperiodeId).map { it.long(1) }.asSingle))
        }

        fun godkjenningsbehovIdFor(vedtaksperiodeId: UUID): UUID = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT id FROM hendelse h INNER JOIN vedtaksperiode_hendelse vh on h.id = vh.hendelse_ref WHERE vh.vedtaksperiode_id = ? AND h.type = 'GODKJENNING';"
            requireNotNull(session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("id") }.asSingle))
        }

        fun settOppgaveIAvventerSystem(oppgaveId: Long) {
            @Language("PostgreSQL")
            val query = "UPDATE oppgave SET status = 'AvventerSystem' WHERE id = ?"
            sessionOf(dataSource).use {
                it.run(queryOf(query, oppgaveId).asUpdate)
            }
        }

        val oppgaveId = oppgaveIdFor(vedtaksperiodeId)
        val godkjenningsbehovId = godkjenningsbehovIdFor(vedtaksperiodeId)
        settOppgaveIAvventerSystem(oppgaveId)
        sisteMeldingId = meldingssenderV2.sendSaksbehandlerløsning(
            fødselsnummer,
            oppgaveId = oppgaveId,
            godkjenningsbehovId = godkjenningsbehovId,
            godkjent = godkjent,
            begrunnelser = begrunnelser,
            kommentar = kommentar
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
        if (this::utbetalingId.isInitialized) håndterUtbetalingUtbetalt(aktørId, fødselsnummer, organisasjonsnummer, this.utbetalingId)
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
        dager: List<OverstyringDagDto> = listOf(
            OverstyringDagDto(1.januar(1970), Dagtype.Feriedag, Dagtype.Sykedag, null, 100)
        ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_tidslinje") {
            sisteMeldingId = meldingssenderV2.sendOverstyrTidslinje(aktørId, fødselsnummer, organisasjonsnummer, dager)
        }
    }

    protected fun håndterOverstyrInntektOgRefusjon(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        arbeidsgivere: List<OverstyrtArbeidsgiver> = listOf(
            OverstyrtArbeidsgiver(
                organisasjonsnummer = ORGNR,
                månedligInntekt = 25000.0,
                fraMånedligInntekt = 25001.0,
                forklaring = "testbortforklaring",
                subsumsjon = Subsumsjon("8-28", "LEDD_1", "BOKSTAV_A"),
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
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt> = listOf(
            OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                orgnummer = ORGNR,
                deaktivert = true,
                begrunnelse = "begrunnelse",
                forklaring = "forklaring"
            )
        )
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
                "SELECT true FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND tilstand = '${Generasjon.Låst.navn()}' ORDER BY id DESC"
            session.run(queryOf(query, vedtaksperiodeId).map { it.boolean(1) }.asSingle) ?: false
        }
    }

    protected fun assertDefinisjonerFor(varselkode: String, vararg forventedeTitler: String) {
        @Language("PostgreSQL")
        val query = "SELECT tittel FROM api_varseldefinisjon WHERE kode = ? ORDER BY id"
        val titler = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselkode).map { it.string("tittel") }.asList)
        }
        assertEquals(forventedeTitler.toList(), titler)
    }

    protected fun assertUtbetalinger(utbetalingId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT COUNT(1) FROM utbetaling_id ui INNER JOIN utbetaling u on ui.id = u.utbetaling_id_ref WHERE ui.utbetaling_id = ?"
        val antall = sessionOf(dataSource).use {
            it.run(queryOf(query, utbetalingId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertFeilendeMeldinger(forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT COUNT(1) FROM feilende_meldinger"
        val antall = sessionOf(dataSource).use {
            it.run(queryOf(query).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertKommandokjedetilstander(hendelseId: UUID, vararg forventedeTilstander: Kommandokjedetilstand) {
        @Language("PostgreSQL")
        val query = "SELECT tilstand FROM command_context WHERE hendelse_id = ? ORDER BY id"
        val tilstander = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, hendelseId).map { it.string("tilstand") }.asList
            )
        }
        assertEquals(forventedeTilstander.map { it.name }.toList(), tilstander)
    }

    protected fun assertGodkjenningsbehovBesvart(godkjent: Boolean, automatiskBehandlet: Boolean, vararg årsakerTilAvvist: String) {
        val løsning = testRapid.inspektør.løsning("Godkjenning") ?: fail("Forventet å finne svar på godkjenningsbehov")
        assertTrue(løsning.path("godkjent").isBoolean)
        assertEquals(godkjent, løsning.path("godkjent").booleanValue())
        assertEquals(automatiskBehandlet, løsning.path("automatiskBehandling").booleanValue())
        assertNotNull(løsning.path("godkjenttidspunkt").asLocalDateTime())
        if (årsakerTilAvvist.isNotEmpty()) {
            val begrunnelser = løsning["begrunnelser"].map { it.asText() }
            assertEquals(begrunnelser, begrunnelser.distinct())
            assertEquals(årsakerTilAvvist.toSet(), begrunnelser.toSet())
        }
    }

    protected fun assertGodkjenningsbehovIkkeBesvart() =
        testRapid.inspektør.løsning("Godkjenningsbehov") == null

    protected fun assertVedtaksperiodeAvvist(
        periodetype: String,
        begrunnelser: List<String>? = null,
        kommentar: String? = null,
    ) {
        testRapid.inspektør.hendelser("vedtaksperiode_avvist").first().let {
            assertEquals(periodetype, it.path("periodetype").asText())
            assertEquals(begrunnelser, it.path("begrunnelser")?.map(JsonNode::asText))
            // TODO: BUG: Vi sender faktisk kommentar som "null", ikke null...
            val faktiskKommentar = it.takeIf { it.hasNonNull("kommentar") }?.get("kommentar")?.asText()
            if (kommentar == null) assertEquals("null", faktiskKommentar)
            else assertEquals(kommentar, faktiskKommentar)
        }
    }

    protected fun assertSaksbehandleroppgave(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        oppgavestatus: Oppgavestatus,
    ) {
        @Language("PostgreSQL")
        val query = "SELECT status FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) ORDER by id DESC"
        val sisteOppgavestatus = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { enumValueOf<Oppgavestatus>(it.string("status")) }.asSingle)
        }
        assertEquals(oppgavestatus, sisteOppgavestatus)
    }

    protected fun assertIkkeSaksbehandleroppgave(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        @Language("PostgreSQL")
        val query = "SELECT 1 FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)"
        val antallOppgaver = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asList)
        }
        assertEquals(0, antallOppgaver.size)
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

    protected fun assertVedtaksperiodeEksisterer(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertVedtaksperiodeEksistererIkke(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    protected fun assertPersonEksisterer(fødselsnummer: String, aktørId: String) {
        assertEquals(1, person(fødselsnummer, aktørId)) { "Person med fødselsnummer=$fødselsnummer og aktørId=$aktørId finnes ikke i databasen" }
    }

    protected fun assertPersonEksistererIkke(fødselsnummer: String, aktørId: String) {
        assertEquals(0, person(fødselsnummer, aktørId))
    }

    protected fun assertArbeidsgiverEksisterer(organisasjonsnummer: String) {
        assertEquals(1, arbeidsgiver(organisasjonsnummer)) { "Arbeidsgiver med organisasjonsnummer=$organisasjonsnummer finnes ikke i databasen" }
    }

    protected fun assertUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse, sisteMeldingId)
        assertEquals(1, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
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
        val forårsaketAvId = inspektør.siste("behov")["@forårsaket_av"]["id"].asText()
        assertEquals(forårsaketAvId, sisteMeldingId.toString())
        assertEquals(behov.toList(), etterspurteBehov) {
            val ikkeEtterspurt = behov.toSet() - etterspurteBehov.toSet()
            "Forventet at følgende behov skulle være etterspurt: $ikkeEtterspurt\nFaktisk etterspurte behov: $etterspurteBehov\n"
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
        val query = """
           SELECT 1 FROM totrinnsvurdering
           INNER JOIN vedtak v on totrinnsvurdering.vedtaksperiode_id = v.vedtaksperiode_id
           INNER JOIN oppgave o on v.id = o.vedtak_ref
           WHERE o.id = ?
           AND utbetaling_id_ref IS NULL
        """.trimIndent()
        val erToTrinnsvurdering = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, oppgaveId).map { it.boolean(1) }.asSingle)
        } ?: throw IllegalStateException("Finner ikke oppgave med id $oppgaveId")
        assertTrue(erToTrinnsvurdering) {
            "Forventer at oppgaveId=$oppgaveId krever totrinnsvurdering"
        }
    }

    private fun erFerdigstilt(godkjenningsbehovId: UUID): Boolean {
        @Language("PostgreSQL")
        val query = "SELECT tilstand FROM command_context WHERE hendelse_id = ? ORDER by id DESC LIMIT 1"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, godkjenningsbehovId).map { it.string("tilstand") }.asSingle) == "FERDIG"
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

    protected fun nullstillVarseldefinisjoner() {
        @Language("PostgreSQL")
        val query = "TRUNCATE TABLE selve_varsel; TRUNCATE TABLE api_varseldefinisjon CASCADE;"
        sessionOf(dataSource).use {
            it.run(queryOf(query).asExecute)
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

    protected enum class Kommandokjedetilstand {
        NY,
        SUSPENDERT,
        FERDIG,
        AVBRUTT
    }
}

