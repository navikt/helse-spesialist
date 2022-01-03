import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.*
import no.nav.helse.abonnement.AbonnementDao
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.FeilendeMeldingerDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.PersonMediator
import no.nav.helse.mediator.api.graphql.SpleisGraphQLClient
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.notat.NotatDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.person.PersonApiDao
import no.nav.helse.person.PersonsnapshotDao
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.vedtaksperiode.VarselDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.abonnement.OpptegnelseDao as OpptegnelseApiDao

internal abstract class AbstractE2ETest : AbstractDatabaseTest() {
    protected val VEDTAKSPERIODE_ID = UUID.randomUUID()
    protected val FØDSELSNUMMER = "12020052345"
    protected val AKTØR = "999999999"
    protected val ORGNR = "222222222"

    protected val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    protected val SAKSBEHANDLER_OID = UUID.randomUUID()
    protected val SAKSBEHANDLER_IDENT = "X999999"
    protected val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"
    protected val SAKSBEHANDLERTILGANGER_UTEN_TILGANGER =
        SaksbehandlerTilganger(
            gruppetilganger = emptyList(),
            kode7Saksbehandlergruppe = UUID.randomUUID(),
            riskSaksbehandlergruppe = UUID.randomUUID()
        )

    protected val SNAPSHOTV1_MED_WARNINGS =
        snapshotMedWarning(vedtaksperiodeId = VEDTAKSPERIODE_ID, orgnr = ORGNR, fnr = FØDSELSNUMMER, aktørId = AKTØR)
    protected val SNAPSHOTV1_UTEN_WARNINGS =
        snapshotUtenWarnings(vedtaksperiodeId = VEDTAKSPERIODE_ID, orgnr = ORGNR, fnr = FØDSELSNUMMER, aktørId = AKTØR)

    protected fun snapshotv1MedWarnings(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        orgnr: String = ORGNR,
        fnr: String = FØDSELSNUMMER,
        aktørId: String = AKTØR
    ) = snapshotMedWarning(vedtaksperiodeId, orgnr, fnr, aktørId)

    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val UTBETALING_ID = UUID.randomUUID()
        internal val UTBETALING_ID2 = UUID.randomUUID()
    }

    protected val oppgaveDao = OppgaveDao(dataSource)
    protected val personDao = PersonDao(dataSource)
    protected val vedtakDao = VedtakDao(dataSource)
    protected val warningDao = WarningDao(dataSource)
    protected val commandContextDao = CommandContextDao(dataSource)
    protected val tildelingDao = TildelingDao(dataSource)
    protected val risikovurderingDao = RisikovurderingDao(dataSource)
    protected val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    protected val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    protected val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val automatiseringDao = AutomatiseringDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    protected val overstyringDao = OverstyringDao(dataSource)
    protected val overstyringApiDao = OverstyringApiDao(dataSource)
    protected val speilSnapshotDao = SpeilSnapshotDao(dataSource)
    protected val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    protected val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    protected val egenAnsattDao = EgenAnsattDao(dataSource)
    protected val utbetalingDao = UtbetalingDao(dataSource)
    private val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    protected val opptegnelseDao = OpptegnelseDao(dataSource)
    protected val opptegnelseApiDao = OpptegnelseApiDao(dataSource)
    protected val abonnementDao = AbonnementDao(dataSource)
    protected val saksbehandlerDao = SaksbehandlerDao(dataSource)
    protected val reservasjonDao = ReservasjonDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val varselDao = VarselDao(dataSource)
    private val personsnapshotDao = PersonsnapshotDao(dataSource)
    private val feilendeMeldingerDao = FeilendeMeldingerDao(dataSource)
    protected val notatDao = NotatDao(dataSource)
    protected val snapshotDao = SnapshotDao(dataSource)

    protected val speilSnapshotRestClient = mockk<SpeilSnapshotRestClient>()

    protected val testRapid = TestRapid()

    protected val meldingsfabrikk = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)

    protected val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    protected val graphqlClient = mockk<SpleisGraphQLClient>(relaxed = true)

    protected val oppgaveMediator = OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao)
    protected val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = ArbeidsgiverDao(dataSource),
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        oppgaveDao = oppgaveDao,
        commandContextDao = commandContextDao,
        speilSnapshotDao = SpeilSnapshotDao(dataSource),
        reservasjonDao = reservasjonDao,
        tildelingDao = tildelingDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = OverstyringDao(dataSource),
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        snapshotDao = snapshotDao,
        speilSnapshotRestClient = restClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao),
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = automatiseringDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vedtakDao = vedtakDao
        ) { false },
        arbeidsforholdDao = arbeidsforholdDao,
        utbetalingDao = utbetalingDao,
        opptegnelseDao = opptegnelseDao,
        spleisGraphQLClient = graphqlClient
    )
    internal val hendelseMediator = HendelseMediator(
        rapidsConnection = testRapid,
        dataSource = dataSource,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk,
    )
    internal val personMediator = PersonMediator(
        personsnapshotDao = personsnapshotDao,
        varselDao = varselDao,
        personDao = personApiDao,
        arbeidsgiverDao = arbeidsgiverApiDao,
        overstyringDao = overstyringApiDao,
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        risikovurderingApiDao = risikovurderingApiDao,
        utbetalingDao = utbetalingDao,
        speilSnapshotDao = speilSnapshotDao,
        speilSnapshotRestClient = speilSnapshotRestClient
    )

    @BeforeEach
    internal fun resetTestSetup() {
        clearMocks(restClient)
        clearMocks(speilSnapshotRestClient)
        testRapid.reset()
    }

    private fun nyHendelseId() = UUID.randomUUID()

    protected fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendVedtaksperiodeEndret(orgnr: String, vedtaksperiodeId: UUID): UUID = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(id, vedtaksperiodeId, orgnr))
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
        aktiveVedtaksperioder: List<Testmeldingfabrikk.AktivVedtaksperiodeJson> = listOf(
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                orgnr,
                vedtaksperiodeId,
                periodetype
            )
        ),
        orgnummereMedAktiveArbeidsforhold: List<String> = emptyList(),
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING
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
                orgnummereMedAktiveArbeidsforhold = orgnummereMedAktiveArbeidsforhold,
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
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
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

    protected fun sendRevurderingAvvist(fødselsnummer: String, errors: List<String>) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRevurderingAvvist(
                    id = id,
                    fødselsnummer = fødselsnummer,
                    errors = errors
                )
            )
        }

    protected fun sendDigitalKontaktinformasjonløsning(
        godkjenningsmeldingId: UUID,
        erDigital: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId()
    ) {
        nyHendelseId().also { id ->
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
    ) {
        nyHendelseId().also { id ->
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

    protected fun sendAvbrytSaksbehandling(fødselsnummer: String, vedtaksperiodeId: UUID) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagAvbrytSaksbehandling(id, fødselsnummer, vedtaksperiodeId)
            )
        }
    }

    protected fun sendRisikovurderingløsning(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId(),
        funn: JsonNode = objectMapper.createArrayNode()
    ) {
        nyHendelseId().also { id ->
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
        contextId: UUID = testRapid.inspektør.contextId()
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagEgenAnsattløsning(
                    id,
                    godkjenningsmeldingId,
                    contextId,
                    erEgenAnsatt
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

    protected fun settOppBruker(): UUID {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns SNAPSHOTV1_MED_WARNINGS
        val godkjenningsbehovId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            UTBETALING_ID,
            1.januar,
            31.januar,
        )
        sendPersoninfoløsning(godkjenningsbehovId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsbehovId,
            orgnummer = ORGNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
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
            assertEquals(kommentar, it.path("kommentar")?.asText())
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

    protected fun assertSnapshot(forventet: String, vedtaksperiodeId: UUID) {
        assertEquals(forventet, sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("data") }.asSingle
            )
        })
    }

    protected fun assertWarning(forventet: String, vedtaksperiodeId: UUID) {
        assertTrue(sessionOf(dataSource).use {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { row -> row.string("melding") }.asList
            )
        }.contains(forventet))
    }

    protected fun vedtaksperiode(
        organisasjonsnummer: String = ORGNR,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kanAutomatiseres: Boolean = false,
        snapshot: String = snapshot(),
        utbetalingId: UUID
    ): UUID {
        every { restClient.hentSpeilSnapshot(FØDSELSNUMMER) } returns snapshot
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            orgnr = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetype = Periodetype.FORLENGELSE,
            utbetalingId = utbetalingId,
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
            contextId = contextId(godkjenningsmeldingId)
        )
        return godkjenningsmeldingId
    }

    @Language("JSON")
    protected fun snapshot(versjon: Int = 1) = """{
      "versjon": $versjon,
      "aktørId": "$AKTØR",
      "fødselsnummer": "$FØDSELSNUMMER",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "$ORGNR",
          "id": "${UUID.randomUUID()}",
          "vedtaksperioder": [
            {
              "id": "${UUID.randomUUID()}",
              "aktivitetslogg": []
            }
          ],
          "utbetalingshistorikk": []
        }
      ],
      "inntektsgrunnlag": {}
      }"""

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
            .lastOrNull { it.hasNonNull("contextId") } ?: error("Prøver å finne contextId fra siste behov, men ingen behov er sendt ut"))
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
