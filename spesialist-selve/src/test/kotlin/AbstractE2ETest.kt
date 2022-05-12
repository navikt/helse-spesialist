import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.SaksbehandlerTilganger
import no.nav.helse.abonnement.AbonnementDao
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.januar
import no.nav.helse.mediator.FeilendeMeldingerDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.graphql.PersonQuery
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.mediator.api.graphql.SnapshotMediator
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.mediator.graphql.HentSnapshot
import no.nav.helse.mediator.graphql.enums.GraphQLBehandlingstype
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.hentsnapshot.Alder
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLAktivitet
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.AktivVedtaksperiodeJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.notat.NotatDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.person.PersonApiDao
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.snapshotMedWarnings
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import no.nav.helse.abonnement.OpptegnelseDao as OpptegnelseApiDao

internal abstract class AbstractE2ETest : AbstractDatabaseTest() {
    protected val VEDTAKSPERIODE_ID: UUID = UUID.randomUUID()
    private val DEFAULT_FØDSELSNUMER = "12020052345"
    protected var FØDSELSNUMMER = DEFAULT_FØDSELSNUMER

    protected val AKTØR = "999999999"
    protected val ORGNR = "222222222"
    protected val ORGNR_GHOST = "666666666"

    protected val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    protected val SAKSBEHANDLER_OID: UUID = UUID.randomUUID()
    protected val SAKSBEHANDLER_IDENT = "X999999"
    protected val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    protected val SAKSBEHANDLERTILGANGER_UTEN_TILGANGER =
        SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            riskSaksbehandlergruppe = UUID.randomUUID()
        )

    protected val SNAPSHOT_MED_WARNINGS = snapshotMedWarnings(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        orgnr = ORGNR,
        fnr = FØDSELSNUMMER,
        aktørId = AKTØR
    )

    protected val SNAPSHOT_UTEN_WARNINGS = snapshot()

    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val UTBETALING_ID = UUID.randomUUID()
        internal val UTBETALING_ID2 = UUID.randomUUID()
    }

    private val commandContextDao = CommandContextDao(dataSource)
    private val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val automatiseringDao = AutomatiseringDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    private val feilendeMeldingerDao = FeilendeMeldingerDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)

    protected val varselDao = VarselDao(dataSource)
    protected val personApiDao = PersonApiDao(dataSource)
    protected val oppgaveDao = OppgaveDao(dataSource)
    protected val personDao = PersonDao(dataSource)
    protected val vedtakDao = VedtakDao(dataSource)
    protected val warningDao = WarningDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    protected val risikovurderingDao = RisikovurderingDao(dataSource)
    protected val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val overstyringDao = OverstyringDao(dataSource)
    protected val overstyringApiDao = OverstyringApiDao(dataSource)
    protected val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    protected val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val utbetalingDao = UtbetalingDao(dataSource)
    protected val opptegnelseDao = OpptegnelseDao(dataSource)
    protected val opptegnelseApiDao = OpptegnelseApiDao(dataSource)
    protected val abonnementDao = AbonnementDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val reservasjonDao = ReservasjonDao(dataSource)
    protected val notatDao = NotatDao(dataSource)
    protected val vergemålDao = VergemålDao(dataSource)

    protected val snapshotClient = mockk<SnapshotClient>(relaxed = true)

    protected val testRapid = TestRapid()

    protected val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)

    protected val oppgaveMediator = OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao)
    protected val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = ArbeidsgiverDao(dataSource),
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        oppgaveDao = oppgaveDao,
        commandContextDao = commandContextDao,
        reservasjonDao = reservasjonDao,
        tildelingDao = tildelingDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = OverstyringDao(dataSource),
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        snapshotDao = snapshotDao,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao, opptegnelseDao),
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = automatiseringDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            vergemålDao = vergemålDao,
            snapshotDao = snapshotDao,
        ) { false },
        arbeidsforholdDao = arbeidsforholdDao,
        utbetalingDao = utbetalingDao,
        opptegnelseDao = opptegnelseDao,
        vergemålDao = vergemålDao,
    )
    internal val hendelseMediator = HendelseMediator(
        rapidsConnection = testRapid,
        dataSource = dataSource,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk,
        opptegnelseDao = opptegnelseDao
    )
    internal val snapshotMediator = SnapshotMediator(
        snapshotDao = snapshotDao,
        snapshotClient = snapshotClient,
    )


    internal val dataFetchingEnvironment = mockk<DataFetchingEnvironment>(relaxed = true)

    internal val personQuery = PersonQuery(
        personApiDao = personApiDao,
        egenAnsattDao = egenAnsattDao,
        tildelingDao = tildelingDao,
        arbeidsgiverApiDao = arbeidsgiverApiDao,
        overstyringApiDao = overstyringApiDao,
        risikovurderingApiDao = risikovurderingApiDao,
        varselDao = varselDao,
        oppgaveDao = oppgaveDao,
        snapshotMediator = snapshotMediator
    )

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
    }

    @AfterEach
    internal fun after() {
        FØDSELSNUMMER = DEFAULT_FØDSELSNUMER
    }

    private fun nyHendelseId() = UUID.randomUUID()

    protected fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendVedtaksperiodeEndret(
        orgnr: String = "orgnr",
        vedtaksperiodeId: UUID,
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND"
    ): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeEndret(
                id,
                vedtaksperiodeId,
                orgnr,
                forrigeTilstand,
                gjeldendeTilstand
            )
        )
    }

    protected fun sendAdressebeskyttelseEndret(): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagAdressebeskyttelseEndret(id))
    }

    protected fun sendGodkjenningsbehov(
        orgnr: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = LocalDate.now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØR,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        aktiveVedtaksperioder: List<AktivVedtaksperiodeJson> = listOf(
            AktivVedtaksperiodeJson(
                orgnr,
                vedtaksperiodeId,
                periodetype
            )
        ),
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        utbetalingtype: Utbetalingtype = UTBETALING
    ): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                orgnummer = orgnr,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                skjæringstidspunkt = skjæringstidspunkt,
                periodetype = periodetype,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                inntektskilde = inntektskilde,
                aktiveVedtaksperioder = aktiveVedtaksperioder,
                orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
                utbetalingtype = utbetalingtype
            )
        )
    }

    protected fun sendArbeidsgiverinformasjonløsning(
        hendelseId: UUID,
        orgnummer: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        navn: String = "En arbeidsgiver",
        bransjer: List<String> = listOf("En bransje", "En annen bransje"),
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson> = emptyList()
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsning(
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    orgnummer = orgnummer,
                    navn = navn,
                    bransjer = bransjer,
                    ekstraArbeidsgivere = ekstraArbeidsgivere
                )
            )
        }

    protected fun håndterAnnullering(annulleringDto: AnnulleringDto, saksbehandler: Saksbehandler) {
        hendelseMediator.håndter(annulleringDto, saksbehandler)
    }

    protected fun sendArbeidsforholdløsning(
        hendelseId: UUID,
        orgnr: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        løsning: List<Arbeidsforholdløsning.Løsning> = listOf(
            Arbeidsforholdløsning.Løsning(
                stillingstittel = "en-stillingstittel",
                stillingsprosent = 100,
                startdato = LocalDate.now(),
                sluttdato = null
            )
        )
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsforholdløsning(
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    organisasjonsnummer = orgnr,
                    løsning
                )
            )
        }

    protected fun sendHentPersoninfoLøsning(
        hendelseId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        adressebeskyttelse: String = "Ugradert"
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagHentPersoninfoløsning(
                    id,
                    hendelseId,
                    contextId,
                    adressebeskyttelse
                )
            )
        }

    protected fun sendKomposittbehov(
        hendelseId: UUID,
        behov: List<String>,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String = "orgnr",
        contextId: UUID = testRapid.inspektør.contextId(),
        detaljer: Map<String, Any>
    ): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagFullstendigBehov(
                id,
                hendelseId,
                contextId,
                vedtaksperiodeId,
                organisasjonsnummer,
                behov,
                detaljer
            )
        )
    }

    protected fun sendPersoninfoløsning(
        hendelseId: UUID,
        orgnr: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        enhet: String = "0301",
        adressebeskyttelse: String = "Ugradert"
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagPersoninfoløsning(
                    id,
                    hendelseId,
                    contextId,
                    vedtaksperiodeId,
                    orgnr,
                    enhet,
                    adressebeskyttelse
                )
            )
        }

    protected fun sendOverstyrteDager(
        dager: List<OverstyringDagDto>,
        orgnr: String = ORGNR,
        saksbehandlerEpost: String = SAKSBEHANDLER_EPOST,
        saksbehandlerOid: UUID = SAKSBEHANDLER_OID,
        saksbehandlerIdent: String = SAKSBEHANDLER_IDENT
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringTidslinje(
                    id = id,
                    dager = dager,
                    organisasjonsnummer = orgnr,
                    saksbehandlerEpost = saksbehandlerEpost,
                    saksbehandlerOid = saksbehandlerOid,
                    saksbehandlerident = saksbehandlerIdent,
                )
            )
        }

    protected fun sendOverstyrtInntekt(
        orgnr: String = ORGNR,
        månedligInntekt: Double = 25000.0,
        skjæringstidspunkt: LocalDate,
        forklaring: String = "testforklaring"
    ): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringInntekt(
                    id = id,
                    organisasjonsnummer = orgnr,
                    månedligInntekt = månedligInntekt,
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandlerEpost = SAKSBEHANDLER_EPOST,
                    forklaring = forklaring
                )
            )
        }

    protected fun sendOverstyrtArbeidsforhold(
        skjæringstidspunkt: LocalDate,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>
    ): UUID =
        nyHendelseId().also {
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringArbeidsforhold(
                    organisasjonsnummer = ORGNR,
                    skjæringstidspunkt = skjæringstidspunkt,
                    overstyrteArbeidsforhold = overstyrteArbeidsforhold
                )
            )
        }


    protected fun sendRevurderingAvvist(fødselsnummer: String, errors: List<String>): UUID =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRevurderingAvvist(
                    id = id,
                    fødselsnummer = fødselsnummer,
                    errors = errors
                )
            )
        }

    protected fun sendGosysOppgaveEndret(fødselsnummer: String, aktørId: String) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagGosysOppgaveEndret(
                    id = id,
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId
                )
            )
        }

    protected fun sendDigitalKontaktinformasjonløsning(
        godkjenningsmeldingId: UUID,
        erDigital: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagDigitalKontaktinformasjonløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    erDigital
                )
            )
        }
    }

    protected fun sendÅpneGosysOppgaverløsning(
        godkjenningsmeldingId: UUID,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagÅpneGosysOppgaverløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    antall,
                    oppslagFeilet
                )
            )
        }
    }

    protected fun sendRisikovurderingløsning(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId(),
        funn: List<Risikofunn> = emptyList()
    ): UUID {
        return nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRisikovurderingløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    vedtaksperiodeId,
                    kanGodkjennesAutomatisk,
                    funn
                )
            )
        }
    }

    protected fun sendEgenAnsattløsning(
        godkjenningsmeldingId: UUID,
        erEgenAnsatt: Boolean,
        fødselsnummer: String = FØDSELSNUMMER,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagEgenAnsattløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    erEgenAnsatt,
                    fødselsnummer,
                )
            )
        }
    }

    protected fun sendVergemålløsning(
        godkjenningsmeldingId: UUID,
        vergemål: VergemålJson = VergemålJson(),
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagVergemålløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    vergemål
                )
            )
        }
    }

    protected fun sendSaksbehandlerløsning(
        oppgaveId: Long,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandlerOid: UUID,
        godkjent: Boolean,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
    ): UUID {
        hendelseMediator.håndter(
            godkjenningDTO = GodkjenningDTO(
                oppgaveId,
                godkjent,
                saksbehandlerIdent,
                if (godkjent) null else "årsak",
                begrunnelser,
                kommentar
            ),
            epost = saksbehandlerEpost,
            oid = saksbehandlerOid
        )
        assertEquals("AvventerSystem", testRapid.inspektør.siste("oppgave_oppdatert").path("status").asText())
        val løsning = testRapid.inspektør.siste("saksbehandler_løsning")
        testRapid.sendTestMessage(løsning.toString())
        return UUID.fromString(løsning.path("@id").asText())
    }


    protected fun settOppBruker(orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList()): UUID {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_MED_WARNINGS
        val godkjenningsbehovId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            1.januar,
            31.januar,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
        sendPersoninfoløsning(godkjenningsbehovId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsbehovId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            ekstraArbeidsgivere = orgnummereMedRelevanteArbeidsforhold.map {
                ArbeidsgiverinformasjonJson(
                    orgnummer = it,
                    navn = "ghost",
                    bransjer = listOf("bransje")
                )
            }
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsbehovId,
            orgnr = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        klargjørForGodkjenning(godkjenningsbehovId)
        return godkjenningsbehovId
    }

    protected fun klargjørForGodkjenning(oppgaveId: UUID) {
        sendEgenAnsattløsning(oppgaveId, false)
        sendVergemålløsning(
            godkjenningsmeldingId = oppgaveId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = oppgaveId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = oppgaveId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = oppgaveId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
    }

    protected fun sendUtbetalingEndret(
        type: String,
        status: Utbetalingsstatus,
        orgnr: String,
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String = "ASJKLD90283JKLHAS3JKLF",
        forrigeStatus: Utbetalingsstatus = status,
        fødselsnummer: String = FØDSELSNUMMER,
        utbetalingId: UUID
    ) {
        @Language("JSON")
        val json = """
{
    "@event_name": "utbetaling_endret",
    "@id": "${UUID.randomUUID()}",
    "@opprettet": "${LocalDateTime.now()}",
    "utbetalingId": "$utbetalingId",
    "fødselsnummer": "$fødselsnummer",
    "type": "$type",
    "forrigeStatus": "$forrigeStatus",
    "gjeldendeStatus": "$status",
    "organisasjonsnummer": "$orgnr",
    "arbeidsgiverOppdrag": {
      "mottaker": "$orgnr",
      "fagområde": "SPREF",
      "endringskode": "NY",
      "fagsystemId": "$arbeidsgiverFagsystemId",
      "sisteArbeidsgiverdag": "${LocalDate.MIN}",
      "linjer": [
        {
          "fom": "${LocalDate.now()}",
          "tom": "${LocalDate.now()}",
          "dagsats": 2000,
          "totalbeløp": 2000,
          "lønn": 2000,
          "grad": 100.00,
          "refFagsystemId": "asdfg",
          "delytelseId": 2,
          "refDelytelseId": 1,
          "datoStatusFom": "${LocalDate.now()}",
          "endringskode": "NY",
          "klassekode": "SPREFAG-IOP",
          "statuskode": "OPPH"
        },
        {
          "fom": "${LocalDate.now()}",
          "tom": "${LocalDate.now()}",
          "dagsats": 2000,
          "totalbeløp": 2000,
          "lønn": 2000,
          "grad": 100.00,
          "refFagsystemId": null,
          "delytelseId": 3,
          "refDelytelseId": null,
          "datoStatusFom": null,
          "endringskode": "NY",
          "klassekode": "SPREFAG-IOP",
          "statuskode": null
        }
      ]
    },
    "personOppdrag": {
      "mottaker": "$FØDSELSNUMMER",
      "fagområde": "SP",
      "endringskode": "NY",
      "fagsystemId": "$personFagsystemId",
      "linjer": []
    }
}"""

        testRapid.sendTestMessage(json)
    }

    protected fun sendPersonUtbetalingEndret(
        type: String,
        status: Utbetalingsstatus,
        orgnr: String,
        arbeidsgiverFagsystemId: String = "DFGKJDWOAWODOAWOW",
        personFagsystemId: String = "ASJKLD90283JKLHAS3JKLF",
        forrigeStatus: Utbetalingsstatus = status,
        fødselsnummer: String = FØDSELSNUMMER,
        utbetalingId: UUID
    ) {
        @Language("JSON")
        val json = """
{
    "@event_name": "utbetaling_endret",
    "@id": "${UUID.randomUUID()}",
    "@opprettet": "${LocalDateTime.now()}",
    "utbetalingId": "$utbetalingId",
    "fødselsnummer": "$fødselsnummer",
    "type": "$type",
    "forrigeStatus": "$forrigeStatus",
    "gjeldendeStatus": "$status",
    "organisasjonsnummer": "$orgnr",
    "arbeidsgiverOppdrag": {
      "mottaker": "$orgnr",
      "fagområde": "SP",
      "endringskode": "NY",
      "fagsystemId": "$arbeidsgiverFagsystemId",
      "sisteArbeidsgiverdag": "${LocalDate.MIN}",
      "linjer": []
    },
    "personOppdrag": {
      "mottaker": "$FØDSELSNUMMER",
      "fagområde": "SP",
      "endringskode": "NY",
      "fagsystemId": "$personFagsystemId",
      "linjer": [{
          "fom": "${LocalDate.now()}",
          "tom": "${LocalDate.now()}",
          "dagsats": 2000,
          "totalbeløp": 2000,
          "lønn": 2000,
          "grad": 100.00,
          "refFagsystemId": "asdfg",
          "delytelseId": 2,
          "refDelytelseId": 1,
          "datoStatusFom": null,
          "endringskode": "NY",
          "klassekode": "SPATORD",
          "statuskode": null
        }]
    }
}"""

        testRapid.sendTestMessage(json)
    }

    protected fun sendUtbetalingAnnullert(
        arbeidsgiverFagsystemId: String = "ASDJ12IA312KLS",
        personFagsystemId: String = "BSDJ12IA312KLS",
        saksbehandlerEpost: String = "saksbehandler_epost"
    ) {
        @Language("JSON")
        val json = """
            {
                "@event_name": "utbetaling_annullert",
                "@id": "${UUID.randomUUID()}",
                "fødselsnummer": "$FØDSELSNUMMER",
                "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                "personFagsystemId": "$personFagsystemId",
                "utbetalingId": "$UTBETALING_ID",
                "tidspunkt": "${LocalDateTime.now()}",
                "epost": "$saksbehandlerEpost"
            }"""

        testRapid.sendTestMessage(json)
    }

    protected fun assertHendelse(hendelseId: UUID) {
        assertEquals(1, sessionOf(dataSource).use {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { row -> row.int(1) }.asSingle)
        })
    }

    protected fun assertIkkeHendelse(hendelseId: UUID) {
        assertEquals(0, sessionOf(dataSource).use {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { row -> row.int(1) }.asSingle)
        })
    }

    protected fun assertVedtak(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertIkkeVedtak(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = ?",
                        vedtaksperiodeId
                    ).map { row -> row.int(1) }.asSingle
                )
            )
        }
    }

    private fun contextId(hendelseId: UUID): UUID {
        return sessionOf(dataSource).use { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT context_id FROM command_context WHERE hendelse_id = ?",
                        hendelseId
                    ).map { UUID.fromString(it.string("context_id")) }.asSingle
                )
            )
        }
    }

    protected fun assertGodkjenningsbehovløsning(
        godkjent: Boolean,
        saksbehandlerIdent: String,
        block: (JsonNode) -> Unit = {}
    ) {
        assertLøsning("Godkjenning") {
            assertTrue(it.path("godkjent").isBoolean)
            assertEquals(godkjent, it.path("godkjent").booleanValue())
            assertEquals(saksbehandlerIdent, it.path("saksbehandlerIdent").textValue())
            assertNotNull(it.path("godkjenttidspunkt").asLocalDateTime())
            block(it)
        }
    }

    protected fun assertVedtaksperiodeAvvist(
        periodetype: String,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
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

    protected fun assertAutomatisertLøsning(godkjent: Boolean = true, block: (JsonNode) -> Unit = {}) {
        assertGodkjenningsbehovløsning(godkjent, "Automatisk behandlet") {
            assertTrue(it.path("automatiskBehandling").booleanValue())
            block(it)
        }
    }

    private fun assertLøsning(behov: String, assertBlock: (JsonNode) -> Unit) {
        testRapid.inspektør.løsning(behov).also(assertBlock)
    }

    protected fun assertBehov(vararg behov: String) {
        assertTrue(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertIkkeEtterspurtBehov(behov: String) {
        assertFalse(testRapid.inspektør.behov().any { it == behov })
    }

    protected fun assertTilstand(hendelseId: UUID, vararg tilstand: String) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE hendelse_id = ? ORDER BY id ASC",
                    hendelseId
                ).map { it.string("tilstand") }.asList
            )
        }.also {
            assertEquals(tilstand.toList(), it)
        }
    }

    protected fun assertAdressebeskyttelse(fnr: String, expected: String) {
        val adressebeskyttelse = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT adressebeskyttelse FROM person_info pi JOIN person ON info_ref = pi.id WHERE fodselsnummer = ?",
                    fnr.toLong()
                ).map { it.string("adressebeskyttelse") }.asSingle
            )
        }
        assertEquals(expected, adressebeskyttelse)
    }

    protected fun assertOppgaver(antall: Int) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(antall, oppgaver.size)
    }

    protected fun assertOppgavestatuser(indeks: Int, vararg status: Oppgavestatus) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(status.toList(), oppgaver[indeks]?.statuser)
    }

    protected fun assertOppgavetype(indeks: Int, type: String) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(type, oppgaver[indeks]?.type)
    }

    private fun TestRapid.RapidInspector.oppgaver(): Map<Int, OppgaveSnapshot> {
        val oppgaveindekser = mutableListOf<Long>()
        val oppgaver = mutableMapOf<Int, MutableList<JsonNode>>()
        hendelser("oppgave_opprettet")
            .forEach {
                oppgaveindekser.add(it.path("oppgaveId").asLong())
                oppgaver[oppgaveindekser.size - 1] = mutableListOf(it)
            }
        hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val indeks = oppgaveindekser.indexOf(oppgave.path("oppgaveId").asLong())
                oppgaver[indeks]?.add(oppgave)
            }
        return oppgaver
            .mapValues { (_, oppgaver) ->
                OppgaveSnapshot(
                    type = oppgaver.first().path("type").asText(),
                    statuser = oppgaver.map { Oppgavestatus.valueOf(it.path("status").asText()) }
                )
            }
    }

    private data class OppgaveSnapshot(
        val statuser: List<Oppgavestatus>,
        val type: String
    )

    protected fun assertIngenOppgave() {
        assertEquals(0, testRapid.inspektør.hendelser("oppgave_opprettet").size)
    }

    protected fun assertSnapshot(forventet: GraphQLClientResponse<HentSnapshot.Result>, vedtaksperiodeId: UUID) {
        assertEquals(forventet.data?.person, sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT data FROM snapshot WHERE id = (SELECT snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> objectMapper.readValue<GraphQLPerson>(row.string("data")) }.asSingle
            )
        })
    }

    protected fun assertWarning(forventet: String, vedtaksperiodeId: UUID) {
        assertTrue(sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId) and (inaktiv_fra is null or inaktiv_fra > now())",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("melding") }.asList
            )
        }.contains(forventet))
    }

    protected fun vedtaksperiode(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kanAutomatiseres: Boolean = false,
        snapshot: GraphQLClientResponse<HentSnapshot.Result> = snapshot(),
        utbetalingId: UUID,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        risikofunn: List<Risikofunn> = emptyList()
    ): UUID {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = utbetalingId,
            fødselsnummer = fødselsnummer,
            periodeFom = periodeFom,
            periodeTom = periodeTom
        )
        sendPersoninfoløsning(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = godkjenningsmeldingId,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            contextId = contextId(godkjenningsmeldingId)
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = kanAutomatiseres,
            contextId = contextId(godkjenningsmeldingId),
            funn = risikofunn
        )
        return godkjenningsmeldingId
    }

    protected fun snapshot(
        versjon: Int = 1,
        fødselsnummer: String = FØDSELSNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UUID.randomUUID(),
        arbeidsgiverbeløp: Int = 30000,
        personbeløp: Int = 0,
        aktivitetslogg: List<GraphQLAktivitet> = emptyList()
    ): GraphQLClientResponse<HentSnapshot.Result> =
        object : GraphQLClientResponse<HentSnapshot.Result> {
            override val data = HentSnapshot.Result(
                GraphQLPerson(
                    aktorId = AKTØR,
                    fodselsnummer = fødselsnummer,
                    versjon = versjon,
                    arbeidsgivere = listOf(
                        GraphQLArbeidsgiver(
                            organisasjonsnummer = ORGNR,
                            ghostPerioder = emptyList(),
                            generasjoner = listOf(
                                GraphQLGenerasjon(
                                    id = UUID.randomUUID().toString(),
                                    perioder = listOf(
                                        GraphQLBeregnetPeriode(
                                            id = UUID.randomUUID().toString(),
                                            vedtaksperiodeId = vedtaksperiodeId.toString(),
                                            utbetaling = GraphQLUtbetaling(
                                                id = utbetalingId.toString(),
                                                arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
                                                arbeidsgiverNettoBelop = arbeidsgiverbeløp,
                                                personFagsystemId = "EN_FAGSYSTEMID",
                                                personNettoBelop = personbeløp,
                                                statusEnum = GraphQLUtbetalingstatus.UBETALT,
                                                typeEnum = no.nav.helse.mediator.graphql.enums.Utbetalingtype.UTBETALING,
                                                vurdering = null,
                                                personoppdrag = null,
                                                arbeidsgiveroppdrag = null
                                            ),
                                            behandlingstype = GraphQLBehandlingstype.BEHANDLET,
                                            erForkastet = false,
                                            fom = "2020-01-01",
                                            tom = "2020-01-31",
                                            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                            opprettet = "2020-01-31",
                                            periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                            tidslinje = emptyList(),
                                            aktivitetslogg = aktivitetslogg,
                                            beregningId = UUID.randomUUID().toString(),
                                            forbrukteSykedager = null,
                                            gjenstaendeSykedager = null,
                                            hendelser = emptyList(),
                                            maksdato = "2021-01-01",
                                            periodevilkar = GraphQLPeriodevilkar(
                                                alder = Alder(
                                                    alderSisteSykedag = 30,
                                                    oppfylt = true,
                                                ),
                                                soknadsfrist = Soknadsfrist(
                                                    sendtNav = "2020-01-31",
                                                    soknadFom = "2020-01-01",
                                                    soknadTom = "2020-01-31",
                                                    oppfylt = true,
                                                ),
                                                sykepengedager = Sykepengedager(
                                                    forbrukteSykedager = null,
                                                    gjenstaendeSykedager = null,
                                                    maksdato = "2021-01-01",
                                                    skjaeringstidspunkt = "2020-01-01",
                                                    oppfylt = true,
                                                )
                                            ),
                                            skjaeringstidspunkt = "2020-01-01",
                                            refusjon = null,
                                            vilkarsgrunnlaghistorikkId = UUID.randomUUID().toString(),
                                            tilstand = GraphQLPeriodetilstand.OPPGAVER,
                                        )
                                    )
                                )
                            ),
                        )
                    ),
                    dodsdato = null,
                    inntektsgrunnlag = emptyList(),
                    vilkarsgrunnlaghistorikk = emptyList(),
                )
            )
        }

//    """{
//  "versjon": $versjon,
//  "aktørId": "$AKTØR",
//  "fødselsnummer": "$fødselsnummer",
//  "arbeidsgivere": [
//    {
//      "organisasjonsnummer": "$ORGNR",
//      "id": "${UUID.randomUUID()}",
//      "vedtaksperioder": [
//        {
//          "id": "$vedtaksperiodeId",
//          "aktivitetslogg": [],
//          "utbetaling": {
//            "utbetalingId": "$utbetalingId",
//            "utbetalingstidslinje": ${utbetalingstidslinje.map
//    {
//        (dato, personbeløp, arbeidsgiverbeløp) ->
//        """
//                       {
//                            "dato": "$dato",
//                            "arbeidsgiverbeløp": $arbeidsgiverbeløp,
//                            "personbeløp": $personbeløp
//                        }
//                        """
//    } },
//            "personOppdrag": {
//              "utbetalingslinjer": ${personOppdragLinjer.map
//    {
//        """
//                            {
//                                "fom": "${it.start}",
//                                "tom": "${it.endInclusive}"
//                             }
//                            """
//    }}
//            },
//            "arbeidsgiverOppdrag": {
//              "utbetalingslinjer": ${arbeidsgiverOppdragLinjer.map
//    {
//        """
//                            {
//                                "fom": "${it.start}",
//                                "tom": "${it.endInclusive}"
//                             }
//                            """
//    }}
//            }
//          }
//        }
//      ],
//      "utbetalingshistorikk": []
//    }
//  ],
//  "inntektsgrunnlag": {}
//}"""

    protected fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    protected fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.siste(type: String) =
        hendelser(type).last()

    protected fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    protected fun TestRapid.RapidInspector.løsning(behov: String): JsonNode =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    protected fun TestRapid.RapidInspector.contextId(): UUID =
        (hendelser("behov")
            .lastOrNull { it.hasNonNull("contextId") }
            ?: error("Prøver å finne contextId fra siste behov, men ingen behov er sendt ut"))
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()

    protected fun TestRapid.RapidInspector.contextId(hendelseId: UUID): UUID =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asText() == hendelseId.toString() }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID): String =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asText()
}
