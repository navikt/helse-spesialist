package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.contextId
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.SAKSBEHANDLER_IDENT
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.Testdata.VARSEL_KODE_1
import no.nav.helse.Testdata.VARSEL_KODE_2
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.api.Arbeidsgiver
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt
import no.nav.helse.mediator.api.SubsumsjonDto
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.SubsumsjonJson
import no.nav.helse.mediator.meldinger.TestmeldingfabrikkUtenFnr
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.*
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.overstyring.Dagtype.*
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import org.intellij.lang.annotations.Language

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

    fun sendOverstyringIgangsatt(
        aktørId: String,
        fødselsnummer: String,
        berørtePerioder: List<Map<String, String>> = listOf(mapOf(
            "vedtaksperiodeId" to "$VEDTAKSPERIODE_ID",
            "skjæringstidspunkt" to "2022-01-01",
            "periodeFom" to "2022-01-01",
            "periodeTom" to "2022-01-31",
            "orgnummer" to ORGNR,
            "typeEndring" to "REVURDERING"
        )),
        kilde: UUID = UUID.randomUUID(),
    ): UUID = uuid.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagOverstyringIgangsatt(
                id = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                berørtePerioder = berørtePerioder,
                kilde = kilde,
            )
        )
    }

    fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
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
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt> = listOf(
            ArbeidsforholdOverstyrt(organisasjonsnummer, true, "begrunnelse", "forklaring")
        ),
    ): UUID =
        uuid.also {
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringArbeidsforhold(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    overstyrteArbeidsforhold = overstyrteArbeidsforhold
                )
            )
        }

    fun sendOverstyrtInntektOgRefusjon(
        aktørId: String,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        arbeidsgivere: List<Arbeidsgiver> = listOf(
            Arbeidsgiver(
                organisasjonsnummer = ORGNR,
                månedligInntekt = 25000.0,
                fraMånedligInntekt = 25001.0,
                forklaring = "testbortforklaring",
                subsumsjon = SubsumsjonDto("8-28", "LEDD_1", "BOKSTAV_A"),
                refusjonsopplysninger = null,
                fraRefusjonsopplysninger = null,
                begrunnelse = "en begrunnelse")
        )
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringInntektOgRefusjon(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    arbeidsgivere = arbeidsgivere,
                    skjæringstidspunkt = skjæringstidspunkt,
                    saksbehandlerepost = SAKSBEHANDLER_EPOST,
                    id = id
                )
            )
        }

    fun sendUtbetalingEndret(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        type: String,
        status: Utbetalingsstatus = IKKE_UTBETALT,
        forrigeStatus: Utbetalingsstatus = NY,
        arbeidsgiverFagsystemId: String = "LWCBIQLHLJISGREBICOHAU",
        personFagsystemId: String = "ASJKLD90283JKLHAS3JKLF",
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) {
        uuid.also { id ->
            @Language("JSON")
            val json = """{
                "@event_name": "utbetaling_endret",
                "@id": "$id",
                "@opprettet": "$opprettet",
                "utbetalingId": "$utbetalingId",
                "aktørId":"$aktørId",
                "fødselsnummer": "$fødselsnummer",
                "organisasjonsnummer": "$organisasjonsnummer",
                "type": "$type",
                "forrigeStatus": "$forrigeStatus",
                "gjeldendeStatus": "$status",
                "arbeidsgiverOppdrag": {
                    "mottaker": "$organisasjonsnummer",
                    "fagområde": "SPREF",
                    "endringskode": "NY",
                    "nettoBeløp": 20000,
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
                    "mottaker": "$fødselsnummer",
                    "fagområde": "SP",
                    "endringskode": "NY",
                    "nettoBeløp": 0,
                    "fagsystemId": "$personFagsystemId",
                    "linjer": []
                }
            }"""

            testRapid.sendTestMessage(json)
        }
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
      "nettoBeløp": 20000,
      "fagsystemId": "$arbeidsgiverFagsystemId",
      "sisteArbeidsgiverdag": "${LocalDate.MIN}",
      "linjer": []
    },
    "personOppdrag": {
      "mottaker": "$FØDSELSNUMMER",
      "fagområde": "SP",
      "endringskode": "NY",
      "nettoBeløp": 20000,
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

    fun sendArbeidsforholdløsningOld(
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
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    organisasjonsnummer = orgnr,
                    vedtaksperiodeId = vedtaksperiodeId,
                    løsning = løsning,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
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
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson> = emptyList()
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsningOld(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    ekstraArbeidsgivere = ekstraArbeidsgivere,
                    navn = navn,
                    bransjer = bransjer,
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

    fun sendOverstyrTidslinje(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        dager: List<OverstyringDagDto> = listOf(OverstyringDagDto(1.januar(1970), Feriedag, Sykedag, null, 100)),
        saksbehandlerident: String = SAKSBEHANDLER_IDENT,
        saksbehandlerepost: String = SAKSBEHANDLER_EPOST,
        saksbehandleroid: UUID = SAKSBEHANDLER_OID
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyringTidslinje(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    dager = dager,
                    saksbehandleroid = saksbehandleroid,
                    saksbehandlerepost = saksbehandlerepost,
                    saksbehandlerident = saksbehandlerident,
                    id = id,
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

    fun sendDigitalKontaktinformasjonløsningOld(
        godkjenningsmeldingId: UUID,
        erDigital: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagDigitalKontaktinformasjonløsning(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    erDigital = erDigital,
                    id = id,
                    hendelseId = godkjenningsmeldingId,
                    contextId = contextId
                )
            )
        }
    }

    fun sendInntektløsningOld(
        godkjenningsmeldingId: UUID,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagInntektløsning(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    orgnummer = ORGNR,
                    vedtaksperiodeId = VEDTAKSPERIODE_ID,
                    id = id,
                    hendelseId = godkjenningsmeldingId,
                    contextId = contextId
                )
            )
        }
    }

    fun sendÅpneGosysOppgaverløsningOld(
        godkjenningsmeldingId: UUID,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagÅpneGosysOppgaverløsning(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    antall = antall,
                    oppslagFeilet = oppslagFeilet,
                    id = id,
                    hendelseId = godkjenningsmeldingId,
                    contextId = contextId
                )
            )
        }
    }

    fun sendRisikovurderingløsningOld(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        contextId: UUID = testRapid.inspektør.contextId(),
        funn: List<Risikofunn> = emptyList()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRisikovurderingløsning(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    vedtaksperiodeId = vedtaksperiodeId,
                    kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                    funn = funn,
                    id = id,
                    hendelseId = godkjenningsmeldingId,
                    contextId = contextId
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

    fun sendEgenAnsattløsningOld(
        godkjenningsmeldingId: UUID,
        erEgenAnsatt: Boolean,
        fødselsnummer: String = FØDSELSNUMMER,
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagEgenAnsattløsning(
                    AKTØR,
                    fødselsnummer,
                    erEgenAnsatt,
                    id,
                    godkjenningsmeldingId,
                    contextId,
                )
            )
        }
    }

    fun sendVergemålløsningOld(
        godkjenningsmeldingId: UUID,
        vergemål: Testmeldingfabrikk.VergemålJson = Testmeldingfabrikk.VergemålJson(),
        contextId: UUID = testRapid.inspektør.contextId()
    ): UUID {
        return uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagVergemålløsning(
                    AKTØR,
                    FØDSELSNUMMER,
                    vergemål,
                    id,
                    godkjenningsmeldingId,
                    contextId
                )
            )
        }
    }

    private val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)
    private val meldingsfabrikkUtenFnr get() = TestmeldingfabrikkUtenFnr()

    private val uuid get() = UUID.randomUUID()

}
