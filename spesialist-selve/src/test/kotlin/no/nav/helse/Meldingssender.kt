package no.nav.helse

import no.nav.helse.TestRapidHelpers.contextId
import no.nav.helse.Testdata.FØDSELSNUMMER
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language

internal object Meldingssender {
    lateinit var testRapid: TestRapid

    fun sendVedtaksperiodeEndret(
        orgnr: String = "orgnr",
        vedtaksperiodeId: UUID,
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND"
    ): UUID = uuid.also { id ->
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

    fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
        }

    fun sendGodkjenningsbehov(
        orgnr: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = LocalDate.now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = Testdata.AKTØR,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        aktiveVedtaksperioder: List<Testmeldingfabrikk.AktivVedtaksperiodeJson> = listOf(
            Testmeldingfabrikk.AktivVedtaksperiodeJson(
                orgnr,
                vedtaksperiodeId,
                periodetype
            )
        ),
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING
    ): UUID = uuid.also { id ->
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

    fun sendAdressebeskyttelseEndret(): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikk.lagAdressebeskyttelseEndret(id))
        }


    fun sendOverstyrtArbeidsforhold(
        skjæringstidspunkt: LocalDate,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
    ): UUID =
        uuid.also {
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringArbeidsforhold(
                    organisasjonsnummer = Testdata.ORGNR,
                    skjæringstidspunkt = skjæringstidspunkt,
                    overstyrteArbeidsforhold = overstyrteArbeidsforhold
                )
            )
        }

    fun sendOverstyrtInntekt(
        orgnr: String = Testdata.ORGNR,
        månedligInntekt: Double = 25000.0,
        skjæringstidspunkt: LocalDate,
        forklaring: String = "testbortforklaring",
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringInntekt(
                    id = id,
                    organisasjonsnummer = orgnr,
                    månedligInntekt = månedligInntekt,
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandlerEpost = Testdata.SAKSBEHANDLER_EPOST,
                    forklaring = forklaring
                )
            )
        }

    fun sendUtbetalingEndret(
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

    fun sendPersonUtbetalingEndret(
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

    fun sendUtbetalingAnnullert(
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
                "utbetalingId": "${Testdata.UTBETALING_ID}",
                "tidspunkt": "${LocalDateTime.now()}",
                "epost": "$saksbehandlerEpost"
            }"""

        testRapid.sendTestMessage(json)
    }

    fun sendArbeidsforholdløsning(
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
        uuid.also { id ->
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

    fun sendHentPersoninfoLøsning(
        hendelseId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        adressebeskyttelse: String = "Ugradert"
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagHentPersoninfoløsning(
                    id,
                    hendelseId,
                    contextId,
                    adressebeskyttelse
                )
            )
        }

    fun sendKomposittbehov(
        hendelseId: UUID,
        behov: List<String>,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String = "orgnr",
        contextId: UUID = testRapid.inspektør.contextId(),
        detaljer: Map<String, Any>
    ): UUID = uuid.also { id ->
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

    fun sendArbeidsgiverinformasjonløsning(
        hendelseId: UUID,
        orgnummer: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        navn: String = "En arbeidsgiver",
        bransjer: List<String> = listOf("En bransje", "En annen bransje"),
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ): UUID =
        uuid.also { id ->
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

    fun sendPersoninfoløsning(
        hendelseId: UUID,
        orgnr: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        enhet: String = "0301",
        adressebeskyttelse: String = "Ugradert"
    ): UUID =
        uuid.also { id ->
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

    fun sendOverstyrteDager(
        dager: List<OverstyringDagDto>,
        orgnr: String = Testdata.ORGNR,
        saksbehandlerEpost: String = Testdata.SAKSBEHANDLER_EPOST,
        saksbehandlerOid: UUID = Testdata.SAKSBEHANDLER_OID,
        saksbehandlerIdent: String = Testdata.SAKSBEHANDLER_IDENT
    ): UUID =
        uuid.also { id ->
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

    fun sendRevurderingAvvist(fødselsnummer: String, errors: List<String>): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRevurderingAvvist(
                    id = id,
                    fødselsnummer = fødselsnummer,
                    errors = errors
                )
            )
        }

    fun sendDigitalKontaktinformasjonløsning(
        godkjenningsmeldingId: UUID,
        erDigital: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
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

    fun sendÅpneGosysOppgaverløsning(
        godkjenningsmeldingId: UUID,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
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

    fun sendRisikovurderingløsning(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId(),
        funn: List<Risikofunn> = emptyList()
    ): UUID {
        return uuid.also { id ->
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

    fun sendEgenAnsattløsning(
        godkjenningsmeldingId: UUID,
        erEgenAnsatt: Boolean,
        fødselsnummer: String = FØDSELSNUMMER,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
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

    fun sendVergemålløsning(
        godkjenningsmeldingId: UUID,
        vergemål: Testmeldingfabrikk.VergemålJson = Testmeldingfabrikk.VergemålJson(),
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
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

    private val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, Testdata.AKTØR)

    private val uuid get() = UUID.randomUUID()

}
