import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotliquery.Query
import kotliquery.Row
import kotliquery.action.QueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.januar
import no.nav.helse.mediator.FeilendeMeldingerDao
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periode
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import kotlin.random.Random.Default.nextLong

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val HENDELSE_ID = UUID.randomUUID()

        internal val VEDTAKSPERIODE = UUID.randomUUID()

        internal val UTBETALING_ID = UUID.randomUUID()

        internal var OPPGAVE_ID = nextLong()
            private set
        internal const val OPPGAVETYPE = "SØKNAD"
        internal const val OPPGAVESTATUS = "AvventerSaksbehandler"

        internal const val ORGNUMMER = "123456789"
        internal const val ORGNAVN = "NAVN AS"
        internal val BRANSJER = listOf("EN BRANSJE")

        internal const val FNR = "02345678911"
        internal const val AKTØR = "4321098765432"
        internal const val FORNAVN = "Kari"
        internal const val MELLOMNAVN = "Mellomnavn"
        internal const val ETTERNAVN = "Nordmann"
        internal val FØDSELSDATO = LocalDate.EPOCH
        internal val KJØNN = Kjønn.Kvinne
        internal val ADRESSEBESKYTTELSE = Adressebeskyttelse.Ugradert
        internal const val ENHET = "0301"

        internal val FOM = LocalDate.of(2018, 1, 1)

        internal val TOM = LocalDate.of(2018, 1, 31)
        internal val SAKSBEHANDLER_OID = UUID.randomUUID()

        internal const val SAKSBEHANDLEREPOST = "sara.saksbehandler@nav.no"
        internal const val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
        internal const val SAKSBEHANDLER_IDENT = "Z999999"

        val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
    }

    private val KODE7_GRUPPE_ID = UUID.randomUUID()
    private val RISK_GRUPPE_ID = UUID.randomUUID()
    private val BESLUTTER_GRUPPE_ID = UUID.randomUUID()
    private val SKJERMEDE_PERSONER_GRUPPE_ID = UUID.randomUUID()

    protected val SAKSBEHANDLERTILGANGER_MED_INGEN = SaksbehandlerTilganger(
        gruppetilganger = emptyList(),
        kode7Saksbehandlergruppe = KODE7_GRUPPE_ID,
        riskSaksbehandlergruppe = RISK_GRUPPE_ID,
        beslutterSaksbehandlergruppe = BESLUTTER_GRUPPE_ID,
        skjermedePersonerSaksbehandlergruppe = SKJERMEDE_PERSONER_GRUPPE_ID,
        saksbehandlerIdent = SAKSBEHANDLER_IDENT,
        saksbehandlereMedTilgangTilStikkprøve = emptyList()
    )
    protected val SAKSBEHANDLERTILGANGER_MED_KODE7 = SaksbehandlerTilganger(
        gruppetilganger = listOf(KODE7_GRUPPE_ID),
        kode7Saksbehandlergruppe = KODE7_GRUPPE_ID,
        riskSaksbehandlergruppe = RISK_GRUPPE_ID,
        beslutterSaksbehandlergruppe = BESLUTTER_GRUPPE_ID,
        skjermedePersonerSaksbehandlergruppe = SKJERMEDE_PERSONER_GRUPPE_ID,
        saksbehandlerIdent = SAKSBEHANDLER_IDENT,
        saksbehandlereMedTilgangTilStikkprøve = emptyList()
    )
    protected val SAKSBEHANDLERTILGANGER_MED_RISK = SaksbehandlerTilganger(
        gruppetilganger = listOf(RISK_GRUPPE_ID),
        kode7Saksbehandlergruppe = KODE7_GRUPPE_ID,
        riskSaksbehandlergruppe = RISK_GRUPPE_ID,
        beslutterSaksbehandlergruppe = BESLUTTER_GRUPPE_ID,
        skjermedePersonerSaksbehandlergruppe = SKJERMEDE_PERSONER_GRUPPE_ID,
        saksbehandlerIdent = SAKSBEHANDLER_IDENT,
        saksbehandlereMedTilgangTilStikkprøve = emptyList()
    )
    protected val SAKSBEHANDLERTILGANGER_MED_BESLUTTER = SaksbehandlerTilganger(
        gruppetilganger = listOf(BESLUTTER_GRUPPE_ID),
        kode7Saksbehandlergruppe = KODE7_GRUPPE_ID,
        riskSaksbehandlergruppe = RISK_GRUPPE_ID,
        beslutterSaksbehandlergruppe = BESLUTTER_GRUPPE_ID,
        skjermedePersonerSaksbehandlergruppe = SKJERMEDE_PERSONER_GRUPPE_ID,
        saksbehandlerIdent = SAKSBEHANDLER_IDENT,
        saksbehandlereMedTilgangTilStikkprøve = emptyList()
    )
    protected val SAKSBEHANDLERTILGANGER_MED_STIKKPRØVE = SaksbehandlerTilganger(
        gruppetilganger = emptyList(),
        kode7Saksbehandlergruppe = KODE7_GRUPPE_ID,
        riskSaksbehandlergruppe = RISK_GRUPPE_ID,
        beslutterSaksbehandlergruppe = BESLUTTER_GRUPPE_ID,
        skjermedePersonerSaksbehandlergruppe = SKJERMEDE_PERSONER_GRUPPE_ID,
        saksbehandlerIdent = SAKSBEHANDLER_IDENT,
        saksbehandlereMedTilgangTilStikkprøve = listOf(SAKSBEHANDLER_IDENT)
    )

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
    internal val hendelseDao = HendelseDao(dataSource)
    internal val risikovurderingDao = RisikovurderingDao(dataSource)
    internal val automatiseringDao = AutomatiseringDao(dataSource)
    internal val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    internal val egenAnsattDao = EgenAnsattDao(dataSource)
    internal val opptegnelseDao = OpptegnelseDao(dataSource)
    internal val abonnementDao = AbonnementDao(dataSource)
    internal val utbetalingDao = UtbetalingDao(dataSource)
    internal val feilendeMeldingerDao = FeilendeMeldingerDao(dataSource)
    internal val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)
    internal val vergemålDao = VergemålDao(dataSource)
    internal val generasjonDao = GenerasjonDao(dataSource)
    internal val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)

    internal fun testhendelse(
        hendelseId: UUID = HENDELSE_ID,
        vedtaksperiodeId: UUID? = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        type: String = "GODKJENNING",
        json: String = "{}"
    ) = TestHendelse(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type, json)
    }

    protected fun godkjenningsbehov(
        hendelseId: UUID = HENDELSE_ID,
        fødselsnummer: String = FNR,
        json: String = "{}"
    ) {
        lagreHendelse(hendelseId, fødselsnummer, "GODKJENNING", json)
    }

    private fun lagreHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FNR,
        type: String,
        json: String = "{}"
    ) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "INSERT INTO hendelse(id, fodselsnummer, data, type) VALUES(?, ?, ?::json, ?)",
                    hendelseId,
                    fødselsnummer.toLong(),
                    json,
                    type
                ).asExecute
            )
        }
    }

    protected fun nyttAutomatiseringsinnslag(automatisert: Boolean) {
        if (automatisert) automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        else automatiseringDao.manuellSaksbehandling(listOf("Dårlig ånde"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
    }

    protected fun nyPerson(
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        organisasjonsnummer: String = ORGNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        generasjonId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        oppgaveEgenskaper: List<String> = listOf(OPPGAVETYPE),
    ) {
        opprettPerson(fødselsnummer = fødselsnummer, aktørId = aktørId)
        opprettArbeidsgiver(organisasjonsnummer = organisasjonsnummer)
        opprettGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        opprettVedtaksperiode(periodetype = periodetype, inntektskilde = inntektskilde, vedtaksperiodeId = vedtaksperiodeId, fødselsnummer = fødselsnummer)
        opprettOppgave(contextId = contextId, vedtaksperiodeId = vedtaksperiodeId, hendelseId = hendelseId, oppgavetype = OPPGAVETYPE, egenskaper = oppgaveEgenskaper)
    }

    private fun opprettCommandContext(hendelse: TestHendelse, contextId: UUID) {
        commandContextDao.opprett(hendelse, contextId)
    }

    protected fun nyVedtaksperiode(periodetype: Periodetype = FØRSTEGANGSBEHANDLING) {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(vedtaksperiodeId, periodetype = periodetype)
        opprettOppgave(vedtaksperiodeId = vedtaksperiodeId)
    }

    private fun opprettVedtakstype(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        type: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER
    ) {
        vedtakDao.leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
    }

    protected fun opprettPerson(
        fødselsnummer: String = FNR,
        aktørId: String = AKTØR,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert
    ): Persondata {
        val personinfoId =
            personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, adressebeskyttelse)
        val infotrygdutbetalingerId = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = personDao.insertPerson(fødselsnummer, aktørId, personinfoId, enhetId, infotrygdutbetalingerId)
        return Persondata(
            personId = personId,
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId
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
        bransjer: List<String> = BRANSJER
    ): Long {
        return arbeidsgiverDao.insertArbeidsgiver(organisasjonsnummer, navn, bransjer)!!.also { arbeidsgiverId = it }
    }

    protected fun opprettSnapshot(
        person: GraphQLPerson = snapshot().data!!.person!!,
        fødselsnummer: String = FNR,
    ) {
        snapshotId = snapshotDao.lagre(fødselsnummer, person)
    }

    protected fun opprettGenerasjon(vedtaksperiodeId: UUID = VEDTAKSPERIODE, generasjonId: UUID = UUID.randomUUID()) {
        generasjonDao.finnSisteGenerasjonFor(vedtaksperiodeId)?.also {
            generasjonDao.oppdaterTilstandFor(generasjonId = it, ny = Generasjon.Låst, endretAv = UUID.randomUUID())
        }
        generasjonDao.opprettFor(
            generasjonId,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
    }

    protected fun utbetalingForSisteGenerasjon(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        utbetalingId: UUID
    ) {
        generasjonDao.utbetalingFor(generasjonId = generasjonDao.finnSisteGenerasjonFor(vedtaksperiodeId)!!, utbetalingId)
    }

    protected fun opprettVedtaksperiode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        forkastet: Boolean = false,
        fødselsnummer: String = FNR,
    ): Long {
        opprettSnapshot(fødselsnummer = fødselsnummer)
        return vedtakDao.opprett(vedtaksperiodeId, fom, tom, personId, arbeidsgiverId, snapshotId)
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
        oppgavetype: String = OPPGAVETYPE,
        egenskaper: List<String> = listOf(OPPGAVETYPE),
        utbetalingId: UUID = UTBETALING_ID,
        hendelseId: UUID = UUID.randomUUID()
    ) {
        val hendelse = testhendelse(hendelseId = hendelseId)
        opprettCommandContext(hendelse, contextId)
        oppgaveId = oppgaveDao.opprettOppgave(
            nextLong().also { OPPGAVE_ID = it },
            contextId,
            oppgavetype,
            egenskaper,
            vedtaksperiodeId,
            utbetalingId
        )
    }

    protected fun ferdigstillSistOpprettedeOppgaveOgOpprettNy() {
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "Ferdigstilt")
        opprettOppgave()
    }

    protected fun opprettTotrinnsvurdering(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        saksbehandler: UUID? = null,
        erRetur: Boolean = false,
        ferdigstill: Boolean = false
    ) {
        totrinnsvurderingDao.opprett(vedtaksperiodeId)

        if (saksbehandler != null) {
            totrinnsvurderingDao.settSaksbehandler(vedtaksperiodeId, saksbehandler)
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
        utbetalingId: UUID
    ) {
        utbetalingDao.opprettKobling(vedtaksperiodeId, utbetalingId)
    }

    protected fun lagArbeidsgiveroppdrag(
        fagsystemId: String = fagsystemId(),
        mottaker: String = ORGNUMMER
    ) =
        utbetalingDao.nyttOppdrag(fagsystemId, mottaker)!!

    protected fun lagPersonoppdrag(fagsystemId: String = fagsystemId()) =
        utbetalingDao.nyttOppdrag(fagsystemId, FNR)!!

    protected fun lagUtbetalingId(
        arbeidsgiverOppdragId: Long,
        personOppdragId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        arbeidsgiverbeløp: Int = 2000,
        personbeløp: Int = 2000,
    ): Long =
        utbetalingDao.opprettUtbetalingId(
            utbetalingId = utbetalingId,
            fødselsnummer = FNR,
            orgnummer = ORGNUMMER,
            type = Utbetalingtype.UTBETALING,
            opprettet = LocalDateTime.now(),
            arbeidsgiverFagsystemIdRef = arbeidsgiverOppdragId,
            personFagsystemIdRef = personOppdragId,
            arbeidsgiverbeløp = arbeidsgiverbeløp,
            personbeløp = personbeløp,
        )

    protected fun lagLinje(oppdrag: Long, fom: LocalDate, tom: LocalDate, totalbeløp: Int? = null) {
        utbetalingDao.nyLinje(
            oppdragId = oppdrag,
            fom = fom,
            tom = tom,
            totalbeløp = totalbeløp
        )
    }

    protected fun opprettInntekt(fødselsnummer: String, skjæringstidspunkt: LocalDate, inntekter: List<Inntekter>) {
        personDao.insertInntekter(fødselsnummer, skjæringstidspunkt, inntekter)
    }

    protected fun hentUtbetalingMedUtbetalingId(utbetalingIdRef: Long): String? {
        @Language("PostgreSQL")
        val statement = "SELECT data FROM utbetaling WHERE utbetaling_id_ref = ? LIMIT 1;"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, utbetalingIdRef).map {
                it.string("data")
            }.asSingle)
        }
    }

    protected fun hentHendelse(hendelseId: UUID): String? {
        @Language("PostgreSQL")
        val statement = "SELECT data FROM hendelse WHERE id = ? LIMIT 1;"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, hendelseId).map {
                it.string("data")
            }.asSingle)
        }
    }

    protected fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    protected data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long,
    )

    protected fun snapshot(
        fødselsnummer: String = "12345612345",
        aktørId: String = "123456789101112",
        versjon: Int = 1,
    ): GraphQLClientResponse<HentSnapshot.Result> =
        object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data = HentSnapshot.Result(
                GraphQLPerson(
                    versjon = versjon,
                    aktorId = aktørId,
                    fodselsnummer = fødselsnummer,
                    arbeidsgivere = listOf(
                        GraphQLArbeidsgiver(
                            organisasjonsnummer = "987654321",
                            ghostPerioder = emptyList(),
                            generasjoner = listOf(
                                GraphQLGenerasjon(
                                    id = UUID.randomUUID().toString(),
                                    perioder = listOf(
                                        GraphQLUberegnetPeriode(
                                            erForkastet = false,
                                            fom = "2020-01-01",
                                            tom = "2020-01-31",
                                            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                            opprettet = "2020-01-31",
                                            periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                            tidslinje = emptyList(),
                                            vedtaksperiodeId = UUID.randomUUID().toString(),
                                            id = UUID.randomUUID().toString(),
                                            periodetilstand = GraphQLPeriodetilstand.VENTERPAANNENPERIODE,
                                            skjaeringstidspunkt = "2020-01-01",
                                            hendelser = emptyList(),
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    dodsdato = null,
                    vilkarsgrunnlag = emptyList(),
                )
            )
        }

    protected fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        tilstandEndretTidspunkt: LocalDateTime? = null,
        skjæringstidspunkt: LocalDate = periode.fom,
    ): UUID = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?, 'Ulåst', ?, ?, ?)
        """
        session.run(
            queryOf(
                query,
                vedtaksperiodeId,
                generasjonId,
                utbetalingId,
                UUID.randomUUID(),
                tilstandEndretTidspunkt,
                UUID.randomUUID(),
                periode.fom,
                periode.tom,
                skjæringstidspunkt
            ).asUpdateAndReturnGeneratedKey
        )
        return generasjonId
    }

    protected fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        generasjonId: UUID,
        definisjonRef: Long? = null,
    ) = nyttVarsel(id, vedtaksperiodeId, kode, generasjonId, definisjonRef, "AKTIV", null)

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
                endretTidspunkt
            ).asExecute
        )
    }

    protected fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
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
                    LocalDateTime.now()
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    protected fun assertGodkjenteVarsler(generasjonId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND status = 'GODKJENT'"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map { it.int(1) }.asSingle)
        }
        Assertions.assertEquals(forventetAntall, antall)
    }

    protected fun assertAvvisteVarsler(generasjonId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query =
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND status = 'AVVIST'"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map { it.int(1) }.asSingle)
        }
        Assertions.assertEquals(forventetAntall, antall)
    }

    protected data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    protected fun query(@Language("postgresql") query: String, vararg params: Pair<String, Any>) =
        queryOf(query, params.toMap())

    protected fun Query.update() = asUpdate.runInSession()

    protected fun <T> Query.single(mapper: (Row) -> T?) = map(mapper).asSingle.runInSession()

    protected fun <T> QueryAction<T>.runInSession() = sessionOf(dataSource).use(::runWithSession)
}
