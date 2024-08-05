import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.Query
import kotliquery.Row
import kotliquery.action.QueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.januar
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.MeldingDuplikatkontrollDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    private val testperson = TestPerson()
    protected open val HENDELSE_ID: UUID = UUID.randomUUID()

    protected val VEDTAKSPERIODE: UUID = testperson.vedtaksperiodeId1

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

    protected val FOM: LocalDate = LocalDate.of(2018, 1, 1)

    protected val TOM: LocalDate = LocalDate.of(2018, 1, 31)

    protected open val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected open val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    protected open val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    protected open val SAKSBEHANDLER_IDENT = "Z999999"

    protected val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))

    protected companion object {
        internal val objectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }

    internal var personId: Long = -1
        private set
    internal var arbeidsgiverId: Long = -1
        private set
    internal var snapshotId: Int = -1
        private set
    internal var vedtakId: Long = -1
        private set
    internal var oppgaveId: Long = -1
        private set

    internal val personDao = PersonDao(dataSource)
    internal val oppgaveDao = OppgaveDao(dataSource)
    internal val oppgaveApiDao = OppgaveApiDao(dataSource)
    internal val periodehistorikkDao = PeriodehistorikkDao(dataSource)
    internal val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    internal val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    internal val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    internal val snapshotDao = SnapshotDao(dataSource)
    internal val vedtakDao = VedtakDao(dataSource)
    internal val commandContextDao = CommandContextDao(dataSource)
    internal val tildelingDao = TildelingDao(dataSource)
    internal val saksbehandlerDao = SaksbehandlerDao(dataSource)
    internal val overstyringDao = OverstyringDao(dataSource)
    internal val overstyringApiDao = OverstyringApiDao(dataSource)
    internal val reservasjonDao = ReservasjonDao(dataSource)
    internal val meldingDao = MeldingDao(dataSource)
    internal val meldingDuplikatkontrollDao = MeldingDuplikatkontrollDao(dataSource)
    internal val risikovurderingDao = RisikovurderingDao(dataSource)
    internal val automatiseringDao = AutomatiseringDao(dataSource)
    internal val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    internal val egenAnsattDao = EgenAnsattDao(dataSource)
    internal val opptegnelseDao = OpptegnelseDao(dataSource)
    internal val abonnementDao = AbonnementDao(dataSource)
    internal val utbetalingDao = UtbetalingDao(dataSource)
    internal val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    internal val vergemålDao = VergemålDao(dataSource)
    internal val generasjonDao = GenerasjonDao(dataSource)
    internal val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    internal val dokumentDao = DokumentDao(dataSource)
    internal val påVentDao = PåVentDao(dataSource)
    internal val stansAutomatiskBehandlingDao = StansAutomatiskBehandlingDao(dataSource)
    internal val notatDao = NotatDao(dataSource)
    internal val annulleringDao = AnnulleringDao(dataSource)

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
        json: String = "{}",
    ) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO hendelse(id, fodselsnummer, data, type) VALUES(?, ?, ?::json, ?)",
                    hendelseId,
                    fødselsnummer.toLong(),
                    json,
                    type,
                ).asExecute,
            )
        }
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
        generasjonId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        oppgaveEgenskaper: List<EgenskapForDatabase> = listOf(EGENSKAP),
    ) {
        opprettPerson(fødselsnummer = fødselsnummer, aktørId = aktørId)
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer)
        opprettGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        opprettVedtaksperiode(
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
        )
        opprettOppgave(
            contextId = contextId,
            vedtaksperiodeId = vedtaksperiodeId,
            egenskaper = oppgaveEgenskaper,
            hendelseId = hendelseId,
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
        @Language("PostgreSQL")
        val query = "INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref) VALUES (?, ?)"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, saksbehandlerOid, oppgaveId).asExecute)
        }
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
            personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, adressebeskyttelse)
        val infotrygdutbetalingerId = personDao.upsertInfotrygdutbetalinger(fødselsnummer, objectMapper.createObjectNode())
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

    protected fun opprettMinimalPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
    ): Long {
        return personDao.insertPerson(fødselsnummer, aktørId)
    }

    protected fun opprettSaksbehandler(
        saksbehandlerOID: UUID = SAKSBEHANDLER_OID,
        navn: String = "SAKSBEHANDLER SAKSBEHANDLERSEN",
        epost: String = "epost@nav.no",
        ident: String = "Z999999",
    ): UUID {
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerOID, navn, epost, ident)
        return saksbehandlerOID
    }

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGNUMMER,
        navn: String = ORGNAVN,
        bransjer: List<String> = BRANSJER,
    ): Long {
        return arbeidsgiverDao.insertArbeidsgiver(organisasjonsnummer, navn, bransjer)!!.also { arbeidsgiverId = it }
    }

    protected fun opprettSnapshot(
        person: GraphQLPerson = snapshot(fødselsnummer = FNR, aktørId = AKTØR).data!!.person!!,
        fødselsnummer: String = FNR,
    ) {
        snapshotId = snapshotDao.lagre(fødselsnummer, person)
    }

    protected fun opprettGenerasjon(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID? = null,
    ) {
        generasjonDao.finnSisteGenerasjonFor(vedtaksperiodeId)?.also {
            generasjonDao.oppdaterTilstandFor(generasjonId = it, ny = Generasjon.VedtakFattet.navn(), endretAv = UUID.randomUUID())
        }
        generasjonDao.opprettFor(
            generasjonId,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
            utbetalingId,
        )
    }

    protected fun opprettVedtaksperiode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        forkastet: Boolean = false,
    ): Long {
        return vedtakDao.opprett(vedtaksperiodeId, fom, tom, personId, arbeidsgiverId)
            .let { vedtakDao.finnVedtakId(vedtaksperiodeId) }
            ?.also {
                vedtakId = it
                opprettVedtakstype(vedtaksperiodeId, periodetype, inntektskilde)
                if (forkastet) vedtakDao.markerForkastet(vedtaksperiodeId, UUID.randomUUID())
            }
            ?: fail { "Kunne ikke opprette vedtak" }
    }

    protected fun opprettOppgave(
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        egenskaper: List<EgenskapForDatabase> = listOf(EGENSKAP),
        kanAvvises: Boolean = true,
        utbetalingId: UUID = UTBETALING_ID,
        hendelseId: UUID = UUID.randomUUID(),
    ) {
        val hendelse = testhendelse(hendelseId = hendelseId)
        opprettCommandContext(hendelse, contextId)
        oppgaveId =
            oppgaveDao.opprettOppgave(
                nextLong().also { OPPGAVE_ID = it },
                contextId,
                egenskaper,
                vedtaksperiodeId,
                utbetalingId,
                kanAvvises,
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
            orgnummer = ORGNUMMER,
            type = utbetalingtype,
            opprettet = LocalDateTime.now(),
            arbeidsgiverFagsystemIdRef = arbeidsgiverOppdragId,
            personFagsystemIdRef = personOppdragId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )

    protected fun lagLinje(
        oppdrag: Long,
        fom: LocalDate,
        tom: LocalDate,
        totalbeløp: Int? = null,
    ) {
        utbetalingDao.nyLinje(
            oppdragId = oppdrag,
            fom = fom,
            tom = tom,
            totalbeløp = totalbeløp,
        )
    }

    protected fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    protected data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    protected fun snapshot(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        versjon: Int = 1,
    ): GraphQLClientResponse<HentSnapshot.Result> =
        object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data =
                HentSnapshot.Result(
                    GraphQLPerson(
                        versjon = versjon,
                        aktorId = aktørId,
                        fodselsnummer = fødselsnummer,
                        arbeidsgivere =
                            listOf(
                                GraphQLArbeidsgiver(
                                    organisasjonsnummer = "987654321",
                                    ghostPerioder = emptyList(),
                                    generasjoner =
                                        listOf(
                                            GraphQLGenerasjon(
                                                id = UUID.randomUUID(),
                                                perioder =
                                                    listOf(
                                                        GraphQLUberegnetPeriode(
                                                            erForkastet = false,
                                                            fom = 1.januar(2020),
                                                            tom = 31.januar(2020),
                                                            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                                            opprettet = 31.januar(2020).atStartOfDay(),
                                                            periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                                            tidslinje = emptyList(),
                                                            vedtaksperiodeId = UUID.randomUUID(),
                                                            periodetilstand = GraphQLPeriodetilstand.VENTERPAANNENPERIODE,
                                                            skjaeringstidspunkt = 1.januar(2020),
                                                            hendelser = emptyList(),
                                                            behandlingId = UUID.randomUUID(),
                                                        ),
                                                    ),
                                            ),
                                        ),
                                ),
                            ),
                        dodsdato = null,
                        vilkarsgrunnlag = emptyList(),
                    ),
                )
        }

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        generasjonId: UUID,
        definisjonRef: Long? = null,
        status: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_varsel(unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
            VALUES (?, ?, ?, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?), ?, ?, ?, ?, ?)
        """
        session.run(
            queryOf(
                query,
                id,
                kode,
                vedtaksperiodeId,
                generasjonId,
                definisjonRef,
                LocalDateTime.now(),
                status,
                if (endretTidspunkt != null) "EN_IDENT" else null,
                endretTidspunkt,
            ).asExecute,
        )
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query = """
            INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, opprettet) 
            VALUES (?, ?, ?, ?, ?, ?)    
        """
            requireNotNull(
                session.run(
                    queryOf(
                        query,
                        definisjonId,
                        kode,
                        tittel,
                        null,
                        null,
                        LocalDateTime.now(),
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        }

    protected fun settSaksbehandler(
        vedtaksperiodeId: UUID,
        saksbehandlerOid: UUID,
    ) = query(
        """
        UPDATE totrinnsvurdering
        SET saksbehandler = :saksbehandlerOid, oppdatert = now()
        WHERE vedtaksperiode_id = :vedtaksperiodeId AND utbetaling_id_ref IS null
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "saksbehandlerOid" to saksbehandlerOid,
    ).execute()

    protected fun assertGodkjenteVarsler(
        generasjonId: UUID,
        forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND status = 'GODKJENT'"
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonId).map { it.int(1) }.asSingle)
            }
        Assertions.assertEquals(forventetAntall, antall)
    }

    protected fun assertAvvisteVarsler(
        generasjonId: UUID,
        forventetAntall: Int,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND status = 'AVVIST'"
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonId).map { it.int(1) }.asSingle)
            }
        Assertions.assertEquals(forventetAntall, antall)
    }

    protected data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    protected fun query(
        @Language("postgresql") query: String,
        vararg params: Pair<String, Any?>,
    ) = queryOf(query, params.toMap())

    protected fun <T> Query.single(mapper: (Row) -> T?) = map(mapper).asSingle.runInSession()

    protected fun <T> Query.list(mapper: (Row) -> T?) = map(mapper).asList.runInSession()

    protected fun Query.update() = asUpdate.runInSession()

    protected fun Query.execute() = asExecute.runInSession()

    private fun <T> QueryAction<T>.runInSession() = sessionOf(dataSource).use(::runWithSession)
}
