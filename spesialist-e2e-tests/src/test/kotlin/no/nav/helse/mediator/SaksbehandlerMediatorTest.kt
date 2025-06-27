package no.nav.helse.mediator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotliquery.sessionOf
import no.nav.helse.MeldingPubliserer
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlinghåndtererImpl
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_SAKSBEHANDLER
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.feilhåndtering.AlleredeAnnullert
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveTildeltNoenAndre
import no.nav.helse.spesialist.api.graphql.mutation.VedtakMutationHandler.VedtakResultat
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData.ApiAnnulleringArsak
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiLovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsforhold
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiOverstyringDag
import no.nav.helse.spesialist.api.graphql.schema.ApiPaVentRequest
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.ApiOpphevStans
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AvmeldOppgave
import no.nav.helse.spesialist.api.saksbehandler.handlinger.TildelOppgave
import no.nav.helse.spesialist.db.DBDaos
import no.nav.helse.spesialist.db.DBSessionContext
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlernavn
import no.nav.helse.spesialist.domain.testfixtures.lagTilfeldigSaksbehandlerepost
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import no.nav.helse.util.testEnv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import kotlin.random.Random.Default.nextLong

class SaksbehandlerMediatorTest : AbstractDatabaseTest() {
    private val dbQuery = DbQuery(dataSource)
    private val testperson = TestPerson()
    private val HENDELSE_ID: UUID = UUID.randomUUID()

    private val VEDTAKSPERIODE: UUID = testperson.vedtaksperiodeId1

    private val UTBETALING_ID: UUID = testperson.utbetalingId1

    private var OPPGAVE_ID = nextLong()

    private val ORGNUMMER =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnummer
        }
    private val ORGNAVN =
        with(testperson) {
            1.arbeidsgiver.organisasjonsnavn
        }

    private val FNR = testperson.fødselsnummer
    private val AKTØR = testperson.aktørId
    private val FORNAVN = testperson.fornavn
    private val MELLOMNAVN = testperson.mellomnavn
    private val ETTERNAVN = testperson.etternavn
    private val FØDSELSDATO: LocalDate = LocalDate.EPOCH
    private val KJØNN = testperson.kjønn
    private val ENHET = "0301"

    private val FOM: LocalDate = LocalDate.of(2018, 1, 1)

    private val TOM: LocalDate = LocalDate.of(2018, 1, 31)

    private val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    private val SAKSBEHANDLER_NAVN = "ET_NAVN"
    private val SAKSBEHANDLER_IDENT = "EN_IDENT"
    private val SAKSBEHANDLER_EPOST = "epost@nav.no"

    private val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

    private companion object {
        val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }

    private var personId: Long = -1
    var vedtakId: Long = -1
        private set
    var oppgaveId: Long = -1
        private set

    private val session = sessionOf(dataSource, returnGeneratedKey = true)
    private val sessionContext = DBSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()

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

    private fun nyPerson(
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

    private fun opprettTotrinnsvurdering(
        saksbehandlerOid: UUID? = null,
        erRetur: Boolean = false,
        ferdigstill: Boolean = false,
        fødselsnummer: String = FNR,
    ) {
        val totrinnsvurdering = Totrinnsvurdering.ny(fødselsnummer = fødselsnummer)
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
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

        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
    }

    private fun opprettCommandContext(
        hendelse: TestMelding,
        contextId: UUID,
    ) {
        daos.commandContextDao.opprett(hendelse.id, contextId)
    }

    private fun opprettVedtakstype(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        type: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
    ) {
        daos.vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
    }

    private fun opprettPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    ): Persondata {
        val personinfoId =
            insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, adressebeskyttelse)
        val infotrygdutbetalingerId =
            sessionContext.personDao.upsertInfotrygdutbetalinger(fødselsnummer, objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = sessionContext.personDao.insertPerson(
            fødselsnummer,
            aktørId,
            personinfoId,
            enhetId,
            infotrygdutbetalingerId
        )
        sessionContext.egenAnsattDao.lagre(fødselsnummer, false, LocalDateTime.now())
        return Persondata(
            personId = personId,
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId,
        )
    }

    private fun insertPersoninfo(
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

    private fun opprettSaksbehandler(
        saksbehandlerOID: UUID = SAKSBEHANDLER_OID,
        navn: String = "SAKSBEHANDLER SAKSBEHANDLERSEN",
        epost: String = "epost@nav.no",
        ident: String = "Z999999",
    ): UUID {
        daos.saksbehandlerDao.opprettEllerOppdater(saksbehandlerOID, navn, epost, ident)
        return saksbehandlerOID
    }

    private fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGNUMMER,
        navn: String = ORGNAVN,
    ) {
        sessionContext.arbeidsgiverRepository.lagre(
            Arbeidsgiver.Factory.ny(
                id = ArbeidsgiverIdentifikator.fraString(organisasjonsnummer),
                navnString = navn
            )
        )
    }

    private fun opprettVedtaksperiode(
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
        sessionContext.personRepository.brukPersonHvisFinnes(fødselsnummer) {
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
        daos.vedtakDao.finnVedtakId(vedtaksperiodeId)?.also {
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

    private data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    private fun nyttVarsel(
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

    private fun opprettVarseldefinisjon(
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

    private data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )


    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val testRapid = TestRapid()
    private val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(testRapid)
    private val stansAutomatiskBehandlinghåndterer =
        StansAutomatiskBehandlinghåndtererImpl(
            sessionContext.stansAutomatiskBehandlingDao,
            daos.oppgaveDao,
            daos.notatDao,
            daos.dialogDao,
        )
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = daos.oppgaveDao,
            reservasjonDao = sessionContext.reservasjonDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = { _, _ -> false },
            tilgangsgrupper = tilgangsgrupper,
            oppgaveRepository = daos.oppgaveRepository,
        )
    private val apiOppgaveService = ApiOppgaveService(
        oppgaveDao = daos.oppgaveDao,
        tilgangsgrupper = tilgangsgrupper,
        oppgaveService = oppgaveService,
        oppgaveApiDao = daos.oppgaveApiDao,
    )

    private val mediator =
        SaksbehandlerMediator(
            daos = DBDaos(dataSource),
            versjonAvKode = "versjonAvKode",
            meldingPubliserer = meldingPubliserer,
            oppgaveService = oppgaveService,
            apiOppgaveService = apiOppgaveService,
            tilgangsgrupper = tilgangsgrupper,
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
            annulleringRepository = daos.annulleringRepository,
            environmentToggles = environmentToggles,
            sessionFactory = TransactionalSessionFactory(dataSource),
            tilgangskontroll = { _, _ -> false },
        )

    private val AKTØR_ID = lagAktørId()
    private val FØDSELSNUMMER = lagFødselsnummer()
    private val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()
    private val ORGANISASJONSNUMMER_GHOST = lagOrganisasjonsnummer()


    private val saksbehandler = saksbehandler()

    private fun saksbehandler(
        oid: UUID = SAKSBEHANDLER_OID,
        navn: String = SAKSBEHANDLER_NAVN,
        epost: String = SAKSBEHANDLER_EPOST,
        ident: String = SAKSBEHANDLER_IDENT,
        grupper: List<UUID> = emptyList(),
    ): SaksbehandlerFraApi = SaksbehandlerFraApi(oid, navn, epost, ident, grupper)

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @ParameterizedTest
    @CsvSource("Innvilget,INNVILGELSE", "DelvisInnvilget,DELVIS_INNVILGELSE", "Avslag,AVSLAG")
    fun `håndter totrinnsvurdering med utfall innvilgelse basert på tags fra Spleis`(
        tag: String,
        utfall: VedtakBegrunnelseTypeFraDatabase
    ) {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )
        opprettTotrinnsvurdering(fødselsnummer = fødselsnummer)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf(tag),
                    utbetalingId = utbetalingId,
                )
            }
        }

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertEquals(AVVENTER_BESLUTTER, totrinnsvurdering.tilstand)
        assertVedtakBegrunnelse(expectedUtfall = utfall, expectedBegrunnelse = "Begrunnelse")
    }

    @Test
    fun `håndter totrinnsvurdering når periode har vurdert varsel`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )
        opprettTotrinnsvurdering(fødselsnummer = fødselsnummer)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf("Innvilget"),
                    utbetalingId = utbetalingId,
                )
            }
        }
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertEquals(AVVENTER_BESLUTTER, totrinnsvurdering.tilstand)
    }

    @Test
    fun `ny overstyring uten eksisterende totrinnsvurdering lager totrinnsvurdering`() {
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })
        opprettSaksbehandler()
        opprettVedtaksperiode()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = VEDTAKSPERIODE,
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finnAktivForPerson(person.fødselsnummer)
        }
        checkNotNull(totrinnsvurdering)
        assertTrue(totrinnsvurdering.overstyringer.single().opprettet.isAfter(LocalDateTime.now().minusSeconds(5)))
        assertEquals(AVVENTER_SAKSBEHANDLER, totrinnsvurdering.tilstand)
    }

    @Test
    fun `ny overstyring med eksisterende totrinnsvurdering legges på eksisterende totrinnsvurdering med opprinnelig saksbehandler`() {
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })
        opprettSaksbehandler()
        opprettVedtaksperiode()
        val saksbehandler2Oid = UUID.randomUUID()
        opprettSaksbehandler(saksbehandler2Oid)
        opprettTotrinnsvurdering(
            saksbehandlerOid = saksbehandler2Oid,
            fødselsnummer = person.fødselsnummer
        )
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = VEDTAKSPERIODE,
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finnAktivForPerson(person.fødselsnummer)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler2Oid, totrinnsvurdering.saksbehandler?.value)
        assertTrue(totrinnsvurdering.overstyringer.single().opprettet.isAfter(LocalDateTime.now().minusSeconds(5)))
        assertEquals(AVVENTER_BESLUTTER, totrinnsvurdering.tilstand)
    }

    @Test
    fun `håndter totrinnsvurdering send i retur`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )
        opprettTotrinnsvurdering(fødselsnummer = fødselsnummer)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf("Innvilget"),
                    utbetalingId = utbetalingId,
                )
            }
        }
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "VURDERT",
        )

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)

        val beslutter = SaksbehandlerFraApi(
            UUID.randomUUID(),
            lagSaksbehandlernavn(),
            lagTilfeldigSaksbehandlerepost(),
            lagSaksbehandlerident(),
            emptyList()
        )
        opprettSaksbehandler(beslutter.oid, beslutter.navn, beslutter.epost, beslutter.ident)
        val resultRetur = mediator.sendIRetur(oppgaveId, beslutter, "begrunnelse")

        assertEquals(SendIReturResult.Ok, resultRetur)

        val totrinnsvurdering = sessionFactory.transactionalSessionScope { session ->
            session.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        }
        checkNotNull(totrinnsvurdering)
        assertEquals(saksbehandler.oid, totrinnsvurdering.saksbehandler?.value)
        assertEquals(beslutter.oid, totrinnsvurdering.beslutter?.value)
        assertEquals(AVVENTER_SAKSBEHANDLER, totrinnsvurdering.tilstand)
    }

    @Test
    fun `håndter totrinnsvurdering når periode har aktivt varsel`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )
        opprettTotrinnsvurdering(fødselsnummer = fødselsnummer)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        val definisjonRef = opprettVarseldefinisjon()
        nyttVarsel(
            vedtaksperiodeId = vedtaksperiodeId,
            definisjonRef = definisjonRef,
            status = "AKTIV",
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf("Innvilget"),
                    utbetalingId = utbetalingId,
                )
            }
        }

        assertTrue(
            mediator.håndterTotrinnsvurdering(
                oppgaveId,
                saksbehandler,
                "Begrunnelse"
            ) is SendTilGodkjenningResult.Feil.ManglerVurderingAvVarsler
        )
    }

    @Test
    fun `håndter totrinnsvurdering når periode ikke har noen varsler`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )

        opprettTotrinnsvurdering(fødselsnummer = fødselsnummer)
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf("Innvilget"),
                    utbetalingId = utbetalingId,
                )
            }
        }

        val result =
            mediator.håndterTotrinnsvurdering(oppgaveId, saksbehandler, "Begrunnelse")

        assertEquals(SendTilGodkjenningResult.Ok, result)
    }

    @Test
    fun `forsøk tildeling av oppgave`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser().last()
        assertEquals("oppgave_oppdatert", melding)
    }

    @Test
    fun `legg på vent forårsaker publisering av hendelse`() {
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(spleisBehandlingId = spleisBehandlingId)
        val oppgaveId = OPPGAVE_ID
        val frist = LocalDate.now()
        val skalTildeles = true
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                frist,
                skalTildeles,
                "en tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )
        val melding = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        val årsaker = melding?.get("årsaker")?.map { it.get("key").asText() to it.get("årsak").asText() }
        assertNotNull(melding)
        assertEquals("lagt_på_vent", melding?.get("@event_name")?.asText())
        assertEquals("en tekst", melding?.get("notatTekst")?.asText())
        assertEquals(listOf("key" to "arsak"), årsaker)
        assertEquals(spleisBehandlingId, melding?.get("behandlingId")?.asUUID())
        assertEquals(oppgaveId, melding?.get("oppgaveId")?.asLong())
        assertEquals(saksbehandler.oid, melding?.get("saksbehandlerOid")?.asUUID())
        assertEquals(saksbehandler.ident, melding?.get("saksbehandlerIdent")?.asText())
        assertEquals(frist, melding?.get("frist")?.asLocalDate())
        assertEquals(skalTildeles, melding?.get("skalTildeles")?.asBoolean())
    }

    @Test
    fun `endring av påVent forårsaker publisering av hendelse`() {
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(spleisBehandlingId = spleisBehandlingId)
        val oppgaveId = OPPGAVE_ID
        val frist = LocalDate.now()
        val skalTildeles = true
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                frist,
                skalTildeles,
                "en tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )
        val melding1 = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        assertEquals("lagt_på_vent", melding1?.get("@event_name")?.asText())

        val nyFrist = LocalDate.now().plusDays(5)
        mediator.påVent(
            ApiPaVentRequest.ApiEndrePaVent(
                oppgaveId,
                saksbehandler.oid,
                nyFrist,
                skalTildeles,
                "en ny tekst",
                listOf(ApiPaVentRequest.ApiPaVentArsak("key", "arsak"))
            ), saksbehandler
        )


        val melding2 = testRapid.inspektør.hendelser("lagt_på_vent").lastOrNull()
        val årsaker = melding2?.get("årsaker")?.map { it.get("key").asText() to it.get("årsak").asText() }
        assertNotNull(melding2)
        assertEquals("lagt_på_vent", melding2?.get("@event_name")?.asText())
        assertEquals("en ny tekst", melding2?.get("notatTekst")?.asText())
        assertEquals(listOf("key" to "arsak"), årsaker)
        assertEquals(spleisBehandlingId, melding2?.get("behandlingId")?.asUUID())
        assertEquals(oppgaveId, melding2?.get("oppgaveId")?.asLong())
        assertEquals(saksbehandler.oid, melding2?.get("saksbehandlerOid")?.asUUID())
        assertEquals(saksbehandler.ident, melding2?.get("saksbehandlerIdent")?.asText())
        assertEquals(nyFrist, melding2?.get("frist")?.asLocalDate())
        assertEquals(skalTildeles, melding2?.get("skalTildeles")?.asBoolean())
    }

    @Test
    fun `forsøk tildeling av oppgave når den allerede er tildelt`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        testRapid.reset()
        assertThrows<OppgaveTildeltNoenAndre> {
            mediator.håndter(TildelOppgave(oppgaveId), saksbehandler(UUID.randomUUID()))
        }
        assertEquals(0, testRapid.inspektør.hendelser().size)
    }

    @Test
    fun `forsøk avmelding av oppgave`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.håndter(TildelOppgave(oppgaveId), saksbehandler)
        testRapid.reset()
        mediator.håndter(AvmeldOppgave(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser().last()
        assertEquals("oppgave_oppdatert", melding)
    }

    @Test
    fun `forsøk avmelding av oppgave når den ikke er tildelt`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        assertThrows<OppgaveIkkeTildelt> {
            mediator.håndter(AvmeldOppgave(oppgaveId), saksbehandler(UUID.randomUUID()))
        }
        assertEquals(0, testRapid.inspektør.hendelser().size)
    }

    @Test
    fun `legg på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                true,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = daos.periodehistorikkApiDao.finn(UTBETALING_ID)
        assertEquals(PeriodehistorikkType.LEGG_PA_VENT, historikk.first().type)
        assertTrue(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `endre på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(10),
                true,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = daos.periodehistorikkApiDao.finn(UTBETALING_ID)
        assertEquals(PeriodehistorikkType.LEGG_PA_VENT, historikk.first().type)
        assertTrue(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))

        mediator.påVent(
            ApiPaVentRequest.ApiEndrePaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(20),
                true,
                "ny notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                ),
            ),
            saksbehandler,
        )
        val melding2 = testRapid.inspektør.hendelser("lagt_på_vent").last()
        val historikk2 = daos.periodehistorikkApiDao.finn(UTBETALING_ID).sortedBy { it.id }
        assertEquals(PeriodehistorikkType.ENDRE_PA_VENT, historikk2.last().type)
        assertEquals("ny notat tekst", melding2["notatTekst"].asText())
    }

    @Test
    fun `fjern på vent`() {
        nyPerson()
        val oppgaveId = OPPGAVE_ID
        mediator.påVent(
            ApiPaVentRequest.ApiLeggPaVent(
                oppgaveId,
                saksbehandler.oid,
                LocalDate.now().plusDays(21),
                false,
                "notat tekst",
                listOf(
                    ApiPaVentRequest.ApiPaVentArsak("key", "arsak"),
                    ApiPaVentRequest.ApiPaVentArsak("key2", "arsak2"),
                ),
            ),
            saksbehandler,
        )
        mediator.påVent(ApiPaVentRequest.ApiFjernPaVent(oppgaveId), saksbehandler)
        val melding = testRapid.inspektør.hendelser("oppgave_oppdatert").last()
        val historikk = daos.periodehistorikkApiDao.finn(UTBETALING_ID)
        assertTrue(historikk.map { it.type }
            .containsAll(listOf(PeriodehistorikkType.FJERN_FRA_PA_VENT, PeriodehistorikkType.LEGG_PA_VENT)))
        assertFalse(melding["egenskaper"].map { it.asText() }.contains("PÅ_VENT"))
    }

    @Test
    fun `håndterer annullering`() {
        mediator.håndter(annullering(), saksbehandler)

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER_OID.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER_NAVN, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, melding["saksbehandler"]["ident"].asText())

        assertEquals(VEDTAKSPERIODE, melding["vedtaksperiodeId"].asUUID())
        assertEquals(UTBETALING_ID, melding["utbetalingId"].asUUID())
        assertEquals("EN_KOMMENTAR", melding["kommentar"]?.asText())
        assertEquals(2, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals(listOf("Ferie", "Perm"), melding["begrunnelser"].map { it.asText() })
        melding["arsaker"].let {
            assertEquals(2, it.size())
            it.first().let { node ->
                assertEquals("Ferie", node["arsak"].asText())
                assertEquals("key01", node["key"].asText())
            }
        }
    }

    @Test
    fun `håndterer annullering uten kommentar, begrunnelser eller årsak`() {
        mediator.håndter(annullering(kommentar = null, arsaker = emptyList()), saksbehandler)

        val melding = testRapid.inspektør.message(0)

        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(SAKSBEHANDLER_OID.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(SAKSBEHANDLER_NAVN, melding["saksbehandler"]["navn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, melding["saksbehandler"]["ident"].asText())

        assertEquals(VEDTAKSPERIODE, melding["vedtaksperiodeId"].asUUID())
        assertEquals(UTBETALING_ID, melding["utbetalingId"].asUUID())
        assertEquals(null, melding["kommentar"]?.asText())
        assertEquals(0, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals("", melding["arsaker"].asText())
    }

    @Test
    fun `godtar ikke å annullere samme utbetaling mer enn 1 gang`() {
        val annullering = annullering(kommentar = null)
        mediator.håndter(annullering, saksbehandler)
        assertThrows<AlleredeAnnullert> {
            mediator.håndter(annullering, saksbehandler)
        }
    }

    @ParameterizedTest
    @CsvSource("Innvilget,INNVILGELSE", "DelvisInnvilget,DELVIS_INNVILGELSE", "Avslag,AVSLAG")
    fun `fatter vedtak med utfall innvilgelse basert på tags fra Spleis`(
        tag: String,
        utfall: VedtakBegrunnelseTypeFraDatabase
    ) {
        val vedtaksperiodeId = UUID.randomUUID()
        val fødselsnummer = lagFødselsnummer()
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId
        )
        opprettSaksbehandler()
        val saksbehandler = SaksbehandlerFraApi(
            SAKSBEHANDLER_OID,
            SAKSBEHANDLER_NAVN,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_IDENT,
            emptyList()
        )
        sessionFactory.transactionalSessionScope { session ->
            session.personRepository.brukPersonHvisFinnes(fødselsnummer = fødselsnummer) {
                oppdaterPeriodeTilGodkjenning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    spleisBehandlingId = spleisBehandlingId,
                    tags = listOf(tag),
                    utbetalingId = utbetalingId,
                )
            }
        }

        val result = mediator.vedtak(
            saksbehandlerFraApi = saksbehandler,
            oppgavereferanse = oppgaveId,
            begrunnelse = "En begrunnelse",
        )

        assertEquals(VedtakResultat.Ok(spleisBehandlingId = spleisBehandlingId), result)
        assertVedtakBegrunnelse(expectedUtfall = utfall, expectedBegrunnelse = "En begrunnelse")
    }

    private fun assertVedtakBegrunnelse(expectedUtfall: VedtakBegrunnelseTypeFraDatabase, expectedBegrunnelse: String) {
        val vedtakBegrunnelse = daos.vedtakBegrunnelseDao.finnVedtakBegrunnelse(oppgaveId = oppgaveId)
        checkNotNull(vedtakBegrunnelse)
        assertEquals(expectedUtfall, vedtakBegrunnelse.type)
        assertEquals(expectedBegrunnelse, vedtakBegrunnelse.tekst)
    }

    // Eksperimentering med DSL for å lage testdata
    // Slett hvis du oppdager denne koden og den ikke er tatt i bruk andre steder 😂
    // Plassert her pga. ren og skjær tilfeldighet
    private data class PERSON(
        val fødselsnummer: String,
        val aktørId: String,
        val arbeidsgiver: List<ARBEIDSGIVER>,
    ) {
        val Int.ag: String
            get() = arbeidsgiver[this - 1].orgnr

        operator fun <T> invoke(func: PERSON.() -> T) = func()
    }

    private data class PERSONBUILDER(
        var fødselsnummer: String,
        var aktørId: String,
        var arbeidsgivere: List<ARBEIDSGIVER>,
    ) {
        fun build() = PERSON(fødselsnummer, aktørId, arbeidsgivere)
    }

    private fun person(init: PERSONBUILDER.() -> Unit): PERSON {
        val builder = PERSONBUILDER(lagFødselsnummer(), lagAktørId(), arbeidsgivere(1))
        builder.init()
        return builder.build()
    }

    private data class ARBEIDSGIVERBUILDER(
        var orgnrs: List<String>,
    ) {
        fun build() = orgnrs.map(SaksbehandlerMediatorTest::ARBEIDSGIVER)
    }

    private data class ARBEIDSGIVER(
        val orgnr: String,
    )

    private fun arbeidsgivere(
        antall: Int,
        init: ARBEIDSGIVERBUILDER.() -> Unit = {},
    ): List<ARBEIDSGIVER> {
        val builder = ARBEIDSGIVERBUILDER(List(antall) { lagOrganisasjonsnummer() }.toList())
        builder.init()
        return builder.build()
    }

    @Test
    fun `håndterer overstyring av tidslinje`() {
        val person =
            person {
                arbeidsgivere = arbeidsgivere(2)
            }
        nyPerson(fødselsnummer = person.fødselsnummer, aktørId = person.aktørId, organisasjonsnummer = person { 2.ag })

        val overstyring =
            ApiTidslinjeOverstyring(
                vedtaksperiodeId = UUID.randomUUID(),
                organisasjonsnummer = person { 2.ag },
                fodselsnummer = person.fødselsnummer,
                aktorId = person.aktørId,
                begrunnelse = "En begrunnelse",
                dager =
                    listOf(
                        ApiOverstyringDag(
                            dato = 10.januar,
                            type = "Sykedag",
                            fraType = "Arbeidsdag",
                            grad = null,
                            fraGrad = 100,
                            null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("overstyr_tidslinje").first()
        val overstyringId = finnOverstyringId(person.fødselsnummer)

        assertNotNull(overstyringId)
        assertEquals(overstyringId.toString(), hendelse["@id"].asText())
        assertEquals(person.fødselsnummer, hendelse["fødselsnummer"].asText())
        assertEquals(person { 2.ag }, hendelse["organisasjonsnummer"].asText())

        val overstyrtDag = hendelse["dager"].toList().single()
        assertEquals(10.januar, overstyrtDag["dato"].asLocalDate())
        assertEquals("Sykedag", overstyrtDag["type"].asText())
        assertEquals("Arbeidsdag", overstyrtDag["fraType"].asText())
        assertEquals(null, overstyrtDag["grad"]?.textValue())
        assertEquals(100, overstyrtDag["fraGrad"].asInt())
    }

    @Test
    fun `håndterer overstyring av arbeidsforhold`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val overstyring =
            ApiArbeidsforholdOverstyringHandling(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                overstyrteArbeidsforhold =
                    listOf(
                        ApiOverstyringArbeidsforhold(
                            orgnummer = ORGANISASJONSNUMMER_GHOST,
                            deaktivert = true,
                            begrunnelse = "en begrunnelse",
                            forklaring = "en forklaring",
                            lovhjemmel = ApiLovhjemmel("8-15", null, null, "folketrygdloven", "1998-12-18"),
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)
        val hendelse = testRapid.inspektør.hendelser("overstyr_arbeidsforhold").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())

        val overstyrtArbeidsforhold = hendelse["overstyrteArbeidsforhold"].toList().single()
        assertEquals("en begrunnelse", overstyrtArbeidsforhold["begrunnelse"].asText())
        assertEquals("en forklaring", overstyrtArbeidsforhold["forklaring"].asText())
        assertEquals(ORGANISASJONSNUMMER_GHOST, overstyrtArbeidsforhold["orgnummer"].asText())
        assertEquals(false, overstyrtArbeidsforhold["orgnummer"].asBoolean())
    }

    @Test
    fun `håndterer overstyring av inntekt og refusjon`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val overstyring =
            ApiInntektOgRefusjonOverstyring(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                vedtaksperiodeId = UUID.randomUUID(),
                arbeidsgivere =
                    listOf(
                        ApiOverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            manedligInntekt = 25000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(
                                        1.januar,
                                        31.januar,
                                        25000.0
                                    ),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 24000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(
                                        1.januar,
                                        31.januar,
                                        24000.0
                                    ),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            begrunnelse = "En begrunnelse",
                            forklaring = "En forklaring",
                            fom = null,
                            tom = null,
                        ),
                        ApiOverstyringArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            manedligInntekt = 21000.0,
                            fraManedligInntekt = 25001.0,
                            refusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(
                                        1.januar,
                                        31.januar,
                                        21000.0
                                    ),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 22000.0),
                                ),
                            fraRefusjonsopplysninger =
                                listOf(
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(
                                        1.januar,
                                        31.januar,
                                        22000.0
                                    ),
                                    ApiOverstyringArbeidsgiver.ApiOverstyringRefusjonselement(1.februar, null, 23000.0),
                                ),
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            begrunnelse = "En begrunnelse 2",
                            forklaring = "En forklaring 2",
                            fom = null,
                            tom = null,
                        ),
                    ),
            )

        mediator.håndter(overstyring, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("overstyr_inntekt_og_refusjon").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())
        hendelse["arbeidsgivere"].first().let {
            assertEquals(ORGANISASJONSNUMMER, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelse", it["begrunnelse"].asText())
            assertEquals("En forklaring", it["forklaring"].asText())
            assertEquals(25000.0, it["månedligInntekt"].asDouble())
            assertEquals(2, it["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(25000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertEquals(24000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
        }
        hendelse["arbeidsgivere"].last().let {
            assertEquals(ORGANISASJONSNUMMER_GHOST, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelse 2", it["begrunnelse"].asText())
            assertEquals("En forklaring 2", it["forklaring"].asText())
            assertEquals(21000.0, it["månedligInntekt"].asDouble())
            assertEquals(2, it["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(21000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertEquals(22000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
        }
    }

    @Test
    fun `håndterer skjønnsfastsetting av sykepengegrunnlag`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, aktørId = AKTØR_ID, organisasjonsnummer = ORGANISASJONSNUMMER)
        val skjønnsfastsetting =
            ApiSkjonnsfastsettelse(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                skjaringstidspunkt = 1.januar,
                arbeidsgivere =
                    listOf(
                        ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            arlig = 25000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak",
                            type = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
                            begrunnelseMal = "En begrunnelsemal",
                            begrunnelseFritekst = "begrunnelsefritekst",
                            begrunnelseKonklusjon = "begrunnelseKonklusjon",
                            initierendeVedtaksperiodeId = PERIODE.id.toString(),
                        ),
                        ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER_GHOST,
                            arlig = 21000.0,
                            fraArlig = 25001.0,
                            lovhjemmel = ApiLovhjemmel("8-28", "3", null, "folketrygdloven", "1970-01-01"),
                            arsak = "En årsak 2",
                            type = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
                            begrunnelseMal = "En begrunnelsemal",
                            begrunnelseFritekst = "begrunnelsefritekst",
                            begrunnelseKonklusjon = "begrunnelseKonklusjon",
                            initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                        ),
                    ),
                vedtaksperiodeId = PERIODE.id,
            )

        mediator.håndter(skjønnsfastsetting, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("skjønnsmessig_fastsettelse").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        assertEquals(1.januar, hendelse["skjæringstidspunkt"].asLocalDate())
        hendelse["arbeidsgivere"].first().let {
            assertEquals(ORGANISASJONSNUMMER, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelsemal", it["begrunnelseMal"].asText())
            assertEquals("begrunnelsefritekst", it["begrunnelseFritekst"].asText())
            assertEquals("begrunnelseKonklusjon", it["begrunnelseKonklusjon"].asText())
            assertEquals("En årsak", it["årsak"].asText())
            assertEquals(25000.0, it["årlig"].asDouble())
            assertEquals(25001.0, it["fraÅrlig"].asDouble())
        }
        hendelse["arbeidsgivere"].last().let {
            assertEquals(ORGANISASJONSNUMMER_GHOST, it["organisasjonsnummer"].asText())
            assertEquals("En begrunnelsemal", it["begrunnelseMal"].asText())
            assertEquals("begrunnelsefritekst", it["begrunnelseFritekst"].asText())
            assertEquals("begrunnelseKonklusjon", it["begrunnelseKonklusjon"].asText())
            assertEquals("En årsak 2", it["årsak"].asText())
            assertEquals(21000.0, it["årlig"].asDouble())
            assertEquals(25001.0, it["fraÅrlig"].asDouble())
        }
    }

    @Test
    fun `håndterer vurdering av minimum sykdomsgrad`() {
        nyPerson(fødselsnummer = FØDSELSNUMMER, organisasjonsnummer = ORGANISASJONSNUMMER, aktørId = AKTØR_ID)
        val minimumSykdomsgrad =
            ApiMinimumSykdomsgrad(
                fodselsnummer = FØDSELSNUMMER,
                aktorId = AKTØR_ID,
                perioderVurdertOk = listOf(
                    ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 1.januar,
                        tom = 15.januar
                    ), ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 30.januar,
                        tom = 31.januar
                    )
                ),
                perioderVurdertIkkeOk = listOf(
                    ApiMinimumSykdomsgrad.ApiPeriode(
                        fom = 16.januar,
                        tom = 29.januar
                    )
                ),
                begrunnelse = "en begrunnelse",
                arbeidsgivere =
                    listOf(
                        ApiMinimumSykdomsgrad.ApiArbeidsgiver(
                            organisasjonsnummer = ORGANISASJONSNUMMER,
                            berortVedtaksperiodeId = PERIODE.id,
                        ),
                    ),
                initierendeVedtaksperiodeId = PERIODE.id,
            )

        mediator.håndter(minimumSykdomsgrad, saksbehandler)

        val hendelse = testRapid.inspektør.hendelser("minimum_sykdomsgrad_vurdert").first()

        assertNotNull(hendelse["@id"].asText())
        assertEquals(FØDSELSNUMMER, hendelse["fødselsnummer"].asText())
        assertEquals(AKTØR_ID, hendelse["aktørId"].asText())
        assertEquals(SAKSBEHANDLER_OID, hendelse["saksbehandlerOid"].asText().let { UUID.fromString(it) })
        assertEquals(SAKSBEHANDLER_NAVN, hendelse["saksbehandlerNavn"].asText())
        assertEquals(SAKSBEHANDLER_IDENT, hendelse["saksbehandlerIdent"].asText())
        assertEquals(SAKSBEHANDLER_EPOST, hendelse["saksbehandlerEpost"].asText())
        hendelse["perioderMedMinimumSykdomsgradVurdertOk"].first().let {
            assertEquals(1.januar, it["fom"].asLocalDate())
            assertEquals(15.januar, it["tom"].asLocalDate())
        }
        hendelse["perioderMedMinimumSykdomsgradVurdertOk"].last().let {
            assertEquals(30.januar, it["fom"].asLocalDate())
            assertEquals(31.januar, it["tom"].asLocalDate())
        }
        hendelse["perioderMedMinimumSykdomsgradVurdertIkkeOk"].first().let {
            assertEquals(16.januar, it["fom"].asLocalDate())
            assertEquals(29.januar, it["tom"].asLocalDate())
        }
    }

    @Test
    fun `opphev stans`() {
        nyPerson()
        mediator.håndter(ApiOpphevStans(FNR, "EN_ÅRSAK"), saksbehandler)
        assertStansOpphevet(FNR)
    }

    private fun assertStansOpphevet(fødselsnummer: String) {
        val status =
            dbQuery.single(
                "select status from stans_automatisering where fødselsnummer = :fnr",
                "fnr" to fødselsnummer,
            ) { it.string(1) }
        assertEquals("NORMAL", status)
    }

    private fun finnOverstyringId(fødselsnummer: String): UUID {
        return dbQuery.single(
            "select ekstern_hendelse_id from overstyring where person_ref = (select id from person where fødselsnummer = :fodselsnummer)",
            "fodselsnummer" to fødselsnummer
        ) { it.uuid("ekstern_hendelse_id") }
    }

    private fun annullering(
        kommentar: String? = "EN_KOMMENTAR",
        arsaker: List<ApiAnnulleringArsak> =
            listOf(
                ApiAnnulleringArsak(_key = "key01", arsak = "Ferie"),
                ApiAnnulleringArsak(_key = "key02", arsak = "Perm")
            ),
    ) = ApiAnnulleringData(
        aktorId = AKTØR_ID,
        fodselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE,
        utbetalingId = UTBETALING_ID,
        arbeidsgiverFagsystemId = "EN-FAGSYSTEMID${Random.nextInt(1000)}",
        personFagsystemId = "EN-FAGSYSTEMID${Random.nextInt(1000)}",
        arsaker = arsaker,
        kommentar = kommentar,
    )
}
