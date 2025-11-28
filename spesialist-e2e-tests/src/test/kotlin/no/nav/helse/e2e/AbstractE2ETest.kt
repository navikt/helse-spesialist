package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.Meldingssender
import no.nav.helse.TestMediator
import no.nav.helse.Testdata.snapshot
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.SENDT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiLovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.client.spleis.SpleisClientSnapshothenter
import no.nav.helse.spesialist.db.DataSourceDbQuery
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.behov
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.hendelser
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.løsning
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.siste
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.sisteBehov
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractE2ETest : AbstractDatabaseTest() {
    protected val testperson = TestPerson().also { println("Bruker testdata: $it") }
    protected val dbQuery = DataSourceDbQuery(dataSource)

    val FØDSELSNUMMER = testperson.fødselsnummer
    val ORGNR =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnummer
        }
    val AKTØR = testperson.aktørId
    val VEDTAKSPERIODE_ID = testperson.vedtaksperiodeId1
    val UTBETALING_ID = testperson.utbetalingId1
    private val vilkårsgrunnlagId = UUID.randomUUID()
    private val behandlinger = mutableMapOf<UUID, MutableList<UUID>>()
    protected val godkjenningsbehovTestdata
        get() =
            GodkjenningsbehovTestdata(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                utbetalingId = UTBETALING_ID,
                spleisBehandlingId = behandlinger.getValue(VEDTAKSPERIODE_ID).last(),
                vilkårsgrunnlagId = vilkårsgrunnlagId,
            )
    private val avviksvurderingTestdata = AvviksvurderingTestdata()
    lateinit var utbetalingId: UUID
        private set
    val spleisClient = mockk<SpleisClient>()
    val snapshothenter = SpleisClientSnapshothenter(spleisClient)
    private val testRapid = TestRapid()
    val inspektør get() = testRapid.inspektør
    private val meldingssender = Meldingssender(testRapid)
    protected lateinit var sisteMeldingId: UUID
    protected lateinit var sisteGodkjenningsbehovId: UUID
    private val testMediator = TestMediator(testRapid, dataSource)
    protected val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected val SAKSBEHANDLER_EPOST = "augunn.saksbehandler@nav.no"
    protected val SAKSBEHANDLER_IDENT = "S199999"
    protected val SAKSBEHANDLER_NAVN = "Augunn Saksbehandler"
    private val saksbehandler =
        Saksbehandler(
            id = SaksbehandlerOid(value = SAKSBEHANDLER_OID),
            navn = SAKSBEHANDLER_NAVN,
            epost = SAKSBEHANDLER_EPOST,
            ident = SAKSBEHANDLER_IDENT,
        )
    private val enhetsnummerOslo = "0301"

    @BeforeEach
    fun resetTestSetup() {
        resetTestRapid()
        lagVarseldefinisjoner()
        opprettSaksbehandler()
    }

    private fun resetTestRapid() = testRapid.reset()

    private fun opprettSaksbehandler() =
        dbQuery.update(
            """
            INSERT INTO saksbehandler
            VALUES (:oid, :navn, :epost, :ident)
            ON CONFLICT (oid) DO NOTHING
            """.trimIndent(),
            "oid" to SAKSBEHANDLER_OID,
            "navn" to SAKSBEHANDLER_NAVN,
            "epost" to SAKSBEHANDLER_EPOST,
            "ident" to SAKSBEHANDLER_IDENT,
        )

    protected fun Int.oppgave(vedtaksperiodeId: UUID): Long {
        require(this > 0) { "Forventet oppgaveId for vedtaksperiodeId=$vedtaksperiodeId må være større enn 0" }
        val oppgaveIder =
            dbQuery.list(
                "SELECT id FROM oppgave WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)",
                "vedtaksperiodeId" to vedtaksperiodeId,
            ) { it.long("id") }
        assertTrue(oppgaveIder.size >= this) {
            "Forventer at det finnes minimum $this antall oppgaver for vedtaksperiodeId=$vedtaksperiodeId. Fant ${oppgaveIder.size} oppgaver."
        }
        return oppgaveIder[this - 1]
    }

    private fun nyUtbetalingId(utbetalingId: UUID) {
        this.utbetalingId = utbetalingId
    }

    protected fun spesialistInnvilgerAutomatisk(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        avviksvurderingTestdata: AvviksvurderingTestdata = AvviksvurderingTestdata(skjæringstidspunkt = skjæringstidspunkt),
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilRisikovurdering(
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata =
                godkjenningsbehovTestdata.copy(
                    periodeFom = fom,
                    periodeTom = tom,
                    skjæringstidspunkt = skjæringstidspunkt,
                ),
        )
        håndterRisikovurderingløsning(vedtaksperiodeId = vedtaksperiodeId)
        håndterUtbetalingUtbetalt()
        håndterAvsluttetMedVedtak(
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId =
                behandlinger[vedtaksperiodeId]?.last()
                    ?: throw IllegalArgumentException("Det finnes ingen behandlinger for vedtaksperiodeId=$vedtaksperiodeId"),
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
        )
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilVergemål(
        regelverksvarsler: List<String> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        enhet: String = enhetsnummerOslo,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilEgenAnsatt(
            regelverksvarsler,
            harOppdatertMetadata = harOppdatertMetadata,
            enhet = enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        if (!harOppdatertMetadata) håndterEgenansattløsning(fødselsnummer = godkjenningsbehovTestdata.fødselsnummer)
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Testmeldingfabrikk.VergemålJson.Fullmakt> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        enhet: String = enhetsnummerOslo,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilVergemål(
            regelverksvarsler,
            harOppdatertMetadata,
            enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        håndterVergemålOgFullmaktløsning(
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            fullmakter = fullmakter,
        )
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilEgenAnsatt(
        regelverksvarsler: List<String> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        enhet: String = enhetsnummerOslo,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        if (regelverksvarsler.isNotEmpty()) {
            håndterAktivitetsloggNyAktivitet(
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                varselkoder = regelverksvarsler,
            )
        }
        håndterGodkjenningsbehov(
            harOppdatertMetainfo = harOppdatertMetadata,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )

        if (!harOppdatertMetadata) {
            håndterPersoninfoløsning(fødselsnummer = godkjenningsbehovTestdata.fødselsnummer)
            håndterEnhetløsning(
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
                enhet = enhet,
            )
            håndterInfotrygdutbetalingerløsning(
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            )
            håndterArbeidsgiverinformasjonløsning(
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            )
            håndterArbeidsforholdløsning(
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            )
        }
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilOppgave(
        enhet: String = enhetsnummerOslo,
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Testmeldingfabrikk.VergemålJson.Fullmakt> = emptyList(),
        risikofunn: List<Testmeldingfabrikk.Risikofunn> = emptyList(),
        harRisikovurdering: Boolean = false,
        harOppdatertMetadata: Boolean = false,
        kanGodkjennesAutomatisk: Boolean = false,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilRisikovurdering(
            enhet = enhet,
            regelverksvarsler = regelverksvarsler,
            fullmakter = fullmakter,
            harOppdatertMetadata = harOppdatertMetadata,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        if (!harRisikovurdering) {
            håndterRisikovurderingløsning(
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                risikofunn = risikofunn,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            )
        }
        if (!erFerdigstilt(sisteGodkjenningsbehovId)) håndterInntektløsning(fødselsnummer = godkjenningsbehovTestdata.fødselsnummer)
    }

    private fun spesialistBehandlerGodkjenningsbehovFremTilRisikovurdering(
        enhet: String = enhetsnummerOslo,
        regelverksvarsler: List<String> = emptyList(),
        fullmakter: List<Testmeldingfabrikk.VergemålJson.Fullmakt> = emptyList(),
        harOppdatertMetadata: Boolean = false,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(
            regelverksvarsler,
            fullmakter,
            harOppdatertMetadata = harOppdatertMetadata,
            enhet = enhet,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            avviksvurderingTestdata = avviksvurderingTestdata,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )
        håndterÅpneOppgaverløsning(fødselsnummer = godkjenningsbehovTestdata.fødselsnummer)
    }

    protected fun vedtaksløsningenMottarNySøknad(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
    ) {
        sisteMeldingId = meldingssender.sendSøknadSendt(aktørId, fødselsnummer, organisasjonsnummer)
        assertIngenEtterspurteBehov()
        assertPersonEksisterer(fødselsnummer, aktørId)
    }

    protected fun spleisOppretterNyBehandling(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        spleisBehandlingId: UUID = UUID.randomUUID(),
    ) {
        behandlinger.getOrPut(vedtaksperiodeId) { mutableListOf() }.addLast(spleisBehandlingId)
        sisteMeldingId =
            meldingssender.sendBehandlingOpprettet(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                spleisBehandlingId = spleisBehandlingId,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            )
        assertIngenEtterspurteBehov()
        assertVedtaksperiodeEksisterer(vedtaksperiodeId)
    }

    protected fun håndterVedtaksperiodeEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        forårsaketAvId: UUID = UUID.randomUUID(),
        forrigeTilstand: String? = null,
        gjeldendeTilstand: String? = null,
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId =
            meldingssender.sendVedtaksperiodeEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forrigeTilstand =
                    forrigeTilstand
                        ?: if (erRevurdering) "AVVENTER_SIMULERING_REVURDERING" else "AVVENTER_SIMULERING",
                gjeldendeTilstand =
                    gjeldendeTilstand
                        ?: if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
                forårsaketAvId = forårsaketAvId,
            )
    }

    protected fun håndterVedtaksperiodeReberegnet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        forårsaketAvId: UUID = UUID.randomUUID(),
    ) {
        val erRevurdering = erRevurdering(vedtaksperiodeId)
        sisteMeldingId =
            meldingssender.sendVedtaksperiodeEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forrigeTilstand = if (erRevurdering) "AVVENTER_GODKJENNING_REVURDERING" else "AVVENTER_GODKJENNING",
                gjeldendeTilstand = if (erRevurdering) "AVVENTER_HISTORIKK_REVURDERING" else "AVVENTER_HISTORIKK",
                forårsaketAvId = forårsaketAvId,
            )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterVedtaksperiodeForkastet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
    ) {
        sisteMeldingId =
            meldingssender.sendVedtaksperiodeForkastet(
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
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        utbetalingId: UUID = testperson.utbetalingId1,
    ) {
        if (!this::utbetalingId.isInitialized || utbetalingId != this.utbetalingId) nyUtbetalingId(utbetalingId)
        sisteMeldingId =
            meldingssender.sendVedtaksperiodeNyUtbetaling(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = this.utbetalingId,
            )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterAktivitetsloggNyAktivitet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        varselkoder: List<String> = emptyList(),
    ) {
        varselkoder.forEach {
            lagVarseldefinisjon(it)
        }
        sisteMeldingId =
            meldingssender.sendAktivitetsloggNyAktivitet(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                varselkoder = varselkoder,
            )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterEndretSkjermetinfo(
        fødselsnummer: String = FØDSELSNUMMER,
        skjermet: Boolean,
    ) {
        sisteMeldingId = meldingssender.sendEndretSkjermetinfo(fødselsnummer, skjermet)
        if (!skjermet) {
            assertIngenEtterspurteBehov()
        }
    }

    protected fun håndterGosysOppgaveEndret(fødselsnummer: String = FØDSELSNUMMER) {
        sisteMeldingId = meldingssender.sendGosysOppgaveEndret(fødselsnummer)
    }

    protected fun håndterTilbakedateringBehandlet(
        fødselsnummer: String = FØDSELSNUMMER,
        perioder: List<Periode>,
    ) {
        sisteMeldingId = meldingssender.sendTilbakedateringBehandlet(fødselsnummer, perioder)
    }

    protected fun håndterKommandokjedePåminnelse(
        commandContextId: UUID,
        meldingId: UUID,
    ) {
        sisteMeldingId = meldingssender.sendKommandokjedePåminnelse(commandContextId, meldingId)
    }

    protected fun håndterUtbetalingOpprettet(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        utbetalingtype: String = "UTBETALING",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        utbetalingId: UUID = testperson.utbetalingId1,
    ) {
        nyUtbetalingId(utbetalingId)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            utbetalingId = this.utbetalingId,
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
        utbetalingId: UUID,
    ) {
        håndterUtbetalingForkastet(aktørId, fødselsnummer, organisasjonsnummer)
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            utbetalingId = utbetalingId,
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
        sisteMeldingId =
            meldingssender.sendUtbetalingEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = this.utbetalingId,
                type = utbetalingtype,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                forrigeStatus = forrigeStatus,
                gjeldendeStatus = gjeldendeStatus,
                opprettet = opprettet,
            )
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
            utbetalingId = this.utbetalingId,
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
            utbetalingId = this.utbetalingId,
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterGodkjenningsbehovUtenValidering(
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        val erRevurdering = erRevurdering(godkjenningsbehovTestdata.vedtaksperiodeId)
        håndterVedtaksperiodeNyUtbetaling(
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
            utbetalingId = godkjenningsbehovTestdata.utbetalingId,
        )
        håndterUtbetalingOpprettet(
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING",
            utbetalingId = godkjenningsbehovTestdata.utbetalingId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )
        håndterVedtaksperiodeEndret(
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
        )
        sisteMeldingId = sendGodkjenningsbehov(godkjenningsbehovTestdata)
        sisteGodkjenningsbehovId = sisteMeldingId
    }

    protected fun håndterGodkjenningsbehov(
        harOppdatertMetainfo: Boolean = false,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        avviksvurderingTestdata: AvviksvurderingTestdata = this.avviksvurderingTestdata,
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata = this.godkjenningsbehovTestdata,
    ) {
        val arbeidsgivereSomSkalLagres = godkjenningsbehovTestdata.orgnummereMedRelevanteArbeidsforhold.toSet()
        val lagredeArbeidsgivere =
            sessionFactory.transactionalSessionScope {
                it.arbeidsgiverRepository.finnAlle(
                    arbeidsgivereSomSkalLagres
                        .map(ArbeidsgiverIdentifikator::fraString)
                        .toSet(),
                )
            }
        håndterGodkjenningsbehovUtenValidering(
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata,
        )

        håndterAvviksvurderingløsning(
            fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
            organisasjonsnummer = godkjenningsbehovTestdata.organisasjonsnummer,
            sammenligningsgrunnlagTotalbeløp = avviksvurderingTestdata.sammenligningsgrunnlag,
            avviksprosent = avviksvurderingTestdata.avviksprosent,
            avviksvurderingId = avviksvurderingTestdata.avviksvurderingId,
        )

        when {
            !harOppdatertMetainfo -> assertEtterspurteBehov("HentPersoninfoV2")
            arbeidsgivereSomSkalLagres.size != lagredeArbeidsgivere.size -> assertEtterspurteBehov("Arbeidsgiverinformasjon")
            else -> assertEtterspurteBehov("Vergemål", "Fullmakt")
        }
    }

    private fun sendGodkjenningsbehov(godkjenningsbehovTestdata: GodkjenningsbehovTestdata) = meldingssender.sendGodkjenningsbehov(godkjenningsbehovTestdata).also { sisteMeldingId = it }

    private fun håndterAvviksvurderingløsning(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        sammenligningsgrunnlagTotalbeløp: Double,
        avviksprosent: Double,
        avviksvurderingId: UUID,
    ) {
        assertEtterspurteBehov("Avviksvurdering")
        sisteMeldingId =
            meldingssender.sendAvviksvurderingløsning(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                sammenligningsgrunnlagTotalbeløp = sammenligningsgrunnlagTotalbeløp,
                avviksprosent = avviksprosent,
                avviksvurderingId = avviksvurderingId,
            )
    }

    protected fun håndterPersoninfoløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ) {
        assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId =
            meldingssender.sendPersoninfoløsning(
                aktørId,
                fødselsnummer,
                adressebeskyttelse,
            )
    }

    protected fun håndterPersoninfoløsningUtenValidering(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ) {
        sisteMeldingId =
            meldingssender.sendPersoninfoløsning(
                aktørId,
                fødselsnummer,
                adressebeskyttelse,
            )
    }

    protected fun håndterEnhetløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
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
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
    ) {
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
        sisteMeldingId =
            meldingssender.sendInfotrygdutbetalingerløsning(
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
            )
    }

    protected fun håndterArbeidsgiverinformasjonløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        arbeidsgiverinformasjonJson: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson>? = null,
    ) {
        val erKompositt = testRapid.inspektør.sisteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2") != null
        if (erKompositt) {
            assertEtterspurteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2")
            sisteMeldingId =
                meldingssender.sendArbeidsgiverinformasjonKompositt(
                    aktørId,
                    fødselsnummer,
                    organisasjonsnummer,
                    vedtaksperiodeId,
                )
            return
        }
        assertEtterspurteBehov("Arbeidsgiverinformasjon")
        sisteMeldingId =
            meldingssender.sendArbeidsgiverinformasjonløsning(
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                arbeidsgiverinformasjonJson,
            )
    }

    protected fun håndterArbeidsforholdløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
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

    protected fun håndterVergemålOgFullmaktløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        vergemål: List<Testmeldingfabrikk.VergemålJson.Vergemål> = emptyList(),
        fremtidsfullmakter: List<Testmeldingfabrikk.VergemålJson.Vergemål> = emptyList(),
        fullmakter: List<Testmeldingfabrikk.VergemålJson.Fullmakt> = emptyList(),
    ) {
        assertEtterspurteBehov("Vergemål", "Fullmakt")
        sisteMeldingId =
            meldingssender.sendVergemålOgFullmaktløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vergemål = vergemål,
                fremtidsfullmakter = fremtidsfullmakter,
                fullmakter = fullmakter,
            )
    }

    protected fun håndterInntektløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        assertEtterspurteBehov("InntekterForSykepengegrunnlag")
        sisteMeldingId = meldingssender.sendInntektløsning(aktørId, fødselsnummer, ORGNR)
    }

    protected fun håndterÅpneOppgaverløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        antallÅpneOppgaverIGosys: Int = 0,
        oppslagFeilet: Boolean = false,
    ) {
        assertEtterspurteBehov("ÅpneOppgaver")
        sisteMeldingId =
            meldingssender.sendÅpneGosysOppgaverløsning(aktørId, fødselsnummer, antallÅpneOppgaverIGosys, oppslagFeilet)
    }

    protected fun håndterRisikovurderingløsning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        kanGodkjennesAutomatisk: Boolean = true,
        risikofunn: List<Testmeldingfabrikk.Risikofunn> = emptyList(),
    ) {
        assertEtterspurteBehov("Risikovurdering")
        sisteMeldingId =
            meldingssender.sendRisikovurderingløsning(
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                kanGodkjennesAutomatisk,
                risikofunn,
            )
    }

    protected fun saksbehandlerVurdererVarsel(
        vedtaksperiodeId: UUID,
        varselkode: String,
        saksbehandlerIdent: String = SAKSBEHANDLER_IDENT,
    ) {
        dbQuery.update(
            """
            UPDATE selve_varsel 
            SET status = 'VURDERT', status_endret_ident = :ident, status_endret_tidspunkt = now()
            WHERE vedtaksperiode_id = :vedtaksperiodeId AND kode = :varselkode
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "varselkode" to varselkode,
            "ident" to saksbehandlerIdent,
        )
    }

    protected fun håndterAvsluttetMedVedtak(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        spleisBehandlingId: UUID = behandlinger.getValue(vedtaksperiodeId).last(),
        fastsattType: String = "EtterHovedregel",
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
    ) {
        val utbetalingId = if (this::utbetalingId.isInitialized) this.utbetalingId else null
        if (utbetalingId != null) håndterUtbetalingUtbetalt(aktørId, fødselsnummer, organisasjonsnummer)
        sisteMeldingId =
            meldingssender.sendAvsluttetMedVedtak(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                fastsattType = fastsattType,
            )
    }

    protected fun håndterAvsluttetUtenVedtak(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        spleisBehandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 11.januar,
        skjæringstidspunkt: LocalDate = fom,
    ) {
        sisteMeldingId =
            meldingssender.sendAvsluttetUtenVedtak(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
            )
    }

    protected fun håndterAdressebeskyttelseEndret(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        harOppdatertMetadata: Boolean = true,
    ) {
        if (!harOppdatertMetadata) assertEtterspurteBehov("HentPersoninfoV2")
        sisteMeldingId = meldingssender.sendAdressebeskyttelseEndret(aktørId, fødselsnummer)
    }

    protected fun håndterOppdaterPersondata(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        sisteMeldingId = meldingssender.sendOppdaterPersondata(aktørId, fødselsnummer)
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
    }

    protected fun håndterSkalKlargjøresForVisning(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
    ) {
        sisteMeldingId = meldingssender.sendKlargjørPersonForVisning(aktørId, fødselsnummer)
        assertEtterspurteBehov("HentPersoninfoV2")
    }

    protected fun håndterOverstyrTidslinje(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        dager: List<ApiOverstyringDag> =
            listOf(
                ApiOverstyringDag(
                    1.januar(1970),
                    Dagtype.Feriedag.toString(),
                    Dagtype.Sykedag.toString(),
                    null,
                    100,
                    null,
                ),
            ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer) {
            val handling =
                ApiTidslinjeOverstyring(
                    vedtaksperiodeId,
                    organisasjonsnummer,
                    fødselsnummer,
                    aktørId,
                    "En begrunnelse",
                    dager,
                )
            testMediator.håndter(handling, saksbehandler, emptySet())
            // Her må det gjøres kall til api for å sende inn overstyring av tidslinje
        }
    }

    protected fun håndterOverstyrInntektOgRefusjon(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        arbeidsgivere: List<ApiOverstyringArbeidsgiver> =
            listOf(
                ApiOverstyringArbeidsgiver(
                    organisasjonsnummer = ORGNR,
                    manedligInntekt = 25000.0,
                    fraManedligInntekt = 25001.0,
                    forklaring = "testbortforklaring",
                    lovhjemmel = ApiLovhjemmel("8-28", "LEDD_1", "BOKSTAV_A", "folketrygdloven", "1970-01-01"),
                    refusjonsopplysninger = null,
                    fraRefusjonsopplysninger = null,
                    begrunnelse = "en begrunnelse",
                    fom = null,
                    tom = null,
                ),
            ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, ORGNR) {
            val handling =
                ApiInntektOgRefusjonOverstyring(
                    aktørId,
                    fødselsnummer,
                    skjæringstidspunkt,
                    arbeidsgivere,
                    vedtaksperiodeId,
                )
            testMediator.håndter(handling, saksbehandler, emptySet())
        }
    }

    protected fun håndterOverstyrArbeidsforhold(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        skjæringstidspunkt: LocalDate = 1.januar,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        overstyrteArbeidsforhold: List<ApiOverstyringArbeidsforhold> =
            listOf(
                ApiOverstyringArbeidsforhold(
                    orgnummer = organisasjonsnummer,
                    deaktivert = true,
                    begrunnelse = "begrunnelse",
                    forklaring = "forklaring",
                    lovhjemmel = ApiLovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                ),
            ),
    ) {
        håndterOverstyring(aktørId, fødselsnummer, organisasjonsnummer) {
            val handling =
                ApiArbeidsforholdOverstyringHandling(
                    fodselsnummer = fødselsnummer,
                    aktorId = aktørId,
                    skjaringstidspunkt = skjæringstidspunkt,
                    overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                    vedtaksperiodeId = vedtaksperiodeId,
                )
            testMediator.håndter(handling, saksbehandler, emptySet())
        }
    }

    private fun håndterOverstyring(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        overstyringBlock: () -> Unit,
    ) {
        overstyringBlock()
        håndterVedtaksperiodeReberegnet(aktørId, fødselsnummer, organisasjonsnummer)
        håndterUtbetalingErstattet(aktørId, fødselsnummer, organisasjonsnummer, utbetalingId = UUID.randomUUID())
    }

    private fun erRevurdering(vedtaksperiodeId: UUID) =
        dbQuery.singleOrNull(
            """
            SELECT 1 FROM behandling
            WHERE vedtaksperiode_id = :vedtaksperiodeId AND tilstand = '${LegacyBehandling.VedtakFattet.navn()}'
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ) { true } ?: false

    protected fun assertUtbetalinger(
        utbetalingId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                """
                SELECT COUNT(1) FROM utbetaling_id ui
                INNER JOIN utbetaling u ON ui.id = u.utbetaling_id_ref
                WHERE ui.utbetaling_id = :utbetalingId
                """.trimIndent(),
                "utbetalingId" to utbetalingId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertKommandokjedetilstander(
        hendelseId: UUID,
        vararg forventedeTilstander: Kommandokjedetilstand,
    ) {
        val tilstander =
            dbQuery.list(
                "SELECT tilstand FROM command_context WHERE hendelse_id = :hendelseId ORDER BY id",
                "hendelseId" to hendelseId,
            ) { it.string("tilstand") }
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

    protected fun assertGodkjenningsbehovIkkeBesvart() = testRapid.inspektør.løsning("Godkjenningsbehov") == null

    protected fun assertSaksbehandleroppgave(
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        oppgavestatus: Oppgavestatus,
    ) {
        val sisteOppgavestatus =
            dbQuery.single(
                """
                SELECT status FROM oppgave
                WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                ORDER by id DESC
                LIMIT 1
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
            ) { enumValueOf<Oppgavestatus>(it.string("status")) }
        assertEquals(oppgavestatus, sisteOppgavestatus)
    }

    protected fun assertHarOppgaveegenskap(
        oppgaveId: Long,
        vararg forventedeEgenskaper: Egenskap,
    ) {
        val egenskaper = hentOppgaveegenskaper(oppgaveId)
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    protected fun assertHarIkkeOppgaveegenskap(
        oppgaveId: Long,
        vararg forventedeEgenskaper: Egenskap,
    ) {
        val egenskaper = hentOppgaveegenskaper(oppgaveId)
        assertTrue(egenskaper.none { it in forventedeEgenskaper }) { "Forventet at oppgaven ikke hadde egenskapen(e) ${forventedeEgenskaper.toSet()}. Egenskaper: $egenskaper" }
    }

    private fun hentOppgaveegenskaper(oppgaveId: Long): Set<Egenskap> {
        val egenskaper =
            dbQuery.singleOrNull(
                "select egenskaper from oppgave where id = :oppgaveId",
                "oppgaveId" to oppgaveId,
            ) { it.array<String>("egenskaper").map<String, Egenskap>(::enumValueOf).toSet() }
        return requireNotNull(egenskaper) { "Forventer å finne en oppgave for id=$oppgaveId" }
    }

    protected fun assertSaksbehandleroppgaveBleIkkeOpprettet(vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1) {
        val antallOppgaver =
            dbQuery.list(
                """
                SELECT 1 FROM oppgave
                JOIN vedtak v ON v.id = oppgave.vedtak_ref
                WHERE vedtaksperiode_id = :vedtaksperiodeId
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
            ) { it.int(1) }
        assertEquals(0, antallOppgaver.size)
    }

    protected fun assertVarsler(
        vedtaksperiodeId: UUID,
        forventetAntall: Int,
    ) {
        val antall =
            dbQuery.single(
                "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = :vedtaksperiodeId",
                "vedtaksperiodeId" to vedtaksperiodeId,
            ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertVarsel(
        vedtaksperiodeId: UUID,
        varselkode: String,
    ) {
        val antall =
            dbQuery.single(
                "SELECT COUNT(1) FROM selve_varsel WHERE vedtaksperiode_id = :vedtaksperiodeId AND kode = :varselkode",
                "vedtaksperiodeId" to vedtaksperiodeId,
                "varselkode" to varselkode,
            ) { it.int(1) }
        assertEquals(1, antall)
    }

    protected fun assertSkjermet(
        fødselsnummer: String = FØDSELSNUMMER,
        skjermet: Boolean?,
    ) {
        assertEquals(skjermet, daos.egenAnsattDao.erEgenAnsatt(fødselsnummer))
    }

    protected fun assertAdressebeskyttelse(
        fødselsnummer: String = FØDSELSNUMMER,
        adressebeskyttelse: Adressebeskyttelse?,
    ) {
        assertEquals(adressebeskyttelse, daos.personDao.finnAdressebeskyttelse(fødselsnummer))
    }

    protected fun assertVedtaksperiodeEksisterer(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertPersonEksisterer(
        fødselsnummer: String,
        aktørId: String,
    ) {
        assertEquals(
            1,
            person(fødselsnummer, aktørId),
        ) { "Person med fødselsnummer=$fødselsnummer og aktørId=$aktørId finnes ikke i databasen" }
    }

    protected fun assertPersonEksistererIkke(
        fødselsnummer: String,
        aktørId: String,
    ) {
        assertEquals(0, person(fødselsnummer, aktørId))
    }

    protected fun assertUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse, sisteMeldingId)
        assertEquals(1, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
        }
    }

    protected fun assertIkkeUtgåendeMelding(hendelse: String) {
        val meldinger = testRapid.inspektør.hendelser(hendelse)
        assertEquals(0, meldinger.size) {
            "Utgående meldinger: ${meldinger.joinToString { it.path("@event_name").asText() }}"
        }
    }

    protected fun assertInnholdIBehov(
        behov: String,
        block: (JsonNode) -> Unit,
    ) {
        val etterspurtBehov = testRapid.inspektør.behov(behov).last()
        block(etterspurtBehov)
    }

    private fun assertEtterspurteBehov(vararg behov: String) {
        val etterspurteBehov = testRapid.inspektør.behov(sisteMeldingId)
        val forårsaketAvId = inspektør.siste("behov")["@forårsaket_av"]["id"].asText()
        assertEquals(behov.toList().sorted(), etterspurteBehov.sorted()) {
            val ikkeEtterspurt = behov.toSet() - etterspurteBehov.toSet()
            "Forventet at følgende behov skulle være etterspurt: $ikkeEtterspurt\nFaktisk etterspurte behov: $etterspurteBehov\n"
        }
        assertEquals(forårsaketAvId, sisteMeldingId.toString())
    }

    protected fun assertIngenEtterspurteBehov() {
        assertEquals(emptyList<String>(), testRapid.inspektør.behov(sisteMeldingId))
    }

    protected fun assertSisteEtterspurteBehov(behov: String) {
        val sisteEtterspurteBehov = testRapid.inspektør.behov().last()
        assertEquals(sisteEtterspurteBehov, behov)
    }

    protected fun assertOverstyringer(fødselsnummer: String) {
        val totrinnsvurderingId =
            dbQuery.single(
                """
            select tv.id from totrinnsvurdering tv 
                inner join person p on p.id = tv.person_ref 
            where p.fødselsnummer = :fodselsnummer
                """.trimMargin(),
                "fodselsnummer" to fødselsnummer,
            ) { TotrinnsvurderingId(it.long("id")) }
        val overstyringer =
            sessionFactory.transactionalSessionScope { session ->
                session.overstyringRepository.finnAktive(totrinnsvurderingId)
            }
        assertTrue(overstyringer.isNotEmpty())
    }

    protected fun assertTotrinnsvurdering(oppgaveId: Long) {
        val erToTrinnsvurdering =
            dbQuery.singleOrNull(
                """
                SELECT 1 FROM totrinnsvurdering tv
                INNER JOIN vedtak v on tv.person_ref = v.person_ref
                INNER JOIN oppgave o on v.id = o.vedtak_ref
                WHERE o.id = :oppgaveId
                AND tv.tilstand = 'AVVENTER_SAKSBEHANDLER'
                """.trimIndent(),
                "oppgaveId" to oppgaveId,
            ) { it.boolean(1) } ?: throw IllegalStateException("Finner ikke oppgave med id $oppgaveId")
        assertTrue(erToTrinnsvurdering) {
            "Forventer at oppgaveId=$oppgaveId krever totrinnsvurdering"
        }
    }

    protected fun assertTotrinnsvurderingForkastet(fødselsnummer: String) {
        val totrinnsvurderingForkastet =
            dbQuery.singleOrNull(
                """
                SELECT 1 FROM totrinnsvurdering tv
                INNER JOIN person p on p.id = tv.person_ref
                WHERE p.fødselsnummer = :fodselsnummer
                AND tv.vedtaksperiode_forkastet = true
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer,
            ) { it.boolean(1) }
                ?: throw IllegalStateException("Finner ikke totrinns markert som forkastet for fødselsnummer=$fødselsnummer")
        assertTrue(totrinnsvurderingForkastet) {
            "Forventer at totrinnsvurdering er markert som forkastet"
        }
    }

    fun erFerdigstilt(godkjenningsbehovId: UUID) =
        dbQuery.single(
            "SELECT tilstand FROM command_context WHERE hendelse_id = :godkjenningsbehovId ORDER by id DESC LIMIT 1",
            "godkjenningsbehovId" to godkjenningsbehovId,
        ) { it.string("tilstand") } == "FERDIG"

    fun commandContextId(godkjenningsbehovId: UUID) =
        dbQuery.single(
            "SELECT context_id FROM command_context WHERE hendelse_id = :godkjenningsbehovId ORDER by id DESC LIMIT 1",
            "godkjenningsbehovId" to godkjenningsbehovId,
        ) { it.uuid("context_id") }

    protected fun person(
        fødselsnummer: String,
        aktørId: String,
    ) = dbQuery.single(
        "SELECT COUNT(*) FROM person WHERE fødselsnummer = :foedselsnummer AND aktør_id = :aktoerId",
        "foedselsnummer" to fødselsnummer,
        "aktoerId" to aktørId,
    ) { it.int(1) }

    private fun vedtak(vedtaksperiodeId: UUID) =
        dbQuery.single(
            "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ) { it.int(1) }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        dbQuery.update(
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unikId, :varselkode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent(),
            "unikId" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
            "varselkode" to varselkode,
            "tittel" to "En tittel for varselkode=$varselkode",
            "forklaring" to "En forklaring for varselkode=$varselkode",
            "handling" to "En handling for varselkode=$varselkode",
            "avviklet" to false,
            "opprettet" to LocalDateTime.now(),
        )
    }

    protected fun mockSnapshot(fødselsnummer: String = FØDSELSNUMMER) {
        every { spleisClient.hentPerson(fødselsnummer) } returns
            snapshot(
                versjon = 1,
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                aktørId = godkjenningsbehovTestdata.aktørId,
                organisasjonsnummer = godkjenningsbehovTestdata.organisasjonsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
                utbetalingId = godkjenningsbehovTestdata.utbetalingId,
                arbeidsgiverbeløp = 0,
                personbeløp = 0,
            )
    }

    protected enum class Kommandokjedetilstand {
        NY,
        SUSPENDERT,
        FERDIG,
        AVBRUTT,
    }
}
