package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.contextId
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.VARSEL_KODE_1
import no.nav.helse.Testdata.VARSEL_KODE_2
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.SubsumsjonJson
import no.nav.helse.mediator.meldinger.TestmeldingfabrikkUtenFnr
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.*
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals

internal object Meldingssender {
    lateinit var testRapid: TestRapid

    fun sendSøknadSendt(aktørId: String, fødselsnummer: String, organisasjonsnummer: String): UUID = uuid.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagSøknadSendt(
                id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
            )
        )
    }

    fun sendVedtaksperiodeEndret(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND",
        forårsaketAvId: UUID = UUID.randomUUID(),
    ): UUID = uuid.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeEndret(
                id,
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                forrigeTilstand,
                gjeldendeTilstand,
                forårsaketAvId,
            )
        )
    }

    fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
        }

    fun sendAktivitetsloggNyAktivitet(
        orgnr: String = "orgnr",
        vedtaksperiodeId: UUID,
        koder: List<String> = listOf("RV_VV")
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikk.lagNyeVarsler(id, vedtaksperiodeId, orgnr, koder.map {
                meldingsfabrikk.lagAktivitet(
                    kode = it,
                    orgnummer = orgnr,
                    vedtaksperiodeId = vedtaksperiodeId
                )
            }))
        }

    fun sendVarseldefinisjonerEndret(
        definisjoner: List<Map<String, Any>> =
            listOf(
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(kode = VARSEL_KODE_1),
                meldingsfabrikkUtenFnr.lagVarseldefinisjon(kode = VARSEL_KODE_2)
            )
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikkUtenFnr.lagVarseldefinisjonerEndret(id, definisjoner))
        }

    fun sendGodkjenningsbehov(
        aktørId: String = AKTØR,
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = LocalDate.now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = true,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        utbetalingtype: Utbetalingtype = UTBETALING
    ): UUID = uuid.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                orgnummer = organisasjonsnummer,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                skjæringstidspunkt = skjæringstidspunkt,
                periodetype = periodetype,
                førstegangsbehandling = førstegangsbehandling,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                inntektskilde = inntektskilde,
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
        fraMånedligInntekt: Double = 25001.0,
        skjæringstidspunkt: LocalDate,
        forklaring: String = "testbortforklaring",
        subsumsjon: SubsumsjonJson?
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringInntekt(
                    id = id,
                    organisasjonsnummer = orgnr,
                    månedligInntekt = månedligInntekt,
                    fraMånedligInntekt = fraMånedligInntekt,
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandlerEpost = Testdata.SAKSBEHANDLER_EPOST,
                    forklaring = forklaring,
                    subsumsjon = subsumsjon
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

    fun sendArbeidsgiverinformasjonløsningOld(
        hendelseId: UUID,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        navn: String = "En arbeidsgiver",
        bransjer: List<String> = listOf("En bransje", "En annen bransje"),
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList()
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsning(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    navn = navn,
                    bransjer = bransjer,
                    ekstraArbeidsgivere = ekstraArbeidsgivere,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }

    fun sendPersoninfoløsningComposite(
        hendelseId: UUID,
        orgnr: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        enhet: String = "0301",
        adressebeskyttelse: String = "Ugradert"
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagPersoninfoløsningComposite(
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

    fun sendPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID
    ): UUID =
        uuid.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            testRapid.reset()
            assertEquals("HentPersoninfoV2", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            testRapid.sendTestMessage(
                meldingsfabrikk.lagPersoninfoløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }
    fun sendEnhetløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID
    ): UUID =
        uuid.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            testRapid.reset()
            assertEquals("HentEnhet", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            testRapid.sendTestMessage(
                meldingsfabrikk.lagEnhetløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }
    fun sendInfotrygdutbetalingerløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID
    ): UUID =
        uuid.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            testRapid.reset()
            assertEquals("HentInfotrygdutbetalinger", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            testRapid.sendTestMessage(
                meldingsfabrikk.lagInfotrygdutbetalingerløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }

    fun sendArbeidsgiverinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        navn: String = "En arbeidsgiver",
        bransjer: List<String> = listOf("En bransje", "En annen bransje"),
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList(),
    ): UUID =
        uuid.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            testRapid.reset()
            assertEquals("Arbeidsgiverinformasjon", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    navn = navn,
                    bransjer = bransjer,
                    ekstraArbeidsgivere = ekstraArbeidsgivere,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
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

    fun sendVedtakFattet(
        fødselsnummer: String = FØDSELSNUMMER,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
    ) {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtakFattet(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId
            )
        )
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

    private val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)
    private val meldingsfabrikkUtenFnr get() = TestmeldingfabrikkUtenFnr()

    private val uuid get() = UUID.randomUUID()

}
