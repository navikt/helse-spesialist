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
import no.nav.helse.TestRapidHelpers.løsningOrNull
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.Testdata.snapshot
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.SENDT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach

internal abstract class AbstractE2ETestV2 : AbstractDatabaseTest() {
    private lateinit var utbetalingId: UUID
    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val testRapid = TestRapid()
    private val meldingssenderV2 = MeldingssenderV2(testRapid)
    private val testMediator = TestMediator(testRapid, snapshotClient, dataSource)

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        lagVarseldefinisjoner()
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
    ) {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()
        håndterGodkjenningsbehov(
            andreArbeidsforhold = andreArbeidsforhold,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt
        )
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        if (andreArbeidsforhold.isNotEmpty()) håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning(regelverksvarsler = regelverksvarsler)
        håndterEgenansattløsning()
        håndterVergemålløsning(fullmakter = fullmakter)
        håndterDigitalKontaktinformasjonløsning()
    }

    protected fun fremTilSaksbehandleroppgave(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
    ) {
        fremTilÅpneOppgaver(fom, tom, skjæringstidspunkt, andreArbeidsforhold, regelverksvarsler, fullmakter)
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning(kanGodkjennesAutomatisk = false, risikofunn = risikofunn)
    }

    protected fun nyttVedtak(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
        andreArbeidsforhold: List<String> = emptyList(),
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
        risikofunn: List<Risikofunn> = emptyList(),
    ) {
        fremTilSaksbehandleroppgave(
            fom,
            tom,
            skjæringstidspunkt,
            andreArbeidsforhold,
            regelverksvarsler,
            fullmakter,
            risikofunn
        )
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
    }

    protected fun håndterSøknad(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        meldingssenderV2.sendSøknadSendt(aktørId, fødselsnummer, organisasjonsnummer)
        assertPersonEksisterer(fødselsnummer, aktørId)
        assertArbeidsgiverEksisterer(organisasjonsnummer)
    }

    protected fun håndterVedtaksperiodeOpprettet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        meldingssenderV2.sendVedtaksperiodeEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            forrigeTilstand = "START"
        )
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
    }

    protected fun håndterVedtaksperiodeEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        forårsaketAvId: UUID = UUID.randomUUID(),
        erRevurdering: Boolean = false,
    ) {
        meldingssenderV2.sendVedtaksperiodeEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forrigeTilstand = if (erRevurdering) "AVVENTER_SIMULERING_REVURDERING" else "AVVENTER_SIMULERING",
            gjeldendeTilstand = if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
            forårsaketAvId = forårsaketAvId
        )
    }

    protected fun håndterVedtaksperiodeNyUtbetaling(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UUID.randomUUID(),
    ) {
        nyUtbetalingId(utbetalingId)
        meldingssenderV2.sendVedtaksperiodeNyUtbetaling(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId
        )
    }

    protected fun håndterAktivitetsloggNyAktivitet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        varselkoder: List<String> = emptyList(),
    ) {
        meldingssenderV2.sendAktivitetsloggNyAktivitet(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            varselkoder = varselkoder
        )
    }

    protected fun håndterGosysOppgaveEndret(aktørId: String = AKTØR, fødselsnummer: String = FØDSELSNUMMER) {
        meldingssenderV2.sendGosysOppgaveEndret(aktørId, fødselsnummer)
        assertEtterspurteBehov("ÅpneOppgaver")
    }

    private fun håndterUtbetalingOpprettet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
    ) {
        meldingssenderV2.sendUtbetalingEndret(aktørId, fødselsnummer, organisasjonsnummer, utbetalingId, utbetalingtype)
    }

    private fun håndterUtbetalingForkastet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            type = "UTBETALING",
            forrigeStatus = NY,
            gjeldendeStatus = FORKASTET
        )
    }

    protected fun håndterUtbetalingUtbetalt(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        meldingssenderV2.sendUtbetalingEndret(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            type = "UTBETALING",
            forrigeStatus = SENDT,
            gjeldendeStatus = UTBETALT
        )
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

        meldingssenderV2.sendUtbetalingAnnullert(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            epost = SAKSBEHANDLER_EPOST,
            arbeidsgiverFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = true),
            personFagsystemId = fagsystemidFor(utbetalingId, tilArbeidsgiver = false),
        )
    }

    protected fun håndterGodkjenningsbehov(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        harOppdatertMetainfo: Boolean = false,
        andreArbeidsforhold: List<String> = emptyList(),
    ) {
        val erRevurdering = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT true FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND låst = true ORDER BY id DESC"
            session.run(queryOf(query, vedtaksperiodeId).map { it.boolean(1) }.asSingle) ?: false
        }

        håndterUtbetalingOpprettet(utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING")
        håndterVedtaksperiodeEndret(erRevurdering = erRevurdering)
        meldingssenderV2.sendGodkjenningsbehov(
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
        if (!harOppdatertMetainfo) assertEtterspurteBehov("HentPersoninfoV2")
        else assertEtterspurteBehov("EgenAnsatt")
    }

    protected fun håndterPersoninfoløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        meldingssenderV2.sendPersoninfoløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterEnhetløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        meldingssenderV2.sendEnhetløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterInfotrygdutbetalingerløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        meldingssenderV2.sendInfotrygdutbetalingerløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterArbeidsgiverinformasjonløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        meldingssenderV2.sendArbeidsgiverinformasjonløsning(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId
        )
    }

    protected fun håndterArbeidsforholdløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        regelverksvarsler: List<String> = emptyList(),
    ) {
        every { snapshotClient.hentSnapshot(fødselsnummer) } returns snapshot(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            regelverksvarsler = regelverksvarsler
        )
        meldingssenderV2.sendArbeidsforholdløsning(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        verify { snapshotClient.hentSnapshot(fødselsnummer) }
    }

    protected fun håndterEgenansattløsning(aktørId: String = AKTØR, fødselsnummer: String = FØDSELSNUMMER) {
        meldingssenderV2.sendEgenAnsattløsning(aktørId, fødselsnummer, false)
    }

    protected fun håndterVergemålløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        fullmakter: List<Fullmakt> = emptyList(),
    ) {
        meldingssenderV2.sendVergemålløsning(aktørId, fødselsnummer, fullmakter)
    }

    protected fun håndterDigitalKontaktinformasjonløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        meldingssenderV2.sendDigitalKontaktinformasjonløsning(aktørId, fødselsnummer)
    }

    protected fun håndterÅpneOppgaverløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
    ) {
        meldingssenderV2.sendÅpneGosysOppgaverløsning(aktørId, fødselsnummer, antall, oppslagFeilet)
    }

    protected fun håndterRisikovurderingløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        kanGodkjennesAutomatisk: Boolean = true,
        risikofunn: List<Risikofunn> = emptyList(),
    ) {
        meldingssenderV2.sendRisikovurderingløsning(
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
        meldingssenderV2.sendSaksbehandlerløsning(
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
        meldingssenderV2.sendVedtakFattet(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
    }

    protected fun håndterOverstyrTidslinje(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_tidslinje") {
            meldingssenderV2.sendOverstyrTidslinje(aktørId, fødselsnummer, organisasjonsnummer)
        }
    }

    protected fun håndterOverstyrInntekt(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_inntekt") {
            meldingssenderV2.sendOverstyrtInntekt(aktørId, fødselsnummer, organisasjonsnummer)
        }
    }

    protected fun håndterOverstyrArbeidsforhold(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer, "overstyr_arbeidsforhold") {
            meldingssenderV2.sendOverstyrtArbeidsforhold(aktørId, fødselsnummer, organisasjonsnummer)
        }
    }

    private fun håndterOverstyring(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        overstyringHendelse: String,
        overstyringBlock: () -> Unit,
    ) {
        testRapid.reset()
        overstyringBlock()
        val hendelser = testRapid.inspektør.hendelser(overstyringHendelse)
        assertEquals(1, hendelser.size)
        val overstyring = hendelser.single()
        val hendelseId = UUID.fromString(overstyring["@id"].asText())
        håndterUtbetalingForkastet(aktørId, fødselsnummer, organisasjonsnummer)
        håndterVedtaksperiodeEndret(forårsaketAvId = hendelseId)
        håndterGodkjenningsbehov(harOppdatertMetainfo = true)
    }

    protected fun assertVarsler(vedtaksperiodeId: UUID, forventetAntall: Int) {
        val antall = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = ?"
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
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

    private fun assertUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse)
        assertEquals(1, meldinger.size)
    }

    private fun assertUtgåendeBehovløsning(behov: String) {
        val løsning = testRapid.inspektør.løsningOrNull(behov)
        assertNotNull(løsning)
    }

    private fun assertEtterspurteBehov(vararg behov: String) {
        val etterspurteBehov = testRapid.inspektør.behov()
        assertEquals(behov.toList(), etterspurteBehov) {
            val ikkeEtterspurt = behov.toSet() - etterspurteBehov.toSet()
            "Følgende behov ble ikke etterspurt: $ikkeEtterspurt\nEtterspurte behov: $etterspurteBehov\n"
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
        sessionOf(dataSource).use { session ->
            val varselkoder = Varselkode.values()
            varselkoder.forEach { varselkode ->
                @Language("PostgreSQL")
                val query = "INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING"
                session.run(
                    queryOf(
                        query,
                        UUID.nameUUIDFromBytes(varselkode.name.toByteArray()),
                        varselkode.name,
                        "En tittel for varselkode=${varselkode.name}",
                        "En forklaring for varselkode=${varselkode.name}",
                        "En handling for varselkode=${varselkode.name}",
                        false,
                        LocalDateTime.now()
                    ).asUpdate)
            }
        }
    }
}