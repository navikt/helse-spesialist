package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.ArbeidsgiverinformasjonJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Fullmakt
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk.VergemålJson.Vergemål
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Meldingssender(private val testRapid: TestRapid) {
    private val newUUID get() = UUID.randomUUID()

    fun sendSøknadSendt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagSøknadSendt(
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
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
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

    fun sendBehandlingOpprettet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        spleisBehandlingId: UUID,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                id = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                fom = fom,
                tom = tom,
                spleisBehandlingId = spleisBehandlingId
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
            Testmeldingfabrikk.lagVedtaksperiodeNyUtbetaling(
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
                Testmeldingfabrikk.lagAktivitetsloggNyAktivitet(
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
                Testmeldingfabrikk.lagVedtaksperiodeForkastet(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    id = id,
                )
            )
        }

    fun sendGosysOppgaveEndret(
        fødselsnummer: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGosysOppgaveEndret(
                fødselsnummer = fødselsnummer,
                id = id,
            )
        )
    }

    fun sendTilbakedateringBehandlet(
        fødselsnummer: String,
        perioder: List<Periode>
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagTilbakedateringBehandlet(
                fødselsnummer = fødselsnummer,
                id = id,
                perioder = perioder
            )
        )
    }

    fun sendKommandokjedePåminnelse(
        commandContextId: UUID,
        meldingId: UUID,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagKommandokjedePåminnelse(
                commandContextId = commandContextId,
                meldingId = meldingId,
                id = id
            )
        )
    }

    fun sendEndretSkjermetinfo(
        fødselsnummer: String,
        skjermet: Boolean,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagEndretSkjermetinfo(
                fødselsnummer = fødselsnummer,
                skjermet = skjermet,
                id = id,
            )
        )
    }

    fun sendUtbetalingEndret(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        type: String = "UTBETALING",
        forrigeStatus: Utbetalingsstatus = NY,
        gjeldendeStatus: Utbetalingsstatus = IKKE_UTBETALT,
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        opprettet: LocalDateTime,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagUtbetalingEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                forrigeStatus = forrigeStatus,
                gjeldendeStatus = gjeldendeStatus,
                type = type,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                opprettet = opprettet,
                id = id,
            )
        )
    }

    fun sendUtbetalingAnnullert(
        fødselsnummer: String,
        utbetalingId: UUID,
        epost: String,
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagUtbetalingAnnullert(
                fødselsnummer = fødselsnummer,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                utbetalingId = utbetalingId,
                saksbehandlerEpost = epost,
                id = id,
            )
        )
    }

    fun sendGodkjenningsbehov(
        godkjenningsbehovTestdata: GodkjenningsbehovTestdata,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = godkjenningsbehovTestdata.aktørId,
                fødselsnummer = godkjenningsbehovTestdata.fødselsnummer,
                vedtaksperiodeId = godkjenningsbehovTestdata.vedtaksperiodeId,
                utbetalingId = godkjenningsbehovTestdata.utbetalingId,
                organisasjonsnummer = godkjenningsbehovTestdata.organisasjonsnummer,
                periodeFom = godkjenningsbehovTestdata.periodeFom,
                periodeTom = godkjenningsbehovTestdata.periodeTom,
                skjæringstidspunkt = godkjenningsbehovTestdata.skjæringstidspunkt,
                periodetype = godkjenningsbehovTestdata.periodetype,
                førstegangsbehandling = godkjenningsbehovTestdata.førstegangsbehandling,
                utbetalingtype = godkjenningsbehovTestdata.utbetalingtype,
                inntektskilde = godkjenningsbehovTestdata.inntektskilde,
                orgnummereMedRelevanteArbeidsforhold = godkjenningsbehovTestdata.orgnummereMedRelevanteArbeidsforhold,
                kanAvvises = godkjenningsbehovTestdata.kanAvvises,
                id = id,
                vilkårsgrunnlagId = godkjenningsbehovTestdata.vilkårsgrunnlagId,
                spleisBehandlingId = godkjenningsbehovTestdata.spleisBehandlingId,
                tags = godkjenningsbehovTestdata.tags
            )
        )
    }

    fun sendAvviksvurderingløsning(
        fødselsnummer: String,
        organisasjonsnummer: String,
        avviksprosent: Double,
        sammenligningsgrunnlagTotalbeløp: Double,
        avviksvurderingId: UUID,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        assertEquals("Avviksvurdering", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagAvviksvurderingløsning(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
                sammenligningsgrunnlagTotalbeløp = sammenligningsgrunnlagTotalbeløp,
                avviksprosent = avviksprosent,
                avviksvurderingId = avviksvurderingId
            )
        )
    }

    fun sendPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        adressebeskyttelse: Adressebeskyttelse,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        assertEquals("HentPersoninfoV2", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagPersoninfoløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
                adressebeskyttelse = adressebeskyttelse.name
            )
        )
    }

    fun sendEnhetløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        enhet: String,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        assertEquals("HentEnhet", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagEnhetløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
                enhet = enhet
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
        assertEquals("HentInfotrygdutbetalinger", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagInfotrygdutbetalingerløsning(
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
        arbeidsgiverinformasjonJson: List<ArbeidsgiverinformasjonJson>? = null,
    ): UUID =
        newUUID.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            assertEquals("Arbeidsgiverinformasjon", behov["@behov"].map { it.asText() }.single())
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            val arbeidsgivere =
                arbeidsgiverinformasjonJson ?: behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                    ArbeidsgiverinformasjonJson(
                        it.asText(),
                        "Navn for ${it.asText()}",
                        listOf("Bransje for ${it.asText()}")
                    )
                }

            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagArbeidsgiverinformasjonløsning(
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

    fun sendArbeidsgiverinformasjonKompositt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ): UUID =
        newUUID.also { id ->
            val behov = testRapid.inspektør.siste("behov")
            assertEquals(
                setOf("Arbeidsgiverinformasjon", "HentPersoninfoV2"),
                behov["@behov"].map { it.asText() }.toSet()
            )
            val contextId = UUID.fromString(behov["contextId"].asText())
            val hendelseId = UUID.fromString(behov["hendelseId"].asText())

            val organisasjoner = behov["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
                ArbeidsgiverinformasjonJson(
                    it.asText(),
                    "Navn for ${it.asText()}",
                    listOf("Bransje for ${it.asText()}")
                )
            }

            val personer: List<Map<String, Any>> = behov["HentPersoninfoV2"]["ident"].map {
                mapOf(
                    "ident" to it.asText(),
                    "fornavn" to it.asText(),
                    "etternavn" to it.asText(),
                    "fødselsdato" to LocalDate.now(),
                    "kjønn" to Kjønn.Ukjent.name,
                    "adressebeskyttelse" to Adressebeskyttelse.Ugradert.name,
                )
            }

            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagArbeidsgiverinformasjonKomposittLøsning(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    organisasjoner = organisasjoner,
                    personer = personer,
                    hendelseId = hendelseId,
                    contextId = contextId,
                    id = id,
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
        assertEquals("Arbeidsforhold", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagArbeidsforholdløsning(
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
        assertEquals("EgenAnsatt", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagEgenAnsattløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                erEgenAnsatt = erEgenAnsatt,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendVergemålOgFullmaktløsning(
        aktørId: String,
        fødselsnummer: String,
        vergemål: List<Vergemål> = emptyList(),
        fremtidsfullmakter: List<Vergemål> = emptyList(),
        fullmakter: List<Fullmakt> = emptyList(),
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        assertEquals(listOf("Vergemål", "Fullmakt"), behov["@behov"].map { it.asText() })
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        val payload = Testmeldingfabrikk.VergemålJson(vergemål, fremtidsfullmakter, fullmakter)

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagVergemålOgFullmaktKomposittLøsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vergemål = payload,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId
            )
        )
    }

    fun sendInntektløsning(
        aktørId: String,
        fødselsnummer: String,
        orgnr: String,
    ): UUID = newUUID.also { id ->
        val behov = testRapid.inspektør.siste("behov")
        assertEquals("InntekterForSykepengegrunnlag", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagInntektløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                orgnummer = orgnr,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
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
        assertEquals("ÅpneOppgaver", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagÅpneGosysOppgaverløsning(
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
        assertEquals("Risikovurdering", behov["@behov"].map { it.asText() }.single())
        val contextId = UUID.fromString(behov["contextId"].asText())
        val hendelseId = UUID.fromString(behov["hendelseId"].asText())

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagRisikovurderingløsning(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                funn = funn,
                id = id,
                hendelseId = hendelseId,
                contextId = contextId,
            )
        )
    }

    fun sendSaksbehandlerløsning(
        fødselsnummer: String,
        oppgaveId: Long,
        godkjenningsbehovId: UUID,
        godkjent: Boolean,
        kommentar: String? = null,
        begrunnelser: List<String> = emptyList(),
    ): UUID {
        return newUUID.also { id ->
            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagSaksbehandlerløsning(
                    fødselsnummer = fødselsnummer,
                    godkjent = godkjent,
                    id = id,
                    oppgaveId = oppgaveId,
                    hendelseId = godkjenningsbehovId,
                    begrunnelser = begrunnelser,
                    kommentar = kommentar
                )
            )
        }
    }

    fun sendAvsluttetMedVedtak(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        utbetalingId: UUID?,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fastsattType: String,
        settInnAvviksvurderingFraSpleis: Boolean = true,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagAvsluttetMedVedtak(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                fastsattType = fastsattType,
                id = id,
                settInnAvviksvurderingFraSpleis = settInnAvviksvurderingFraSpleis
            )
        )
    }

    fun sendAvsluttetUtenVedtak(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
    ): UUID = newUUID.also { id ->
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagAvsluttetUtenVedtak(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                id = id,
            )
        )
    }

    fun sendAdressebeskyttelseEndret(
        aktørId: String,
        fødselsnummer: String,
    ): UUID =
        newUUID.also { id ->
            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagAdressebeskyttelseEndret(aktørId, fødselsnummer, id)
            )
        }

    fun sendOppdaterPersondata(
        aktørId: String,
        fødselsnummer: String
    ): UUID =
        newUUID.also { id ->
            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagOppdaterPersondata(aktørId, fødselsnummer, id)
            )
        }

    fun sendKlargjørPersonForVisning(
        aktørId: String,
        fødselsnummer: String,
    ): UUID =
        newUUID.also { id ->
            testRapid.sendTestMessage(
                Testmeldingfabrikk.lagKlargjørPersonForVisning(aktørId, fødselsnummer, id)
            )
        }
}
