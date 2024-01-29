
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
import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.Meldingssender
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
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent.OverstyrtRefusjonselementEvent
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
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vilkårsprøving.LovhjemmelEvent
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.LovhjemmelFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrInntektOgRefusjonHandlingFraApi.OverstyrArbeidsgiverFraApi.RefusjonselementFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrTidslinjeHandlingFraApi.OverstyrDagFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail

internal abstract class AbstractE2ETest : AbstractDatabaseTest() {
    private lateinit var utbetalingId: UUID
    internal val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val testRapid = TestRapid()
    internal val inspektør get() = testRapid.inspektør
    private val meldingssender = Meldingssender(testRapid)
    protected lateinit var sisteMeldingId: UUID
    protected lateinit var sisteGodkjenningsbehovId: UUID
    internal val dataSource = AbstractDatabaseTest.dataSource
    private val testMediator = TestMediator(testRapid, snapshotClient, dataSource)
    protected val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected val SAKSBEHANDLER_EPOST = "augunn.saksbehandler@nav.no"
    protected val SAKSBEHANDLER_IDENT = "S199999"
    protected val SAKSBEHANDLER_NAVN = "Augunn Saksbehandler"
    private val saksbehandler = SaksbehandlerFraApi(
        oid = SAKSBEHANDLER_OID,
        navn = SAKSBEHANDLER_NAVN,
        epost = SAKSBEHANDLER_EPOST,
        ident = SAKSBEHANDLER_IDENT,
        grupper = emptyList()
    )

    @BeforeEach
    internal fun resetTestSetup() {
        resetTestRapid()
        lagVarseldefinisjoner()
        opprettSaksbehandler()
    }

    protected fun resetTestRapid() = testRapid.reset()

    // Tanken er at denne ikke skal eksponeres ut av AbstractE2ETest, for å unngå at enkelttester implementer egen kode
    // som bør være felles
    protected val __ikke_bruk_denne get() = testRapid

    private fun opprettSaksbehandler() {
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """INSERT INTO saksbehandler(oid, navn, epost) VALUES (?, ?, ?) ON CONFLICT (oid) DO NOTHING """
            it.run(queryOf(query, SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST).asExecute)
        }
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
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
    ) {
        fremTilÅpneOppgaver(
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                periodeFom = fom,
                periodeTom = tom,
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning(vedtaksperiodeId = vedtaksperiodeId)
        håndterUtbetalingUtbetalt()
        håndterAvsluttetMedVedtak(fom = fom, tom = tom, skjæringstidspunkt = skjæringstidspunkt)
        håndterVedtakFattet()
    }

    protected fun fremTilVergemål(
        regelverksvarsler: List<String> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(),
    ) {
        fremForbiUtbetalingsfilter(
            regelverksvarsler,
            harOppdatertMetadata = harOppdatertMetadata,
            snapshotversjon = snapshotversjon,
            enhet = enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        if (!harOppdatertMetadata) håndterEgenansattløsning()
    }

    protected fun fremTilÅpneOppgaver(
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(),
    ) {
        fremTilVergemål(
            regelverksvarsler,
            harOppdatertMetadata,
            snapshotversjon,
            enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata
        )
        håndterVergemålløsning(fullmakter = fullmakter)
    }

    protected fun fremForbiUtbetalingsfilter(
        regelverksvarsler: List<String> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        snapshotversjon: Int = 1,
        enhet: String = ENHET_OSLO,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(
            avviksvurderingId = avviksvurderingTestdata.avviksvurderingId,
        ),
    ) {
        håndterSøknad(fødselsnummer = godkjenningsbehovTestdata.fødselsnummer)
        håndterVedtaksperiodeOpprettet(
            vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            fom = godkjenningsbehovTestdata.periodeFom,
            tom = godkjenningsbehovTestdata.periodeTom,
            skjæringstidspunkt = godkjenningsbehovTestdata.skjæringstidspunkt
        )
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot(
            versjon = snapshotversjon,
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            utbetalingId = godkjenningsbehovTestdata.utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )
        if (regelverksvarsler.isNotEmpty()) håndterAktivitetsloggNyAktivitet(varselkoder = regelverksvarsler)
        håndterGodkjenningsbehov(
            harOppdatertMetainfo = harOppdatertMetadata,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = godkjenningsbehovTestdata.utbetalingId, vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
        )
        if (!harOppdatertMetadata) {
            håndterPersoninfoløsning()
            håndterEnhetløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId, enhet = enhet)
            håndterInfotrygdutbetalingerløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
            if (godkjenningsbehovTestdata.orgnummereMedRelevanteArbeidsforhold.isNotEmpty()) håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
            håndterArbeidsgiverinformasjonløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
            håndterArbeidsforholdløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
        }
        verify { snapshotClient.hentSnapshot(godkjenningsbehovTestdata.fødselsnummer) }
    }

    private fun håndterAvviksvurdering(avviksvurderingTestdata: AvviksvurderingTestdata) {
        sisteMeldingId = meldingssender.sendAvvikVurdert(avviksvurderingTestdata)
    }

    private fun forlengelseFremTilÅpneOppgaver(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        harOppdatertMetadata: Boolean = true,
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
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
        val avviksvurderingTestdata = AvviksvurderingTestdata()
        håndterGodkjenningsbehov(
            harOppdatertMetainfo = harOppdatertMetadata,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = GodkjenningsbehovTestdata(
                periodeFom = fom,
                periodeTom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                periodetype = FORLENGELSE,
                avviksvurderingId = avviksvurderingTestdata.avviksvurderingId,
            )
        )
        verify { snapshotClient.hentSnapshot(FØDSELSNUMMER) }

        håndterVergemålløsning(fullmakter = fullmakter)
    }

    protected fun fremTilSaksbehandleroppgave(
        enhet: String = ENHET_OSLO,
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UUID.randomUUID(),
        harRisikovurdering: Boolean = false,
        harOppdatertMetadata: Boolean = false,
        kanGodkjennesAutomatisk: Boolean = false,
        snapshotversjon: Int = 1,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(utbetalingId = utbetalingId, vedtaksperiodeId = vedtaksperiodeId),
    ) {
        fremTilÅpneOppgaver(
            regelverksvarsler,
            fullmakter,
            harOppdatertMetadata = harOppdatertMetadata,
            snapshotversjon = snapshotversjon,
            enhet = enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        håndterÅpneOppgaverløsning()
        if (!harRisikovurdering) håndterRisikovurderingløsning(
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            risikofunn = risikofunn,
            vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId
        )
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
        håndterRisikovurderingløsning(
            kanGodkjennesAutomatisk = false,
            risikofunn = risikofunn,
            vedtaksperiodeId = vedtaksperiodeId
        )
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
        harOppdatertMetadata: Boolean = true,
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
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
        harOppdatertMetadata: Boolean = false,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(periodeFom = fom, periodeTom = tom, skjæringstidspunkt = skjæringstidspunkt, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
    ) {
        fremTilSaksbehandleroppgave(
            regelverksvarsler = regelverksvarsler,
            fullmakter = fullmakter,
            risikofunn = risikofunn,
            harOppdatertMetadata = harOppdatertMetadata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        håndterSaksbehandlerløsning(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
        håndterVedtakFattet(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
    }

    protected fun håndterSøknad(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        sisteMeldingId = meldingssender.sendSøknadSendt(aktørId, fødselsnummer, organisasjonsnummer)
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
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
    ) {
        sisteMeldingId = meldingssender.sendVedtaksperiodeOpprettet(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
    }

    protected fun håndterSkjønnsfastsattSykepengegrunnlag(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        saksbehandlerOid: UUID = SAKSBEHANDLER_OID,
        saksbehandlerEpost: String = SAKSBEHANDLER_EPOST,
        saksbehandlerNavn: String = SAKSBEHANDLER_NAVN,
        saksbehandlerIdent: String = SAKSBEHANDLER_IDENT,
    ) {
        sisteMeldingId = meldingssender.sendSaksbehandlerSkjønnsfastsettingSykepengegrunnlag(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            saksbehandlerEpost = saksbehandlerEpost,
            saksbehandlerIdent = saksbehandlerIdent,
            saksbehandlerNavn = saksbehandlerNavn
        )
    }

    protected fun håndterVedtaksperiodeEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        forårsaketAvId: UUID = UUID.randomUUID(),
        forrigeTilstand: String? = null,
        gjeldendeTilstand: String? = null,
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId = meldingssender.sendVedtaksperiodeEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forrigeTilstand = forrigeTilstand
                ?: if (erRevurdering) "AVVENTER_SIMULERING_REVURDERING" else "AVVENTER_SIMULERING",
            gjeldendeTilstand = gjeldendeTilstand
                ?: if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
            forårsaketAvId = forårsaketAvId
        )
    }

    protected fun håndterVedtaksperiodeReberegnet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        forårsaketAvId: UUID = UUID.randomUUID(),
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId = meldingssender.sendVedtaksperiodeEndret(
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
        sisteMeldingId = meldingssender.sendVedtaksperiodeForkastet(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterVedtaksperiodeNyUtbetaling(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID,
    ) {
        if (!this::utbetalingId.isInitialized || utbetalingId != this.utbetalingId) nyUtbetalingId(utbetalingId)
        sisteMeldingId = meldingssender.sendVedtaksperiodeNyUtbetaling(
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
        sisteMeldingId = meldingssender.sendAktivitetsloggNyAktivitet(
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
        sisteMeldingId = meldingssender.sendSykefraværstilfeller(
            aktørId,
            fødselsnummer,
            tilfeller
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterEndretSkjermetinfo(
        fødselsnummer: String = FØDSELSNUMMER,
        skjermet: Boolean,
    ) {
        sisteMeldingId = meldingssender.sendEndretSkjermetinfo(fødselsnummer, skjermet)
        if (!skjermet) {
            assertIngenEtterspurteBehov()
            assertIngenUtgåendeMeldinger()
        }
    }

    protected fun håndterGosysOppgaveEndret(fødselsnummer: String = FØDSELSNUMMER) {
        sisteMeldingId = meldingssender.sendGosysOppgaveEndret(fødselsnummer)
        assertEtterspurteBehov("ÅpneOppgaver")
    }

    protected fun håndterTilbakedateringBehandlet(fødselsnummer: String = FØDSELSNUMMER, skjæringstidspunkt: LocalDate) {
        sisteMeldingId = meldingssender.sendTilbakedateringBehandlet(fødselsnummer, skjæringstidspunkt)
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
    }

    protected fun håndterUtbetalingErstattet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
    ) {
        håndterUtbetalingForkastet(aktørId, fødselsnummer, organisasjonsnummer)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            utbetalingId = UUID.randomUUID()
        )
        assertIngenEtterspurteBehov()
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
        utbetalingId: UUID = this.utbetalingId,
    ) {
        nyUtbetalingId(utbetalingId)
        sisteMeldingId = meldingssender.sendUtbetalingEndret(
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
    ) {
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
    ) {
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
        fødselsnummer: String = FØDSELSNUMMER,
        saksbehandler_epost: String,
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

        sisteMeldingId = meldingssender.sendUtbetalingAnnullert(
            fødselsnummer = fødselsnummer,
            utbetalingId = utbetalingId,
            epost = saksbehandler_epost,
            arbeidsgiverFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = true),
            personFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = false),
        )
        assertIngenEtterspurteBehov()
        assertIngenUtgåendeMeldinger()
    }

    protected fun håndterGodkjenningsbehovUtenValidering(
        utbetalingId: UUID = UTBETALING_ID,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(utbetalingId = utbetalingId, avviksvurderingId = avviksvurderingTestdata.avviksvurderingId),
    ) {
        val erRevurdering = erRevurdering(godkjenningsbehovTestdata.vedtaksperiodeId)
        håndterVedtaksperiodeNyUtbetaling(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId, utbetalingId = godkjenningsbehovTestdata.utbetalingId)
        håndterUtbetalingOpprettet(
            utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING",
            utbetalingId = godkjenningsbehovTestdata.utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp
        )
        håndterVedtaksperiodeEndret(vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId)
        håndterAvviksvurdering(avviksvurderingTestdata)
        sisteMeldingId = sendGodkjenningsbehov(godkjenningsbehovTestdata)
        sisteGodkjenningsbehovId = sisteMeldingId
    }

    protected fun håndterGodkjenningsbehov(
        harOppdatertMetainfo: Boolean = false,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(),
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = GodkjenningsbehovTestdata(),
    ) {
        val alleArbeidsforhold = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT a.orgnummer FROM arbeidsgiver a INNER JOIN vedtak v on a.id = v.arbeidsgiver_ref INNER JOIN person p on p.id = v.person_ref WHERE p.fodselsnummer = ?"
            session.run(queryOf(query, godkjenningsbehovTestdata.fødselsnummer.toLong()).map { it.string("orgnummer") }.asList)
        }
        håndterGodkjenningsbehovUtenValidering(
            utbetalingId = godkjenningsbehovTestdata.utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(avviksvurderingId = avviksvurderingTestdata.avviksvurderingId)
        )

        when {
            !harOppdatertMetainfo -> assertEtterspurteBehov("HentPersoninfoV2")
            !godkjenningsbehovTestdata.orgnummereMedRelevanteArbeidsforhold.all { it in alleArbeidsforhold } -> assertEtterspurteBehov("Arbeidsgiverinformasjon")
            else -> assertEtterspurteBehov("Vergemål")
        }
    }

    internal fun sendGodkjenningsbehov(godkjenningsbehovTestdata: GodkjenningsbehovTestdata) =
        meldingssender.sendGodkjenningsbehov(godkjenningsbehovTestdata).also { sisteMeldingId = it }

    protected fun håndterPersoninfoløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ) {
        assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId = meldingssender.sendPersoninfoløsning(
            aktørId,
            fødselsnummer,
            adressebeskyttelse
        )
    }

    protected fun håndterPersoninfoløsningUtenValidering(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ) {
        sisteMeldingId = meldingssender.sendPersoninfoløsning(
            aktørId,
            fødselsnummer,
            adressebeskyttelse,
        )
    }

    protected fun håndterEnhetløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        enhet: String = "0301", // Oslo
    ) {
        assertEtterspurteBehov("HentEnhet")
        sisteMeldingId =
            meldingssender.sendEnhetløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, enhet)
    }

    protected fun håndterInfotrygdutbetalingerløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
        sisteMeldingId = meldingssender.sendInfotrygdutbetalingerløsning(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId
        )
    }

    protected fun håndterArbeidsgiverinformasjonløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        arbeidsgiverinformasjonJson: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson>? = null,
    ) {
        val erKompositt = testRapid.inspektør.sisteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2") != null
        if (erKompositt) {
            assertEtterspurteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2")
            sisteMeldingId = meldingssender.sendArbeidsgiverinformasjonKompositt(
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId
            )
            return
        }
        assertEtterspurteBehov("Arbeidsgiverinformasjon")
        sisteMeldingId = meldingssender.sendArbeidsgiverinformasjonløsning(
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
        sisteMeldingId =
            meldingssender.sendArbeidsforholdløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterEgenansattløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        erEgenAnsatt: Boolean = false,
    ) {
        assertEtterspurteBehov("EgenAnsatt")
        sisteMeldingId = meldingssender.sendEgenAnsattløsning(aktørId, fødselsnummer, erEgenAnsatt)
    }

    protected fun håndterVergemålløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        vergemål: List<Vergemål> = emptyList(),
        fremtidsfullmakter: List<Vergemål> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
    ) {
        assertEtterspurteBehov("Vergemål")
        sisteMeldingId =
            meldingssender.sendVergemålløsning(aktørId, fødselsnummer, vergemål, fremtidsfullmakter, fullmakter)
    }

    protected fun håndterInntektløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        assertEtterspurteBehov("InntekterForSykepengegrunnlag")
        sisteMeldingId = meldingssender.sendInntektløsning(aktørId, fødselsnummer)
    }

    protected fun håndterÅpneOppgaverløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
    ) {
        assertEtterspurteBehov("ÅpneOppgaver")
        sisteMeldingId = meldingssender.sendÅpneGosysOppgaverløsning(aktørId, fødselsnummer, antall, oppslagFeilet)
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
        sisteMeldingId = meldingssender.sendRisikovurderingløsning(
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
            val query =
                "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) ORDER BY id DESC LIMIT 1;"
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
        sisteMeldingId = meldingssender.sendSaksbehandlerløsning(
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

    protected fun håndterAvsluttetMedVedtak(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        fastsattType: String = "EtterHovedregel",
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        settInnAvviksvurderingFraSpleis: Boolean = true,
    ) {
        val utbetalingId = if (this::utbetalingId.isInitialized) this.utbetalingId else null
        sisteMeldingId = meldingssender.sendAvsluttetMedVedtak(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            fastsattType = fastsattType,
            settInnAvviksvurderingFraSpleis = settInnAvviksvurderingFraSpleis,
        )
    }

    protected fun håndterAvsluttetUtenVedtak(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 11.januar,
        skjæringstidspunkt: LocalDate = fom,
    ) {
        sisteMeldingId = meldingssender.sendAvsluttetUtenVedtak(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
        )
    }

    protected fun håndterVedtakFattet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        if (this::utbetalingId.isInitialized) håndterUtbetalingUtbetalt(aktørId, fødselsnummer, organisasjonsnummer)
        sisteMeldingId = meldingssender.sendVedtakFattet(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterAdressebeskyttelseEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        harOppdatertMetadata: Boolean = true,
    ) {
        if (!harOppdatertMetadata) assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId = meldingssender.sendAdressebeskyttelseEndret(aktørId, fødselsnummer)
    }

    protected fun håndterOppdaterPersonsnapshot(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        snapshotSomSkalHentes: GraphQLClientResponse<HentSnapshot.Result>,
    ) {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotSomSkalHentes
        sisteMeldingId = meldingssender.sendOppdaterPersonsnapshot(aktørId, fødselsnummer)
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
    }

    protected fun håndterOverstyrTidslinje(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        dager: List<OverstyrDagFraApi> = listOf(
            OverstyrDagFraApi(1.januar(1970), Dagtype.Feriedag.toString(), Dagtype.Sykedag.toString(), null, 100, null)
        ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_tidslinje") {
            val handling = OverstyrTidslinjeHandlingFraApi(
                vedtaksperiodeId,
                organisasjonsnummer,
                fødselsnummer,
                aktørId,
                "En begrunnelse",
                dager
            )
            testMediator.håndter(handling, saksbehandler)
            // Her må det gjøres kall til api for å sende inn overstyring av tidslinje
        }
    }

    protected fun håndterOverstyrInntektOgRefusjon(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        arbeidsgivere: List<OverstyrArbeidsgiverFraApi> = listOf(
            OverstyrArbeidsgiverFraApi(
                organisasjonsnummer = ORGNR,
                månedligInntekt = 25000.0,
                fraMånedligInntekt = 25001.0,
                forklaring = "testbortforklaring",
                lovhjemmel = LovhjemmelFraApi("8-28", "LEDD_1", "BOKSTAV_A", "folketrygdloven", "1970-01-01"),
                refusjonsopplysninger = null,
                fraRefusjonsopplysninger = null,
                begrunnelse = "en begrunnelse"
            )
        ),

        ) {
        håndterOverstyring(aktørId, fødselsnummer, ORGNR, "overstyr_inntekt_og_refusjon") {
            val handling =
                OverstyrInntektOgRefusjonHandlingFraApi(aktørId, fødselsnummer, skjæringstidspunkt, arbeidsgivere)
            testMediator.håndter(handling, saksbehandler)
            sisteMeldingId = meldingssender.sendOverstyrtInntektOgRefusjon(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                arbeidsgivere = arbeidsgivere.byggOverstyrArbeidsgiverEvent(),
                saksbehandlerOid = SAKSBEHANDLER_OID,
            )
        }
    }

    protected fun håndterOverstyrArbeidsforhold(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        skjæringstidspunkt: LocalDate = 1.januar,
        overstyrteArbeidsforhold: List<ArbeidsforholdFraApi> = listOf(
            ArbeidsforholdFraApi(
                orgnummer = ORGNR,
                deaktivert = true,
                begrunnelse = "begrunnelse",
                forklaring = "forklaring"
            )
        ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_arbeidsforhold") {
            val handling = OverstyrArbeidsforholdHandlingFraApi(
                fødselsnummer,
                aktørId,
                skjæringstidspunkt,
                overstyrteArbeidsforhold
            )
            testMediator.håndter(handling, saksbehandler)
            sisteMeldingId = meldingssender.sendOverstyrtArbeidsforhold(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                saksbehandlerOid = SAKSBEHANDLER_OID,
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
        håndterOverstyringIgangsatt(fødselsnummer, hendelseId)
        håndterUtbetalingErstattet(aktørId, fødselsnummer, organisasjonsnummer)
        håndterVedtaksperiodeReberegnet(aktørId, fødselsnummer, organisasjonsnummer)
    }

    private fun håndterOverstyringIgangsatt(fødselsnummer: String, kildeId: UUID) {
        sisteMeldingId = meldingssender.sendOverstyringIgangsatt(
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

    protected fun assertUtbetalinger(utbetalingId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM utbetaling_id ui INNER JOIN utbetaling u on ui.id = u.utbetaling_id_ref WHERE ui.utbetaling_id = ?"
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

    protected fun assertGodkjenningsbehovBesvart(
        godkjent: Boolean,
        automatiskBehandlet: Boolean,
        vararg årsakerTilAvvist: String,
    ) {
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
        val query =
            "SELECT status FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?) ORDER by id DESC"
        val sisteOppgavestatus = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId
                ).map { enumValueOf<Oppgavestatus>(it.string("status")) }.asSingle
            )
        }
        assertEquals(oppgavestatus, sisteOppgavestatus)
    }

    protected fun assertHarOppgaveegenskap(oppgaveId: Int, vararg forventedeEgenskaper: Egenskap) {
        val egenskaper = hentOppgaveegenskaper(oppgaveId)
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList()))
    }

    protected fun assertHarIkkeOppgaveegenskap(oppgaveId: Int, vararg forventedeEgenskaper: Egenskap) {
        val egenskaper = hentOppgaveegenskaper(oppgaveId)
        assertTrue(egenskaper.none { it in forventedeEgenskaper })
    }

    private fun hentOppgaveegenskaper(oppgaveId: Int): Set<Egenskap> {
        @Language("PostgreSQL")
        val query = "select egenskaper from oppgave o where id = :oppgaveId"
        val egenskaper = requireNotNull(sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("oppgaveId" to oppgaveId)).map { row ->
                row.array<String>("egenskaper").map<String, Egenskap>(::enumValueOf).toSet()
            }.asSingle)
        }) { "Forventer å finne en oppgave for id=$oppgaveId" }

        return egenskaper
    }

    protected fun assertSaksbehandleroppgaveBleIkkeOpprettet(
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

    protected fun assertGodkjentVarsel(vedtaksperiodeId: UUID, varselkode: String) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = ? AND kode = ? AND status = 'GODKJENT'"
            session.run(queryOf(query, vedtaksperiodeId, varselkode).map { it.int(1) }.asSingle)
        }
        assertEquals(1, antall)
    }

    protected fun assertSkjermet(fødselsnummer: String = FØDSELSNUMMER, skjermet: Boolean?) {
        assertEquals(skjermet, EgenAnsattDao(dataSource).erEgenAnsatt(fødselsnummer))
    }

    protected fun assertAdressebeskyttelse(
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse?,
    ) {
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
        assertEquals(
            1,
            person(fødselsnummer, aktørId)
        ) { "Person med fødselsnummer=$fødselsnummer og aktørId=$aktørId finnes ikke i databasen" }
    }

    protected fun assertHarPersoninfo(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT info_ref FROM person WHERE fodselsnummer = ?"
            session.run(queryOf(query, fødselsnummer.toLong()).map { it }.asSingle)
                ?: fail("Fant ikke personinfo for $fødselsnummer")
        }

    protected fun assertPersonEksistererIkke(fødselsnummer: String, aktørId: String) {
        assertEquals(0, person(fødselsnummer, aktørId))
    }

    protected fun assertArbeidsgiverEksisterer(organisasjonsnummer: String) {
        assertEquals(
            1,
            arbeidsgiver(organisasjonsnummer)
        ) { "Arbeidsgiver med organisasjonsnummer=$organisasjonsnummer finnes ikke i databasen" }
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
        val utførteOverstyringstyper = testMediator.overstyringstyperForVedtaksperiode(vedtaksperiodeId)
        assertEquals(forventedeOverstyringstyper.toSet(), utførteOverstyringstyper.toSet()) {
            val ikkeEtterspurt = utførteOverstyringstyper.toSet() - forventedeOverstyringstyper.toSet()
            "Følgende overstyringstyper ble utført i tillegg til de forventede: $ikkeEtterspurt\nForventede typer: ${forventedeOverstyringstyper.joinToString()}\n"
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

    private fun List<OverstyrArbeidsgiverFraApi>.byggOverstyrArbeidsgiverEvent() = this.map {
        OverstyrtArbeidsgiverEvent(
            organisasjonsnummer = it.organisasjonsnummer,
            månedligInntekt = it.månedligInntekt,
            fraMånedligInntekt = it.fraMånedligInntekt,
            refusjonsopplysninger = it.refusjonsopplysninger?.byggRefusjonselementEvent(),
            fraRefusjonsopplysninger = it.fraRefusjonsopplysninger?.byggRefusjonselementEvent(),
            begrunnelse = it.begrunnelse,
            forklaring = it.forklaring,
            subsumsjon = it.lovhjemmel?.byggLovhjemmelEvent(),
        )
    }

    private fun List<RefusjonselementFraApi>.byggRefusjonselementEvent() = this.map {
        OverstyrtRefusjonselementEvent(
            fom = it.fom,
            tom = it.tom,
            beløp = it.beløp,
        )
    }

    private fun LovhjemmelFraApi.byggLovhjemmelEvent() =
        LovhjemmelEvent(paragraf, ledd, bokstav, lovverk, lovverksversjon)

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING"
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
                ).asUpdate
            )
        }
    }

    protected enum class Kommandokjedetilstand {
        NY,
        SUSPENDERT,
        FERDIG,
        AVBRUTT
    }
}

