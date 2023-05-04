package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.contextId
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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

    fun sendVedtaksperiodeOpprettet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        forårsaketAvId: UUID = UUID.randomUUID(),
        skjæringstidspunkt: LocalDate,
        fom: LocalDate,
        tom: LocalDate,
    ): UUID = uuid.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeOpprettet(
                id,
                aktørId,
                fødselsnummer,
                organisasjonsnummer,
                vedtaksperiodeId,
                forårsaketAvId,
                fom,
                tom,
                skjæringstidspunkt
            )
        )
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

    fun sendArbeidsgiverinformasjonløsningOld(
        hendelseId: UUID,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson> = emptyList()
    ): UUID =
        uuid.also { id ->
            val behov = testRapid.inspektør.siste("behov")

            val arbeidsgivere = ekstraArbeidsgivere.ifEmpty {
                behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                    ArbeidsgiverinformasjonJson(
                        it.asText(),
                        "Navn for ${it.asText()}",
                        listOf("Bransje for ${it.asText()}")
                    )
                }
            }

            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsningOld(
                    aktørId = AKTØR,
                    fødselsnummer = FØDSELSNUMMER,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    ekstraArbeidsgivere = arbeidsgivere,
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

    fun sendVedtaksperiodeNyUtbetaling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String
    ): UUID {
        return uuid.also {
            testRapid.sendTestMessage(
                meldingsfabrikk.lagVedtaksperiodeNyUtbetaling(vedtaksperiodeId, utbetalingId, organisasjonsnummer)
            )
        }
    }

    fun sendAktivitetsloggNyAktivitet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        varselkoder: List<String> = emptyList(),
    ): UUID =
        uuid.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagAktivitetsloggNyAktivitet(
                    id,
                    aktørId,
                    fødselsnummer,
                    organisasjonsnummer,
                    vedtaksperiodeId,
                    varselkoder
                )
            )
        }

    private val meldingsfabrikk get() = Testmeldingfabrikk(FØDSELSNUMMER, AKTØR)

    private val uuid get() = UUID.randomUUID()

}
