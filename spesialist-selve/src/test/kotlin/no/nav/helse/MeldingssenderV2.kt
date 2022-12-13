package no.nav.helse

import TestmeldingsfabrikkV2
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.SubsumsjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.overstyring.Dagtype.Feriedag
import no.nav.helse.spesialist.api.overstyring.Dagtype.Sykedag
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals

internal class MeldingssenderV2(private val testRapid: TestRapid) {
    private val meldingsfabrikk = TestmeldingsfabrikkV2
    private val newUUID get() = UUID.randomUUID()

    fun sendSøknadSendt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagSøknadSendt(
                organisasjonsnummer = organisasjonsnummer,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                id,
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
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeEndret(
                id = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                forrigeTilstand = forrigeTilstand,
                gjeldendeTilstand = gjeldendeTilstand,
                forårsaketAvId = forårsaketAvId,
            )
        )
    }

    fun sendVedtaksperiodeNyUtbetaling(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtaksperiodeNyUtbetaling(
                id = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId
            )
        )
    }

    fun sendAktivitetsloggNyAktivitet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        varselkoder: List<String> = emptyList(),
    ): UUID =
        newUUID.also { id ->
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

    fun sendVedtaksperiodeForkastet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID =
        newUUID.also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagVedtaksperiodeForkastet(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    id = id
                )
            )
        }

    fun sendGosysOppgaveEndret(
        aktørId: String,
        fødselsnummer: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGosysOppgaveEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                id = id,
            )
        )
    }

    fun sendEndretSkjermetinfo(
        fødselsnummer: String,
        skjermet: Boolean,
    ) {
        @Language("JSON")
        val json = """{
          "@event_name": "endret_skjermetinfo",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "$fødselsnummer",
          "skjermet": "$skjermet",
          "@opprettet": "${LocalDateTime.now()}"
        }"""
        testRapid.sendTestMessage(json)
    }

    fun sendUtbetalingEndret(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        type: String = "UTBETALING",
        forrigeStatus: Utbetalingsstatus = NY,
        gjeldendeStatus: Utbetalingsstatus = IKKE_UTBETALT,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                forrigeStatus = forrigeStatus,
                gjeldendeStatus = gjeldendeStatus,
                type = type,
                id = id
            )
        )
    }

    fun sendUtbetalingAnnullert(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        epost: String,
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingAnnullert(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                epost = epost,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                id = id
            )
        )
    }

    fun sendGodkjenningsbehov(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        skjæringstidspunkt: LocalDate = LocalDate.now(),
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = true,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        utbetalingtype: Utbetalingtype = UTBETALING,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                organisasjonsnummer = organisasjonsnummer,
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

    fun sendPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID = newUUID.also { id ->
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
        vedtaksperiodeId: UUID,
    ): UUID = newUUID.also { id ->
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
        vedtaksperiodeId: UUID,
    ): UUID = newUUID.also { id ->
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
    ): UUID =
        newUUID.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            testRapid.reset()
            assertEquals("Arbeidsgiverinformasjon", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            val arbeidsgivere = behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                val arbeidsgivernavn = listOf(
                    "En arbeidsgiver",
                    "En annen arbeidsgiver",
                    "En tredje arbeidsgiver",
                    "En fjerde arbeidsgiver"
                )
                val arbeidsgiverbransjer =
                    listOf("En bransje", "En annen bransje", "En tredje bransje", "En fjerde bransje")
                ArbeidsgiverinformasjonJson(
                    it.asText(),
                    arbeidsgivernavn.random(),
                    listOf(arbeidsgiverbransjer.random())
                )
            }

            testRapid.sendTestMessage(
                meldingsfabrikk.lagArbeidsgiverinformasjonløsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    ekstraArbeidsgivere = arbeidsgivere,
                    id = id,
                    hendelseId = hendelseId,
                    contextId = contextId
                )
            )
        }

    fun sendArbeidsforholdløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        løsning: List<Arbeidsforholdløsning.Løsning> = listOf(
            Arbeidsforholdløsning.Løsning(
                stillingstittel = "en-stillingstittel",
                stillingsprosent = 100,
                startdato = LocalDate.now(),
                sluttdato = null
            )
        ),
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("Arbeidsforhold", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagArbeidsforholdløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                løsning = løsning,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendEgenAnsattløsning(
        aktørId: String,
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("EgenAnsatt", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagEgenAnsattløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendVergemålløsning(
        aktørId: String,
        fødselsnummer: String,
        fullmakter: List<Fullmakt> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("Vergemål", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        val vergemål = Testmeldingfabrikk.VergemålJson(emptyList(), emptyList(), fullmakter)

        testRapid.sendTestMessage(
            meldingsfabrikk.lagVergemålløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vergemål = vergemål,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendDigitalKontaktinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        erDigital: Boolean = true,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("DigitalKontaktinformasjon", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagDigitalKontaktinformasjonløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                erDigital = erDigital,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendInntektløsning(
        aktørId: String,
        fødselsnummer: String,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("InntekterForSykepengegrunnlag", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagInntektløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendÅpneGosysOppgaverløsning(
        aktørId: String,
        fødselsnummer: String,
        antall: Int,
        oppslagFeilet: Boolean,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("ÅpneOppgaver", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagÅpneGosysOppgaverløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendRisikovurderingløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: List<Risikofunn> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        testRapid.reset()
        assertEquals("Risikovurdering", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            meldingsfabrikk.lagRisikovurderingløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                funn = funn,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendSaksbehandlerløsning(
        fødselsnummer: String,
        oppgaveId: Long,
        godkjenningsbehovId: UUID,
        godkjent: Boolean,
    ): UUID {
        return newUUID.also { id ->
            testRapid.reset()
            testRapid.sendTestMessage(
                meldingsfabrikk.lagSaksbehandlerløsning(
                    fødselsnummer = fødselsnummer,
                    godkjent = godkjent,
                    id = id,
                    oppgaveId = oppgaveId,
                    hendelseId = godkjenningsbehovId,
                )
            )
        }
    }

    fun sendVedtakFattet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVedtakFattet(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                id = id
            )
        )
    }

    fun sendOverstyrTidslinje(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        dager: List<OverstyringDagDto> = listOf(
            OverstyringDagDto(1.januar(1970), Feriedag, Sykedag, null, 100)
        ),
        saksbehandlerident: String = Testdata.SAKSBEHANDLER_IDENT,
        saksbehandlerepost: String = Testdata.SAKSBEHANDLER_EPOST,
        saksbehandleroid: UUID = Testdata.SAKSBEHANDLER_OID,
    ): UUID = newUUID.also { id ->
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

    fun sendOverstyrtArbeidsforhold(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt> = listOf(
            ArbeidsforholdOverstyrt(organisasjonsnummer, true, "begrunnelse", "forklaring")
        ),
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagOverstyringArbeidsforhold(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                id = id
            )
        )
    }

    fun sendOverstyrtInntekt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        månedligInntekt: Double = 25000.0,
        fraMånedligInntekt: Double = 25001.0,
        skjæringstidspunkt: LocalDate = 1.januar(1970),
        forklaring: String = "testbortforklaring",
        subsumsjon: SubsumsjonJson? = SubsumsjonJson("8-28", "LEDD_1", "BOKSTAV_A"),
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagOverstyringInntekt(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                månedligInntekt = månedligInntekt,
                fraMånedligInntekt = fraMånedligInntekt,
                skjæringstidspunkt = skjæringstidspunkt,
                forklaring = forklaring,
                subsumsjon = subsumsjon,
                saksbehandlerepost = Testdata.SAKSBEHANDLER_EPOST,
                id = id
            )
        )
    }
}
