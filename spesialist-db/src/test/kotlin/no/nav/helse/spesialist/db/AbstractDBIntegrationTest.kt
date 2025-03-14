package no.nav.helse.spesialist.db

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.db.dao.PgMeldingDuplikatkontrollDao
import no.nav.helse.spesialist.db.dao.PgPersonDao
import no.nav.helse.spesialist.db.dao.api.PgAbonnementApiDao
import no.nav.helse.spesialist.db.dao.api.PgArbeidsgiverApiDao
import no.nav.helse.spesialist.db.dao.api.PgOppgaveApiDao
import no.nav.helse.spesialist.db.dao.api.PgOverstyringApiDao
import no.nav.helse.spesialist.db.dao.api.PgPeriodehistorikkApiDao
import no.nav.helse.spesialist.db.dao.api.PgPersonApiDao
import no.nav.helse.spesialist.db.dao.api.PgRisikovurderingApiDao
import no.nav.helse.spesialist.db.dao.api.PgVarselApiRepository
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import no.nav.helse.spesialist.testhjelp.jan
import no.nav.helse.spesialist.testhjelp.lagAktørId
import no.nav.helse.spesialist.testhjelp.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.testhjelp.lagEtternavn
import no.nav.helse.spesialist.testhjelp.lagFornavn
import no.nav.helse.spesialist.testhjelp.lagFødselsnummer
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import no.nav.helse.spesialist.testhjelp.lagSaksbehandlerident
import no.nav.helse.spesialist.testhjelp.lagSaksbehandlernavn
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

abstract class AbstractDBIntegrationTest {
    private val testperson = TestPerson()
    protected open val HENDELSE_ID: UUID = UUID.randomUUID()

    protected val VEDTAKSPERIODE: UUID = testperson.vedtaksperiodeId1
    protected val ARBEIDSFORHOLD =
        ArbeidsforholdForTest(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)
    protected open val UTBETALING_ID: UUID = testperson.utbetalingId1

    protected val ORGNUMMER =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnummer
        }
    protected val ORGNAVN =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnavn
        }

    protected val BRANSJER = listOf("EN BRANSJE")

    protected val FNR = testperson.fødselsnummer
    protected val AKTØR = testperson.aktørId
    protected val FORNAVN = testperson.fornavn
    protected val MELLOMNAVN = testperson.mellomnavn
    protected val ETTERNAVN = testperson.etternavn
    protected val FØDSELSDATO: LocalDate = LocalDate.EPOCH
    protected val KJØNN = testperson.kjønn
    protected val ADRESSEBESKYTTELSE = Adressebeskyttelse.Ugradert
    protected val ENHET = "0301"

    private val FOM: LocalDate = LocalDate.of(2018, 1, 1)
    private val TOM: LocalDate = LocalDate.of(2018, 1, 31)
    protected val PERIODE = Periode(VEDTAKSPERIODE, FOM, TOM)

    protected val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    protected val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    protected val SAKSBEHANDLER_IDENT = lagSaksbehandlerident()
    protected val SAKSBEHANDLER =
        Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            epost = SAKSBEHANDLER_EPOST,
        )

    protected val dataSource = DBDBTestFixture.fixture.module.dataSource
    protected val dbQuery = DbQuery(dataSource)
    protected val daos = DBDBTestFixture.fixture.module.daos

    protected val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val sessionContext = DBSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()

    internal val personDao = PgPersonDao(session)
    protected val personApiDao = PgPersonApiDao(dataSource)
    internal val oppgaveDao = daos.oppgaveDao
    internal val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    internal val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    internal val periodehistorikkDao = daos.periodehistorikkDao
    internal val arbeidsforholdDao = sessionContext.arbeidsforholdDao
    internal val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    internal val vedtakDao = daos.vedtakDao
    protected val apiVarselRepository = PgVarselApiRepository(dataSource)
    internal val commandContextDao = daos.commandContextDao
    internal val tildelingDao = daos.tildelingDao
    internal val saksbehandlerDao = daos.saksbehandlerDao
    internal val overstyringDao = sessionContext.overstyringDao
    internal val overstyringApiDao = PgOverstyringApiDao(dataSource)
    internal val reservasjonDao = sessionContext.reservasjonDao
    internal val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    internal val risikovurderingDao = sessionContext.risikovurderingDao
    internal val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    internal val automatiseringDao = sessionContext.automatiseringDao
    internal val åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao
    internal val egenAnsattDao = sessionContext.egenAnsattDao
    internal val abonnementDao = PgAbonnementApiDao(dataSource)
    internal val utbetalingDao = sessionContext.utbetalingDao
    internal val behandlingsstatistikkDao = daos.behandlingsstatistikkDao
    internal val vergemålDao = sessionContext.vergemålDao
    internal val dokumentDao = daos.dokumentDao
    internal val påVentDao = sessionContext.påVentDao
    internal val stansAutomatiskBehandlingDao = sessionContext.stansAutomatiskBehandlingDao
    internal val dialogDao = daos.dialogDao
    internal val annulleringRepository = daos.annulleringRepository
    private val pgPersonRepository = sessionContext.personRepository
    internal val inntektskilderRepository = sessionContext.inntektskilderRepository
    internal val overstyringRepository = sessionContext.overstyringRepository
    internal val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    internal fun testhendelse(
        hendelseId: UUID = HENDELSE_ID,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        type: String = "GODKJENNING",
        json: String = "{}",
    ) = TestMelding(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type, json)
    }

    protected fun godkjenningsbehov(
        hendelseId: UUID = HENDELSE_ID,
        fødselsnummer: String = FNR,
        json: String = "{}",
    ) {
        lagreHendelse(hendelseId, fødselsnummer, "GODKJENNING", json)
    }

    private fun lagreHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FNR,
        type: String,
        json: String = """{"fødselsnummer": "$fødselsnummer"}""",
    ) {
        dbQuery.update(
            "INSERT INTO hendelse (id, data, type) VALUES (:hendelseId, :json::json, :type)",
            "hendelseId" to hendelseId,
            "json" to json,
            "type" to type,
        )
    }

    protected fun nyttAutomatiseringsinnslag(automatisert: Boolean) {
        if (automatisert) {
            automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        } else {
            automatiseringDao.manuellSaksbehandling(listOf("Dårlig ånde"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        }
    }

    protected fun nyPerson(
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        organisasjonsnummer: String = ORGNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        utbetalingId: UUID = UTBETALING_ID,
        contextId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        oppgaveEgenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
    ) {
        opprettPerson(fødselsnummer = fødselsnummer, aktørId = aktørId)
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer)
        opprettVedtaksperiode(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
        )
        opprettOppgave(
            contextId = contextId,
            vedtaksperiodeId = vedtaksperiodeId,
            egenskaper = oppgaveEgenskaper,
            godkjenningsbehovId = godkjenningsbehovId,
            behandlingId = spleisBehandlingId,
        )
    }

    private fun opprettCommandContext(
        hendelse: TestMelding,
        contextId: UUID,
    ) {
        commandContextDao.opprett(hendelse.id, contextId)
    }

    private fun opprettVedtakstype(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        type: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
    ) {
        vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
    }

    protected fun opprettMinimalPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
    ) = personDao.lagreMinimalPerson(MinimalPersonDto(fødselsnummer, aktørId))

    protected fun opprettPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        fornavn: String = lagFornavn(),
        mellomnavn: String? = null,
        etternavn: String = lagEtternavn(),
    ): Persondata {
        personDao.lagreMinimalPerson(MinimalPersonDto(fødselsnummer, aktørId))
        val personinfoId = opprettPersoninfo(
            fødselsnummer = fødselsnummer,
            adressebeskyttelse = adressebeskyttelse,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn
        )
        val infotrygdutbetalingerId =
            personDao.upsertInfotrygdutbetalinger(fødselsnummer, objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personDao.oppdaterEnhet(fødselsnummer, enhetId)
        personDao.finnPersonMedFødselsnummer(fødselsnummer)!!
        egenAnsattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        return Persondata(
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId,
        )
    }

    protected fun oppdaterEnhet(
        fødselsnummer: String = FNR,
        enhetNr: Int,
    ) = personDao.oppdaterEnhet(fødselsnummer, enhetNr)

    protected fun opprettEgenAnsatt(
        fødselsnummer: String,
        @Suppress("SameParameterValue") erEgenAnsatt: Boolean,
    ) = egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, LocalDateTime.now())

    protected fun oppdaterAdressebeskyttelse(@Suppress("SameParameterValue") adressebeskyttelse: Adressebeskyttelse) {
        opprettPersoninfo(FNR, adressebeskyttelse = adressebeskyttelse)
    }

    protected fun opprettPersoninfo(
        fødselsnummer: String,
        fornavn: String = FORNAVN,
        mellomnavn: String? = MELLOMNAVN,
        etternavn: String = ETTERNAVN,
        fødselsdato: LocalDate = LocalDate.of(1970, 1, 1),
        kjønn: Kjønn = Kjønn.Ukjent,
        adressebeskyttelse: Adressebeskyttelse,
    ) = personDao.upsertPersoninfo(
        fødselsnummer = fødselsnummer,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kjønn = kjønn,
        adressebeskyttelse = adressebeskyttelse,
    ).let { personDao.finnPersoninfoRef(fødselsnummer) }!!

    protected fun opprettSaksbehandler(
        saksbehandlerOID: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: String = SAKSBEHANDLER.ident,
    ): UUID {
        saksbehandlerDao.opprettEllerOppdater(saksbehandlerOID, navn, epost, ident)
        return saksbehandlerOID
    }

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGNUMMER,
        navn: String = ORGNAVN,
        bransjer: List<String> = BRANSJER,
    ) {
        inntektskilderRepository.lagreInntektskilder(
            listOf(
                KomplettInntektskildeDto(
                    identifikator = organisasjonsnummer,
                    type = InntektskildetypeDto.ORDINÆR,
                    navn = navn,
                    bransjer = bransjer,
                    sistOppdatert = LocalDate.now(),
                ),
            ),
        )
    }

    protected fun opprettArbeidsforhold(
        fødselsnummer: String = FNR,
        orgnummer: String = ORGNUMMER,
    ) {
        val arbeidsforholdDto = KomplettArbeidsforholdDto(
            ARBEIDSFORHOLD.start,
            ARBEIDSFORHOLD.slutt,
            ARBEIDSFORHOLD.tittel,
            ARBEIDSFORHOLD.prosent,
            LocalDateTime.now(),
            fødselsnummer,
            orgnummer
        )
        arbeidsforholdDao.upsertArbeidsforhold(fødselsnummer, orgnummer, listOf(arbeidsforholdDto))
    }

    protected fun opprettBehandling(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        spleisBehandlingId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = 31 jan 2018,
        tags: List<String>? = emptyList(),
    ) {
        pgPersonRepository.brukPersonHvisFinnes(FNR) {
            this.nySpleisBehandling(
                SpleisBehandling(
                    ORGNUMMER,
                    vedtaksperiodeId,
                    spleisBehandlingId,
                    fom,
                    tom
                )
            )
            nyUtbetalingForVedtaksperiode(vedtaksperiodeId, utbetalingId)
            if (tags != null) {
                this.oppdaterPeriodeTilGodkjenning(vedtaksperiodeId, tags, spleisBehandlingId, utbetalingId)
            }
        }
    }

    // For å få satt riktig skjæringstidspunkt på behandlinger
    // Burde kanskje erstattes med bruk av DAO-er
    protected fun oppdaterBehandlingdata(vararg perioder: SpleisVedtaksperiode) {
        pgPersonRepository.brukPersonHvisFinnes(FNR) {
            mottaSpleisVedtaksperioder(perioder.toList())
        }
    }

    protected fun opprettVedtaksperiode(
        fødselsnummer: String = FNR,
        organisasjonsnummer: String = ORGNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        utbetalingId: UUID? = UTBETALING_ID,
        forkastet: Boolean = false,
        spleisBehandlingId: UUID = UUID.randomUUID(),
    ) {
        pgPersonRepository.brukPersonHvisFinnes(fødselsnummer) {
            this.nySpleisBehandling(
                SpleisBehandling(
                    organisasjonsnummer,
                    vedtaksperiodeId,
                    spleisBehandlingId,
                    fom,
                    tom
                )
            )
            if (utbetalingId != null) this.nyUtbetalingForVedtaksperiode(vedtaksperiodeId, utbetalingId)
            if (forkastet) this.vedtaksperiodeForkastet(vedtaksperiodeId)
        }
        vedtakDao.finnVedtakId(vedtaksperiodeId)
        opprettVedtakstype(vedtaksperiodeId, periodetype, inntektskilde)
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        insert into api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, opprettet) 
        values (:definisjonId, :kode, :tittel, null, null, :opprettet)
        """.trimIndent(),
        "definisjonId" to definisjonId,
        "kode" to kode,
        "tittel" to tittel,
        "opprettet" to LocalDateTime.now(),
    ).let(::checkNotNull)

    fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime? = LocalDateTime.now(),
        kode: String = "EN_KODE",
        spleisBehandlingId: UUID = UUID.randomUUID(),
        definisjonRef: Long? = null,
        status: String = "AKTIV",
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ) = dbQuery.update(
        """
        insert into selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
        select :id, :kode, :vedtaksperiodeId, id, :definisjonRef, :opprettet, :status, :ident, :endretTidspunkt
        from behandling
        where spleis_behandling_id = :spleisBehandlingId
        """.trimIndent(),
        "id" to id,
        "kode" to kode,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "spleisBehandlingId" to spleisBehandlingId,
        "definisjonRef" to definisjonRef,
        "opprettet" to opprettet,
        "status" to status,
        "ident" to if (endretTidspunkt != null) "EN_IDENT" else null,
        "endretTidspunkt" to endretTidspunkt,
    )

    protected fun opprettOppgave(
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
        kanAvvises: Boolean = true,
        utbetalingId: UUID = UTBETALING_ID,
        behandlingId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
    ): Oppgave {
        val hendelse = testhendelse(hendelseId = godkjenningsbehovId)
        opprettCommandContext(hendelse, contextId)
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = kanAvvises,
            egenskaper = egenskaper
        )
        sessionContext.oppgaveRepository.lagre(oppgave)
        return oppgave
    }

    fun finnOppgaveIdFor(vedtaksperiodeId: UUID): Long = oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)!!

    private fun opprettUtbetalingKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
    }

    protected fun utbetalingsopplegg(
        beløpTilArbeidsgiver: Int,
        beløpTilSykmeldt: Int,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        utbetalingId: UUID = UTBETALING_ID,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        organisasjonsnummer: String = ORGNUMMER,
    ) {
        val arbeidsgiveroppdragId = lagArbeidsgiveroppdrag(fagsystemId())
        val personOppdragId = lagPersonoppdrag(fagsystemId())
        val utbetaling_idId =
            lagUtbetalingId(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                arbeidsgiverOppdragId = arbeidsgiveroppdragId,
                personOppdragId = personOppdragId,
                utbetalingId = utbetalingId,
                arbeidsgiverbeløp = beløpTilArbeidsgiver,
                personbeløp = beløpTilSykmeldt,
                utbetalingtype = utbetalingtype,
            )
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, Utbetalingsstatus.UTBETALT, LocalDateTime.now(), "{}")
        opprettUtbetalingKobling(vedtaksperiodeId, utbetalingId)
    }

    protected fun lagArbeidsgiveroppdrag(
        fagsystemId: String = fagsystemId(),
        mottaker: String = ORGNUMMER,
    ) = utbetalingDao.nyttOppdrag(fagsystemId, mottaker)!!

    protected fun lagPersonoppdrag(fagsystemId: String = fagsystemId()) = utbetalingDao.nyttOppdrag(fagsystemId, FNR)!!

    protected fun lagUtbetalingId(
        arbeidsgiverOppdragId: Long,
        personOppdragId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        arbeidsgiverbeløp: Int = 2000,
        personbeløp: Int = 2000,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        fødselsnummer: String = FNR,
        organisasjonsnummer: String = ORGNUMMER,
    ): Long =
        utbetalingDao.opprettUtbetalingId(
            utbetalingId = utbetalingId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            type = utbetalingtype,
            opprettet = LocalDateTime.now(),
            arbeidsgiverFagsystemIdRef = arbeidsgiverOppdragId,
            personFagsystemIdRef = personOppdragId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )

    protected fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    protected fun nyOverstyrtTidslinjedag(): OverstyrtTidslinjedag =
        OverstyrtTidslinjedag(
            dato = 1 jan 2018,
            type = Dagtype.Sykedag.toString(),
            grad = 100,
            fraType = Dagtype.Feriedag.toString(),
            fraGrad = null,
            lovhjemmel = null,
        )

    protected fun nyOppgaveForNyPerson(
        fødselsnummer: String = lagFødselsnummer(),
        aktørId: String = lagAktørId(),
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        commandContextId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
        fornavn: String = lagFornavn(),
        mellomnavn: String? = null,
        etternavn: String = lagEtternavn(),
        oppgaveegenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
    ): Oppgave {
        opprettPerson(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            adressebeskyttelse = adressebeskyttelse,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
        )
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer)
        opprettVedtaksperiode(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = behandlingId,
            utbetalingId = utbetalingId,
        )
        utbetalingsopplegg(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            beløpTilSykmeldt = 1000,
            beløpTilArbeidsgiver = 1000
        )
        return opprettOppgave(
            contextId = commandContextId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            behandlingId = behandlingId,
            godkjenningsbehovId = godkjenningsbehovId,
            egenskaper = oppgaveegenskaper,
        )
    }

    protected fun Oppgave.tildelOgLagre(
        legacySaksbehandler: LegacySaksbehandler,
    ): Oppgave {
        opprettSaksbehandler(
            saksbehandlerOID = legacySaksbehandler.oid,
            navn = legacySaksbehandler.navn,
            epost = legacySaksbehandler.epostadresse,
            ident = legacySaksbehandler.ident()
        )
        this.forsøkTildeling(legacySaksbehandler)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.leggPåVentOgLagre(
        legacySaksbehandler: LegacySaksbehandler,
        frist: LocalDate = LocalDate.now().plusDays(1),
        årsaker: List<PåVentÅrsak> = emptyList(),
        tekst: String? = null,
    ): Oppgave {
        opprettSaksbehandler(
            saksbehandlerOID = legacySaksbehandler.oid,
            navn = legacySaksbehandler.navn,
            epost = legacySaksbehandler.epostadresse,
            ident = legacySaksbehandler.ident()
        )
        this.leggPåVent(true, legacySaksbehandler)
        val dialog = Dialog.Factory.ny().apply {
            leggTilKommentar(tekst = "En kommentar", saksbehandlerident = SAKSBEHANDLER_IDENT)
        }
        sessionContext.dialogRepository.lagre(dialog)
        sessionContext.oppgaveRepository.lagre(this)
        påVentDao.lagrePåVent(this.id, legacySaksbehandler.oid, frist, årsaker, tekst, dialog.id().value)
        return this
    }

    protected fun Oppgave.invaliderOgLagre(): Oppgave {
        this.avbryt()
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.sendTilBeslutterOgLagre(beslutter: LegacySaksbehandler?): Oppgave {
        this.sendTilBeslutter(beslutter)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.avventSystemOgLagre(legacySaksbehandler: LegacySaksbehandler): Oppgave {
        this.avventerSystem(legacySaksbehandler.ident(), legacySaksbehandler.oid)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.ferdigstillOgLagre(): Oppgave {
        this.ferdigstill()
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun nyTotrinnsvurdering(fødselsnummer: String, oppgave: Oppgave): TotrinnsvurderingKontekst {
        val totrinnsvurdering = Totrinnsvurdering.ny(oppgave.vedtaksperiodeId)
        sessionContext.totrinnsvurderingRepository.lagre(
            fødselsnummer = fødselsnummer,
            totrinnsvurdering = totrinnsvurdering,
        )
        return TotrinnsvurderingKontekst(totrinnsvurdering, fødselsnummer, oppgave)
    }

    protected fun TotrinnsvurderingKontekst.sendTilBeslutterOgLagre(legacySaksbehandler: LegacySaksbehandler): TotrinnsvurderingKontekst {
        totrinnsvurdering.sendTilBeslutter(oppgave.id, SaksbehandlerOid(legacySaksbehandler.oid))
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
        return this
    }

    protected fun TotrinnsvurderingKontekst.ferdigstillOgLagre(beslutter: LegacySaksbehandler): TotrinnsvurderingKontekst {
        totrinnsvurdering.settBeslutter(SaksbehandlerOid(beslutter.oid))
        totrinnsvurdering.ferdigstill(utbetalingId = oppgave.utbetalingId)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)
        return this
    }

    class TotrinnsvurderingKontekst(
        val totrinnsvurdering: Totrinnsvurdering,
        val fødselsnummer: String,
        val oppgave: Oppgave,
    )

    protected fun nyLegacySaksbehandler(navn: String = lagSaksbehandlernavn()): LegacySaksbehandler {
        val saksbehandler = LegacySaksbehandler(
            epostadresse = lagEpostadresseFraFulltNavn(navn),
            oid = UUID.randomUUID(),
            navn = navn,
            ident = lagSaksbehandlerident(),
            tilgangskontroll = { _, _ -> false }
        )
        opprettSaksbehandler(saksbehandler.oid, saksbehandler.navn, saksbehandler.epostadresse, saksbehandler.ident())
        return saksbehandler
    }

    protected data class Persondata(
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    protected data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    ) {

        companion object {
            infix fun LocalDate.til(tom: LocalDate) =
                Periode(UUID.randomUUID(), this, tom)
        }
    }

    protected data class Saksbehandler(
        val oid: UUID,
        val navn: String,
        val ident: String,
        val epost: String,
    )

    protected data class ArbeidsforholdForTest(
        val start: LocalDate,
        val slutt: LocalDate,
        val tittel: String,
        val prosent: Int,
    )

    protected companion object {
        internal val objectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }
}

object DBDBTestFixture {
    val fixture = DBTestFixture("db")
}
