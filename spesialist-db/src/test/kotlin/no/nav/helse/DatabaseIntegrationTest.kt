package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.db.DBSessionContext
import no.nav.helse.db.DbQuery
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.PgMeldingDuplikatkontrollDao
import no.nav.helse.db.PgPersonDao
import no.nav.helse.db.TestMelding
import no.nav.helse.db.api.ArbeidsgiverApiDao.Inntekter
import no.nav.helse.db.api.PgAbonnementApiDao
import no.nav.helse.db.api.PgArbeidsgiverApiDao
import no.nav.helse.db.api.PgNotatApiDao
import no.nav.helse.db.api.PgOppgaveApiDao
import no.nav.helse.db.api.PgOverstyringApiDao
import no.nav.helse.db.api.PgPeriodehistorikkApiDao
import no.nav.helse.db.api.PgPersonApiDao
import no.nav.helse.db.api.PgRisikovurderingApiDao
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import no.nav.helse.spesialist.typer.Kjønn
import no.nav.helse.util.januar
import org.junit.jupiter.api.AfterEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected val dbQuery = DbQuery(dataSource)
    private val testperson = TestPerson()
    protected open val HENDELSE_ID: UUID = UUID.randomUUID()

    protected val VEDTAKSPERIODE: UUID = testperson.vedtaksperiodeId1
    protected val ARBEIDSFORHOLD = Arbeidsforhold(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2), "EN TITTEL", 100)
    protected open val UTBETALING_ID: UUID = testperson.utbetalingId1

    protected open var OPPGAVE_ID = nextLong()
    protected val EGENSKAP = EgenskapForDatabase.SØKNAD
    protected val OPPGAVESTATUS = "AvventerSaksbehandler"

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
    protected val TOM: LocalDate = LocalDate.of(2018, 1, 31)
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

    protected companion object {
        internal val objectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }

    private var personId: Long = -1
    internal var vedtakId: Long = -1
        private set
    internal var oppgaveId: Long = -1
        private set

    protected val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val sessionContext = DBSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()

    internal val personDao = PgPersonDao(session)
    protected val personApiDao = PgPersonApiDao(dataSource)
    internal val oppgaveDao = repositories.oppgaveDao
    internal val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    internal val notatApiDao = PgNotatApiDao(dataSource)
    internal val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    internal val periodehistorikkDao = repositories.periodehistorikkDao
    internal val arbeidsforholdDao = sessionContext.arbeidsforholdDao
    internal val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    internal val vedtakDao = repositories.vedtakDao
    internal val commandContextDao = repositories.commandContextDao
    internal val tildelingDao = repositories.tildelingDao
    internal val saksbehandlerDao = repositories.saksbehandlerDao
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
    internal val behandlingsstatistikkDao = repositories.behandlingsstatistikkDao
    internal val vergemålDao = sessionContext.vergemålDao
    internal val totrinnsvurderingDao = sessionContext.totrinnsvurderingDao
    internal val dokumentDao = repositories.dokumentDao
    internal val påVentDao = sessionContext.påVentDao
    internal val stansAutomatiskBehandlingDao = sessionContext.stansAutomatiskBehandlingDao
    internal val dialogDao = repositories.dialogDao
    internal val annulleringRepository = repositories.annulleringRepository
    private val pgPersonRepository = sessionContext.personRepository
    private val inntektskilderRepository = sessionContext.inntektskilderRepository

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
        oppgaveEgenskaper: List<EgenskapForDatabase> = listOf(EGENSKAP),
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

    protected fun tildelOppgave(
        oppgaveId: Long = OPPGAVE_ID,
        saksbehandlerOid: UUID,
        navn: String = SAKSBEHANDLER_NAVN,
        egenskaper: List<EgenskapForDatabase> = listOf(EgenskapForDatabase.SØKNAD),
    ) {
        opprettSaksbehandler(saksbehandlerOid, navn = navn, epost = SAKSBEHANDLER_EPOST, ident = SAKSBEHANDLER_IDENT)
        oppgaveDao.updateOppgave(oppgaveId, oppgavestatus = "AvventerSaksbehandler", egenskaper = egenskaper)
        dbQuery.update(
            "INSERT INTO tildeling (saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgaveId)",
            "oid" to saksbehandlerOid,
            "oppgaveId" to oppgaveId
        )
    }

    protected fun leggOppgavePåVent(
        oppgaveId: Long = OPPGAVE_ID,
        saksbehandlerOid: UUID,
        frist: LocalDate = LocalDate.now().plusDays(1),
        årsaker: List<PåVentÅrsak> = emptyList(),
        tekst: String = "En notattekst",
    ) {
        val dialogRef = dialogDao.lagre()
        påVentDao.lagrePåVent(oppgaveId, saksbehandlerOid, frist, årsaker, tekst, dialogRef)
        notatApiDao.leggTilKommentar(dialogRef.toInt(), "En kommentar", SAKSBEHANDLER_IDENT)
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
    ) = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            "insert into person (fødselsnummer, aktør_id) values (:foedselsnummer, :aktoerId)",
            "foedselsnummer" to fødselsnummer,
            "aktoerId" to aktørId,
        )
    )

    protected fun opprettPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ): Persondata {
        val personinfoId =
            insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, adressebeskyttelse)
        val infotrygdutbetalingerId =
            personDao.upsertInfotrygdutbetalinger(fødselsnummer, objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = personDao.insertPerson(fødselsnummer, aktørId, personinfoId, enhetId, infotrygdutbetalingerId)
        egenAnsattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        return Persondata(
            personId = personId,
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId,
        )
    }

    protected fun oppdaterEnhet(
        personId: Long,
        enhetNr: Int,
    ) = dbQuery.update(
        "update person set enhet_ref = :enhetNr, enhet_ref_oppdatert = now() where id = :personId",
        "enhetNr" to enhetNr,
        "personId" to personId,
    )

    protected fun opprettEgenAnsatt(
        personId: Long,
        erEgenAnsatt: Boolean,
    ) = dbQuery.update(
        "insert into egen_ansatt values (:personId, :erEgenAnsatt, now())",
        "personId" to personId,
        "erEgenAnsatt" to erEgenAnsatt,
    )

    protected fun oppdaterPersoninfo(adressebeskyttelse: Adressebeskyttelse) {
        val personinfoId = opprettPersoninfo(adressebeskyttelse)
        oppdaterPersonpekere(FNR, personinfoId)
    }

    private fun opprettPersoninfo(adressebeskyttelse: Adressebeskyttelse) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
        VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato::date, :kjoenn::person_kjonn, :adressebeskyttelse)
        """.trimIndent(),
        "fornavn" to FORNAVN,
        "mellomnavn" to MELLOMNAVN,
        "etternavn" to ETTERNAVN,
        "foedselsdato" to LocalDate.of(1970, 1, 1),
        "kjoenn" to "Ukjent",
        "adressebeskyttelse" to adressebeskyttelse.name,
    )

    private fun oppdaterPersonpekere(
        fødselsnummer: String,
        personinfoId: Long? = null,
        infotrygdutbetalingerId: Long? = null,
    ) {
        dbQuery.update(
            """
            update person
            set info_ref=:personinfoId,
                infotrygdutbetalinger_ref=:infotrygdutbetalingerRef,
                personinfo_oppdatert = (
                    CASE 
                        when (:harPersoninfoId is not null) then now()
                    END
                ),
                infotrygdutbetalinger_oppdatert = (
                    CASE 
                        when (:harInfotrygdutbetalingerRef is not null) then now()
                    END
                )
            where fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "personinfoId" to personinfoId,
            "harPersoninfoId" to (personinfoId != null),
            "infotrygdutbetalingerRef" to infotrygdutbetalingerId,
            "harInfotrygdutbetalingerRef" to (infotrygdutbetalingerId != null),
            "foedselsnummer" to fødselsnummer,
        )
    }

    protected fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
        VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato, CAST(:kjoenn as person_kjonn), :adressebeskyttelse);
        """.trimIndent(),
        "fornavn" to fornavn,
        "mellomnavn" to mellomnavn,
        "etternavn" to etternavn,
        "foedselsdato" to fødselsdato,
        "kjoenn" to kjønn.name,
        "adressebeskyttelse" to adressebeskyttelse.name,
    ).let(::requireNotNull)

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
        personId: Long = this.personId,
        orgnummer: String = ORGNUMMER,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO arbeidsforhold (person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert)
        select :personId, id, :startdato, :sluttdato, :tittel, :prosent, :oppdatert
        from arbeidsgiver
        where organisasjonsnummer = :orgnummer
        """.trimIndent(),
        "personId" to personId,
        "orgnummer" to orgnummer,
        "startdato" to ARBEIDSFORHOLD.start,
        "sluttdato" to ARBEIDSFORHOLD.slutt,
        "tittel" to ARBEIDSFORHOLD.tittel,
        "prosent" to ARBEIDSFORHOLD.prosent,
        "oppdatert" to LocalDateTime.now(),
    )

    protected fun opprettInntekt(
        personId: Long,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = dbQuery.update(
        """
        insert into inntekt (person_ref, skjaeringstidspunkt, inntekter)
        values (:person_ref, :skjaeringstidspunkt, :inntekter::json)
        """.trimIndent(),
        "person_ref" to personId,
        "skjaeringstidspunkt" to skjæringstidspunkt,
        "inntekter" to objectMapper.writeValueAsString(inntekter),
    )

    protected fun opprettGenerasjon(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        spleisBehandlingId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
    ) {
        pgPersonRepository.brukPersonHvisFinnes(FNR) {
            this.nySpleisBehandling(
                SpleisBehandling(
                    ORGNUMMER,
                    vedtaksperiodeId,
                    spleisBehandlingId,
                    1.januar,
                    31.januar
                )
            )
            nyUtbetalingForVedtaksperiode(vedtaksperiodeId, utbetalingId)
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
        vedtakDao.finnVedtakId(vedtaksperiodeId)?.also {
            vedtakId = it
        }
        opprettVedtakstype(vedtaksperiodeId, periodetype, inntektskilde)
    }

    protected fun opprettOppgave(
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        egenskaper: List<EgenskapForDatabase> = listOf(EGENSKAP),
        kanAvvises: Boolean = true,
        utbetalingId: UUID = UTBETALING_ID,
        behandlingId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
    ) {
        val hendelse = testhendelse(hendelseId = godkjenningsbehovId)
        opprettCommandContext(hendelse, contextId)
        oppgaveId = nextLong()
        OPPGAVE_ID = oppgaveId
        oppgaveDao.opprettOppgave(
            id = oppgaveId,
            godkjenningsbehovId = godkjenningsbehovId,
            egenskaper = egenskaper,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            kanAvvises = kanAvvises,
        )
    }

    protected fun avventerSystem(
        oppgaveId: Long,
        ferdigstiltAv: String,
        ferdigstiltAvOid: UUID,
    ) {
        oppgaveDao.updateOppgave(
            oppgaveId = oppgaveId,
            oppgavestatus = "AvventerSystem",
            ferdigstiltAv = ferdigstiltAv,
            oid = ferdigstiltAvOid,
            egenskaper = listOf(EGENSKAP),
        )
    }

    protected fun ferdigstillOppgave(
        oppgaveId: Long,
        ferdigstiltAv: String? = null,
        ferdigstiltAvOid: UUID? = null,
    ) {
        oppgaveDao.updateOppgave(
            oppgaveId = oppgaveId,
            oppgavestatus = "Ferdigstilt",
            ferdigstiltAv = ferdigstiltAv,
            oid = ferdigstiltAvOid,
            egenskaper = listOf(EGENSKAP),
        )
    }

    protected fun opprettTotrinnsvurdering(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        saksbehandler: UUID? = null,
        erRetur: Boolean = false,
        ferdigstill: Boolean = false,
    ) {
        totrinnsvurderingDao.opprett(vedtaksperiodeId)

        if (saksbehandler != null) {
            settSaksbehandler(vedtaksperiodeId, saksbehandler)
        }
        if (erRetur) {
            totrinnsvurderingDao.settErRetur(vedtaksperiodeId)
        }
        if (ferdigstill) {
            totrinnsvurderingDao.ferdigstill(vedtaksperiodeId)
        }
    }

    protected fun opprettUtbetalingKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
    }

    protected fun utbetalingsopplegg(
        beløpTilArbeidsgiver: Int,
        beløpTilSykmeldt: Int,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
    ) {
        val arbeidsgiveroppdragId = lagArbeidsgiveroppdrag(fagsystemId())
        val personOppdragId = lagPersonoppdrag(fagsystemId())
        val utbetaling_idId =
            lagUtbetalingId(
                arbeidsgiveroppdragId,
                personOppdragId,
                UTBETALING_ID,
                arbeidsgiverbeløp = beløpTilArbeidsgiver,
                personbeløp = beløpTilSykmeldt,
                utbetalingtype = utbetalingtype,
            )
        utbetalingDao.nyUtbetalingStatus(utbetaling_idId, Utbetalingsstatus.UTBETALT, LocalDateTime.now(), "{}")
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)
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
    ): Long =
        utbetalingDao.opprettUtbetalingId(
            utbetalingId = utbetalingId,
            fødselsnummer = FNR,
            organisasjonsnummer = ORGNUMMER,
            type = utbetalingtype,
            opprettet = LocalDateTime.now(),
            arbeidsgiverFagsystemIdRef = arbeidsgiverOppdragId,
            personFagsystemIdRef = personOppdragId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )

    protected fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    protected fun settSaksbehandler(
        vedtaksperiodeId: UUID,
        saksbehandlerOid: UUID,
    ) = dbQuery.update(
        """
        UPDATE totrinnsvurdering
        SET saksbehandler = :saksbehandlerOid, oppdatert = now()
        WHERE vedtaksperiode_id = :vedtaksperiodeId AND utbetaling_id_ref IS null
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "saksbehandlerOid" to saksbehandlerOid,
    )

    protected data class Persondata(
        val personId: Long,
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

    protected data class Arbeidsforhold(
        val start: LocalDate,
        val slutt: LocalDate,
        val tittel: String,
        val prosent: Int,
    )

}
