package no.nav.helse.e2e

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.db.DBSessionContext
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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

    protected open val UTBETALING_ID: UUID = testperson.utbetalingId1

    protected open var OPPGAVE_ID = nextLong()

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
    protected val ENHET = "0301"

    private val FOM: LocalDate = LocalDate.of(2018, 1, 1)

    protected val TOM: LocalDate = LocalDate.of(2018, 1, 31)

    protected open val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected open val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    protected open val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    protected open val SAKSBEHANDLER_IDENT = "Z999999"

    protected val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

    protected companion object {
        val objectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }

    private var personId: Long = -1
    var vedtakId: Long = -1
        private set
    var oppgaveId: Long = -1
        private set

    protected val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val sessionContext = DBSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()

    val personDao = sessionContext.personDao
    val oppgaveDao = daos.oppgaveDao
    val periodehistorikkApiDao = daos.periodehistorikkApiDao
    val vedtakDao = daos.vedtakDao
    val commandContextDao = daos.commandContextDao
    val saksbehandlerDao = daos.saksbehandlerDao
    val reservasjonDao = sessionContext.reservasjonDao
    val meldingDao = daos.meldingDao
    val egenAnsattDao = sessionContext.egenAnsattDao
    val påVentDao = sessionContext.påVentDao
    val stansAutomatiskBehandlingDao = sessionContext.stansAutomatiskBehandlingDao
    val notatDao = daos.notatDao
    val dialogDao = daos.dialogDao
    val annulleringRepository = daos.annulleringRepository
    private val pgPersonRepository = sessionContext.personRepository
    private val inntektskilderRepository = sessionContext.inntektskilderRepository
    private val totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository

    fun testhendelse(
        hendelseId: UUID = HENDELSE_ID,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        type: String = "GODKJENNING",
        json: String = "{}",
    ) = TestMelding(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type, json)
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

    protected fun nyPerson(
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        organisasjonsnummer: String = ORGNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        utbetalingId: UUID = UTBETALING_ID,
        contextId: UUID = UUID.randomUUID(),
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
            godkjenningsbehovId = UUID.randomUUID(),
            behandlingId = spleisBehandlingId,
        )
    }

    protected fun opprettTotrinnsvurdering(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        saksbehandlerOid: UUID? = null,
        erRetur: Boolean = false,
        ferdigstill: Boolean = false,
        fødselsnummer: String = FNR,
    ) {
        val totrinnsvurdering = Totrinnsvurdering.ny(vedtaksperiodeId = vedtaksperiodeId, fødselsnummer = fødselsnummer)
        totrinnsvurderingRepository.lagre(totrinnsvurdering)
        saksbehandlerOid?.let {
            totrinnsvurdering.sendTilBeslutter(
                oppgaveId = OPPGAVE_ID,
                behandlendeSaksbehandler = SaksbehandlerOid(saksbehandlerOid)
            )
        }

        if (erRetur) totrinnsvurdering.sendIRetur(
            oppgaveId = OPPGAVE_ID,
            beslutter = SaksbehandlerOid(UUID.randomUUID())
        )

        if (ferdigstill) totrinnsvurdering.ferdigstill(UTBETALING_ID)

        totrinnsvurderingRepository.lagre(totrinnsvurdering)
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
        navn: String = "SAKSBEHANDLER SAKSBEHANDLERSEN",
        epost: String = "epost@nav.no",
        ident: String = "Z999999",
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

    private fun opprettOppgave(
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        egenskaper: Set<Egenskap> = setOf(Egenskap.SØKNAD),
        kanAvvises: Boolean = true,
        utbetalingId: UUID = UTBETALING_ID,
        behandlingId: UUID = UUID.randomUUID(),
        godkjenningsbehovId: UUID = UUID.randomUUID(),
    ) {
        val hendelse = testhendelse(hendelseId = godkjenningsbehovId)
        opprettCommandContext(hendelse, contextId)
        oppgaveId = nextLong()
        OPPGAVE_ID = oppgaveId
        sessionContext.oppgaveRepository.lagre(
            Oppgave.ny(
                id = oppgaveId,
                hendelseId = godkjenningsbehovId,
                egenskaper = egenskaper,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                kanAvvises = kanAvvises,
            )
        )
    }

    protected fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    protected data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        definisjonRef: Long? = null,
        status: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ) = dbQuery.update(
        """
        INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
        VALUES (:id, :kode, :vedtaksperiodeId, (SELECT id FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId LIMIT 1), :definisjonRef, :opprettet, :status, :ident, :tidspunkt)
        """.trimIndent(),
        "id" to id,
        "kode" to kode,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "definisjonRef" to definisjonRef,
        "opprettet" to LocalDateTime.now(),
        "status" to status,
        "ident" to if (endretTidspunkt != null) "EN_IDENT" else null,
        "tidspunkt" to endretTidspunkt,
    )

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, opprettet) 
        VALUES (:definisjonId, :kode, :tittel, null, null, :opprettet)    
        """.trimIndent(),
        "definisjonId" to definisjonId,
        "kode" to kode,
        "tittel" to tittel,
        "opprettet" to LocalDateTime.now(),
    ).let(::requireNotNull)

    protected fun assertGodkjenteVarsler(
        vedtaksperiodeId: UUID,
        forventetAntall: Int,
    ) {
        val antall = dbQuery.single(
            """
            SELECT COUNT(1) FROM selve_varsel sv
            WHERE sv.generasjon_ref = ( SELECT id FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId )
             AND status = 'GODKJENT'
            """.trimIndent(), "vedtaksperiodeId" to vedtaksperiodeId
        ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    protected fun assertAvvisteVarsler(
        vedtaksperiodeId: UUID,
        @Suppress("SameParameterValue") forventetAntall: Int,
    ) {
        val antall = dbQuery.single(
            """
            SELECT COUNT(1) FROM selve_varsel sv
            WHERE sv.generasjon_ref = ( SELECT id FROM behandling WHERE vedtaksperiode_id = :vedtaksperiodeId )
             AND status = 'AVVIST'
             """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId
        ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    protected data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )

}
