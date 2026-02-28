package no.nav.helse.spesialist.db

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.db.dao.PgMeldingDuplikatkontrollDao
import no.nav.helse.spesialist.db.dao.PgPersonDao
import no.nav.helse.spesialist.db.dao.api.PgArbeidsgiverApiDao
import no.nav.helse.spesialist.db.dao.api.PgOppgaveApiDao
import no.nav.helse.spesialist.db.dao.api.PgPeriodehistorikkApiDao
import no.nav.helse.spesialist.db.dao.api.PgRisikovurderingApiDao
import no.nav.helse.spesialist.db.dao.api.PgVarselApiRepository
import no.nav.helse.spesialist.db.testfixtures.ModuleIsolatedDBTestFixture
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinjedag
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektsforhold
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektskilde
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnMottaker
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnOppgavetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnPeriodetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsdato
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

abstract class AbstractDBIntegrationTest {
    private val testperson = TestPerson()

    protected val ARBEIDSFORHOLD =
        ArbeidsforholdForTest(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)

    protected val ORGNUMMER =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnummer
        }

    protected val SAKSBEHANDLER = lagSaksbehandler()
    protected val SAKSBEHANDLER_OID: UUID = SAKSBEHANDLER.id.value
    protected val SAKSBEHANDLER_EPOST = SAKSBEHANDLER.epost
    protected val SAKSBEHANDLER_NAVN = SAKSBEHANDLER.navn
    protected val SAKSBEHANDLER_IDENT = SAKSBEHANDLER.ident

    protected val dataSource = DBDBTestFixture.fixture.module.dataSource
    protected val dbQuery = DataSourceDbQuery(dataSource)
    protected val daos = DBDBTestFixture.fixture.module.daos

    protected val session = sessionOf(dataSource, returnGeneratedKey = true)
    protected val sessionContext = DBSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()

    internal val personDao = PgPersonDao(session)
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
    internal val reservasjonDao = sessionContext.reservasjonDao
    internal val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    internal val risikovurderingDao = sessionContext.risikovurderingDao
    internal val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    internal val automatiseringDao = sessionContext.automatiseringDao
    internal val åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao
    internal val egenAnsattDao = sessionContext.egenAnsattDao
    internal val utbetalingDao = sessionContext.utbetalingDao
    internal val behandlingsstatistikkDao = daos.behandlingsstatistikkDao
    internal val vergemålDao = sessionContext.vergemålDao
    internal val dokumentDao = daos.dokumentDao
    internal val påVentDao = sessionContext.påVentDao
    internal val stansAutomatiskBehandlingDao = sessionContext.stansAutomatiskBehandlingDao
    internal val dialogDao = daos.dialogDao
    internal val annulleringRepository = daos.annulleringRepository
    internal val overstyringRepository = sessionContext.overstyringRepository
    internal val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository
    internal val stansAutomatiskBehandlingSaksbehandlerDao = sessionContext.stansAutomatiskBehandlingSaksbehandlerDao

    internal fun testhendelse(
        hendelseId: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        type: String = "GODKJENNING",
        json: String = "{}",
    ) = TestMelding(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type, json)
    }

    protected fun godkjenningsbehov(
        hendelseId: UUID,
        fødselsnummer: String,
        json: String = "{}",
    ) {
        lagreHendelse(hendelseId, fødselsnummer, "GODKJENNING", json)
    }

    private fun lagreHendelse(
        hendelseId: UUID,
        fødselsnummer: String,
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

    protected fun nyttAutomatiseringsinnslag(
        automatisert: Boolean,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
    ) {
        if (automatisert) {
            automatiseringDao.automatisert(vedtaksperiodeId, hendelseId, utbetalingId)
        } else {
            automatiseringDao.manuellSaksbehandling(listOf("Dårlig ånde"), vedtaksperiodeId, hendelseId, utbetalingId)
        }
    }

    private fun opprettVedtakstype(
        vedtaksperiodeId: UUID,
        type: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
    ) {
        vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
    }

    protected fun opprettPerson(person: Person = lagPerson()): Person {
        sessionContext.personRepository.lagre(person)
        return person
    }

    protected fun opprettSaksbehandler(
        saksbehandlerOID: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER.navn,
        epost: String = SAKSBEHANDLER.epost,
        ident: NAVIdent = SAKSBEHANDLER.ident,
    ): UUID {
        saksbehandlerDao.opprettEllerOppdater(saksbehandlerOID, navn, epost, ident.value)
        return saksbehandlerOID
    }

    protected fun opprettArbeidsgiver(
        identifikator: String = ORGNUMMER,
        navn: String = lagOrganisasjonsnavn(),
    ): Arbeidsgiver =
        Arbeidsgiver.Factory
            .ny(
                id = ArbeidsgiverIdentifikator.fraString(identifikator),
                navnString = navn,
            ).also(sessionContext.arbeidsgiverRepository::lagre)

    protected fun opprettArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        val arbeidsforholdDto =
            KomplettArbeidsforholdDto(
                ARBEIDSFORHOLD.start,
                ARBEIDSFORHOLD.slutt,
                ARBEIDSFORHOLD.tittel,
                ARBEIDSFORHOLD.prosent,
                LocalDateTime.now(),
                fødselsnummer,
                organisasjonsnummer,
            )
        arbeidsforholdDao.upsertArbeidsforhold(fødselsnummer, organisasjonsnummer, listOf(arbeidsforholdDto))
    }

    protected fun opprettVedtaksperiode(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        forkastet: Boolean = false,
    ): Vedtaksperiode {
        val organisasjonsnummer =
            when (val id = arbeidsgiver.id) {
                is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
                is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
            }
        val vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
        val vedtaksperiode =
            Vedtaksperiode(vedtaksperiodeId, person.id, organisasjonsnummer, forkastet)
        sessionContext.vedtaksperiodeRepository.lagre(vedtaksperiode)
        opprettVedtakstype(vedtaksperiodeId.value, periodetype, inntektskilde)
        return vedtaksperiode
    }

    protected fun opprettSaksbehandler(): Saksbehandler {
        val saksbehandler = lagSaksbehandler()
        sessionContext.saksbehandlerRepository.lagre(saksbehandler)
        return saksbehandler
    }

    protected fun opprettBehandling(
        vedtaksperiode: Vedtaksperiode,
        tags: List<String> = emptyList(),
        fom: LocalDate = LocalDate.now().minusDays(30),
        tom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = fom,
        utbetalingId: UtbetalingId? = UtbetalingId(UUID.randomUUID()),
        spleisBehandlingId: SpleisBehandlingId? = SpleisBehandlingId(UUID.randomUUID()),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    ): Behandling {
        val behandling =
            Behandling.fraLagring(
                id = BehandlingUnikId(UUID.randomUUID()),
                spleisBehandlingId = spleisBehandlingId,
                vedtaksperiodeId = vedtaksperiode.id,
                fom = fom,
                tom = tom,
                yrkesaktivitetstype = yrkesaktivitetstype,
                utbetalingId = utbetalingId,
                tags = tags.toSet(),
                tilstand = Behandling.Tilstand.VidereBehandlingAvklares,
                skjæringstidspunkt = skjæringstidspunkt,
            )
        sessionContext.behandlingRepository.lagre(behandling)
        return behandling
    }

    protected fun opprettVarsel(
        behandling: Behandling,
        kode: String,
    ): Varsel {
        val varsel =
            Varsel.nytt(
                id = VarselId(UUID.randomUUID()),
                behandlingUnikId = behandling.id,
                spleisBehandlingId = behandling.spleisBehandlingId,
                kode = kode,
                opprettetTidspunkt = LocalDateTime.now(),
            )
        sessionContext.varselRepository.lagre(varsel)
        return varsel
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ) = dbQuery
        .updateAndReturnGeneratedKey(
            """
            insert into api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, opprettet) 
            values (:definisjonId, :kode, :tittel, null, null, :opprettet)
            """.trimIndent(),
            "definisjonId" to definisjonId,
            "kode" to kode,
            "tittel" to tittel,
            "opprettet" to LocalDateTime.now(),
        ).let(::checkNotNull)

    protected fun opprettOppgave(
        førsteOpprettet: LocalDateTime? = null,
        vedtaksperiodeId: UUID,
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
        kanAvvises: Boolean = true,
        utbetalingId: UUID,
        behandlingId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
    ): Oppgave {
        val oppgave =
            Oppgave.ny(
                id = nextLong(),
                førsteOpprettet = førsteOpprettet,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                hendelseId = godkjenningsbehovId,
                kanAvvises = kanAvvises,
                egenskaper = egenskaper,
                mottaker = egenskaper.finnMottaker(),
                oppgavetype = egenskaper.finnOppgavetype(),
                inntektskilde = egenskaper.finnInntektskilde(),
                inntektsforhold = egenskaper.finnInntektsforhold(),
                periodetype = egenskaper.finnPeriodetype(),
            )
        sessionContext.oppgaveRepository.lagre(oppgave)
        return oppgave
    }

    protected fun opprettOppgave(
        vedtaksperiode: Vedtaksperiode,
        behandling: Behandling,
        førsteOpprettet: LocalDateTime? = null,
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
        kanAvvises: Boolean = true,
        godkjenningsbehovId: UUID = UUID.randomUUID(),
    ): Oppgave {
        val oppgave =
            Oppgave.ny(
                id = nextLong(),
                førsteOpprettet = førsteOpprettet,
                vedtaksperiodeId = vedtaksperiode.id.value,
                behandlingId = behandling.spleisBehandlingId!!.value,
                utbetalingId = behandling.utbetalingId!!.value,
                hendelseId = godkjenningsbehovId,
                kanAvvises = kanAvvises,
                egenskaper = egenskaper,
                mottaker = egenskaper.finnMottaker(),
                oppgavetype = egenskaper.finnOppgavetype(),
                inntektskilde = egenskaper.finnInntektskilde(),
                inntektsforhold = egenskaper.finnInntektsforhold(),
                periodetype = egenskaper.finnPeriodetype(),
            )
        sessionContext.oppgaveRepository.lagre(oppgave)
        return oppgave
    }

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
        utbetalingId: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        val arbeidsgiveroppdragId = lagArbeidsgiveroppdrag(fagsystemId())
        val personOppdragId = lagPersonoppdrag(fagsystemId(), fødselsnummer)
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

    protected fun lagPersonoppdrag(
        fagsystemId: String = fagsystemId(),
        fødselsnummer: String,
    ) = utbetalingDao.nyttOppdrag(fagsystemId, fødselsnummer)!!

    protected fun lagUtbetalingId(
        arbeidsgiverOppdragId: Long,
        personOppdragId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        arbeidsgiverbeløp: Int = 2000,
        personbeløp: Int = 2000,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        fødselsnummer: String,
        organisasjonsnummer: String = ORGNUMMER,
    ): Long =
        utbetalingDao.opprettUtbetalingId(
            utbetalingId = utbetalingId,
            fødselsnummer = fødselsnummer,
            arbeidsgiverIdentifikator = organisasjonsnummer,
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
        adressebeskyttelse: Personinfo.Adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert,
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        utbetalingId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
        fornavn: String = lagFornavn(),
        mellomnavn: String? = null,
        etternavn: String = lagEtternavn(),
        oppgaveegenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
    ): Oppgave {
        val person =
            opprettPerson(
                person =
                    lagPerson(
                        id = Identitetsnummer.fraString(fødselsnummer),
                        aktørId = aktørId,
                        adressebeskyttelse = adressebeskyttelse,
                        info =
                            Personinfo(
                                fornavn = fornavn,
                                mellomnavn = mellomnavn,
                                etternavn = etternavn,
                                fødselsdato = lagFødselsdato(),
                                kjønn = Personinfo.Kjønn.Ukjent,
                                adressebeskyttelse = adressebeskyttelse,
                            ),
                    ),
            )
        val arbeidsgiver = opprettArbeidsgiver(identifikator = organisasjonsnummer)
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling =
            opprettBehandling(
                vedtaksperiode,
                yrkesaktivitetstype = yrkesaktivitetstype,
                utbetalingId = UtbetalingId(utbetalingId),
            )
        utbetalingsopplegg(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiode.id.value,
            utbetalingId = utbetalingId,
            beløpTilSykmeldt = 1000,
            beløpTilArbeidsgiver = 1000,
        )
        return opprettOppgave(
            vedtaksperiodeId = vedtaksperiode.id.value,
            utbetalingId = utbetalingId,
            behandlingId = behandling.spleisBehandlingId!!.value,
            godkjenningsbehovId = godkjenningsbehovId,
            egenskaper = oppgaveegenskaper,
        )
    }

    protected fun Oppgave.tildelOgLagre(
        saksbehandlerWrapper: SaksbehandlerWrapper,
        brukerroller: Set<Brukerrolle> = emptySet(),
    ): Oppgave {
        opprettSaksbehandler(
            saksbehandlerOID = saksbehandlerWrapper.saksbehandler.id.value,
            navn = saksbehandlerWrapper.saksbehandler.navn,
            epost = saksbehandlerWrapper.saksbehandler.epost,
            ident = saksbehandlerWrapper.saksbehandler.ident,
        )
        this.forsøkTildeling(saksbehandlerWrapper, brukerroller)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.leggPåVentOgLagre(
        saksbehandlerWrapper: SaksbehandlerWrapper,
        frist: LocalDate = LocalDate.now().plusDays(1),
        årsaker: List<PåVentÅrsak> = emptyList(),
        tekst: String? = null,
    ): Oppgave {
        opprettSaksbehandler(
            saksbehandlerOID = saksbehandlerWrapper.saksbehandler.id.value,
            navn = saksbehandlerWrapper.saksbehandler.navn,
            epost = saksbehandlerWrapper.saksbehandler.epost,
            ident = saksbehandlerWrapper.saksbehandler.ident,
        )
        this.leggPåVent(true, saksbehandlerWrapper)
        val dialog =
            Dialog.Factory.ny().apply {
                leggTilKommentar(tekst = "En kommentar", saksbehandlerident = saksbehandlerWrapper.saksbehandler.ident)
            }
        sessionContext.dialogRepository.lagre(dialog)
        sessionContext.oppgaveRepository.lagre(this)
        påVentDao.lagrePåVent(
            this.id,
            saksbehandlerWrapper.saksbehandler.id.value,
            frist,
            årsaker,
            tekst,
            dialog.id().value,
        )
        return this
    }

    protected fun Oppgave.invaliderOgLagre(): Oppgave {
        this.avbryt()
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.sendTilBeslutterOgLagre(beslutter: SaksbehandlerWrapper?): Oppgave {
        this.sendTilBeslutter(beslutter)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.avventSystemOgLagre(saksbehandlerWrapper: SaksbehandlerWrapper): Oppgave {
        this.avventerSystem(saksbehandlerWrapper.saksbehandler.ident, saksbehandlerWrapper.saksbehandler.id.value)
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun Oppgave.ferdigstillOgLagre(): Oppgave {
        this.ferdigstill()
        sessionContext.oppgaveRepository.lagre(this)
        return this
    }

    protected fun opprettTotrinnsvurdering(person: Person): TotrinnsvurderingId {
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = person.id.value)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return totrinnsvurdering.id()
    }

    protected fun nyTotrinnsvurdering(
        fødselsnummer: String,
        oppgave: Oppgave,
    ): TotrinnsvurderingKontekst {
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer)
        sessionContext.totrinnsvurderingRepository.lagre(
            totrinnsvurdering = totrinnsvurdering,
        )
        return TotrinnsvurderingKontekst(totrinnsvurdering, fødselsnummer, oppgave)
    }

    protected fun TotrinnsvurderingKontekst.sendTilBeslutterOgLagre(saksbehandlerWrapper: SaksbehandlerWrapper): TotrinnsvurderingKontekst {
        totrinnsvurdering.sendTilBeslutter(oppgave.id, SaksbehandlerOid(saksbehandlerWrapper.saksbehandler.id.value))
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return this
    }

    protected fun TotrinnsvurderingKontekst.ferdigstillOgLagre(beslutter: SaksbehandlerWrapper): TotrinnsvurderingKontekst {
        totrinnsvurdering.settBeslutter(SaksbehandlerOid(beslutter.saksbehandler.id.value))
        totrinnsvurdering.ferdigstill()
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return this
    }

    class TotrinnsvurderingKontekst(
        val totrinnsvurdering: Totrinnsvurdering,
        val fødselsnummer: String,
        val oppgave: Oppgave,
    )

    protected fun nyLegacySaksbehandler(): SaksbehandlerWrapper {
        val saksbehandler = SaksbehandlerWrapper(lagSaksbehandler())
        opprettSaksbehandler(
            saksbehandler.saksbehandler.id.value,
            saksbehandler.saksbehandler.navn,
            saksbehandler.saksbehandler.epost,
            saksbehandler.saksbehandler.ident,
        )
        return saksbehandler
    }

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
    val fixture = ModuleIsolatedDBTestFixture("db")
}
