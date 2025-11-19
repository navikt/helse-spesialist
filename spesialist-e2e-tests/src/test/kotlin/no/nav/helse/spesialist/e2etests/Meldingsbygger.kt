package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.Sykepengegrunnlagsfakta
import no.nav.helse.spesialist.e2etests.context.Sykepengegrunnlagsfakta.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import java.time.LocalDateTime
import java.util.UUID

object Meldingsbygger {
    fun byggSendSøknadNav(person: Person, arbeidsgiver: Arbeidsgiver) =
        Testmeldingfabrikk.lagSøknadSendt(
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer
        )

    fun byggBehandlingOpprettet(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) =
        Testmeldingfabrikk.lagBehandlingOpprettet(
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer,
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            spleisBehandlingId = vedtaksperiode.spleisBehandlingIdForÅByggeMelding("behandling_opprettet"),
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
        )

    fun byggVedtaksperiodeNyUtbetaling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) =
        Testmeldingfabrikk.lagVedtaksperiodeNyUtbetaling(
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            utbetalingId = vedtaksperiode.utbetalingIdForÅByggeMelding("vedtaksperiode_ny_utbetaling"),
        )

    fun byggVedtaksperiodeEndret(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        forrigeTilstand: String,
        gjeldendeTilstand: String,
    ) =
        Testmeldingfabrikk.lagVedtaksperiodeEndret(
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand,
        )

    fun byggVedtaksperiodeForkastet(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
    ) =
        Testmeldingfabrikk.lagVedtaksperiodeForkastet(
            fødselsnummer = person.fødselsnummer,
            aktørId = person.aktørId,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        )

    fun byggGodkjenningsbehov(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vilkårsgrunnlagId: UUID,
        vedtaksperiode: Vedtaksperiode,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta = Sykepengegrunnlagsfakta(
            fastsatt = Sykepengegrunnlagsfakta.FastsattType.EtterHovedregel,
            arbeidsgivere = listOf(
                Sykepengegrunnlagsfakta.Arbeidsgiver(
                    organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                    omregnetÅrsinntekt = 123456.7
                )
            )
        ),
        tags: List<String> = listOf("Innvilget")
    ): String {
        val meldingsnavn = "Godkjenningsbehov"
        return Testmeldingfabrikk.lagGodkjenningsbehov(
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            utbetalingId = vedtaksperiode.utbetalingIdForÅByggeMelding(meldingsnavn),
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            periodeFom = vedtaksperiode.fom,
            periodeTom = vedtaksperiode.tom,
            skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            førstegangsbehandling = true,
            utbetalingtype = Utbetalingtype.UTBETALING,
            inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
            orgnummereMedRelevanteArbeidsforhold = emptyList(),
            kanAvvises = true,
            spleisBehandlingId = vedtaksperiode.spleisBehandlingIdForÅByggeMelding(meldingsnavn),
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            tags = tags,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf(
                    "fom" to vedtaksperiode.fom,
                    "tom" to vedtaksperiode.tom,
                    "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
                    "behandlingId" to vedtaksperiode.spleisBehandlingIdForÅByggeMelding(meldingsnavn)
                )
            ),
            sykepengegrunnlagsfakta = when (sykepengegrunnlagsfakta.fastsatt) {
                Sykepengegrunnlagsfakta.FastsattType.EtterHovedregel -> Testmeldingfabrikk.godkjenningsbehovFastsattEtterHovedregel(
                    sykepengegrunnlag = sykepengegrunnlagsfakta.arbeidsgivere.sumOf { it.omregnetÅrsinntekt },
                    arbeidsgivere = sykepengegrunnlagsfakta.arbeidsgivere.map {
                        buildMap {
                            put("arbeidsgiver", it.organisasjonsnummer)
                            put("omregnetÅrsinntekt", it.omregnetÅrsinntekt)
                            put("inntektskilde", it.inntektskilde)
                        }
                    }
                )
                Sykepengegrunnlagsfakta.FastsattType.EtterSkjønn -> Testmeldingfabrikk.godkjenningsbehovFastsattEtterSkjønn(
                    sykepengegrunnlag = sykepengegrunnlagsfakta.arbeidsgivere.sumOf { it.omregnetÅrsinntekt },
                    arbeidsgivere = sykepengegrunnlagsfakta.arbeidsgivere.map {
                        buildMap {
                            put("arbeidsgiver", it.organisasjonsnummer)
                            put("omregnetÅrsinntekt", it.omregnetÅrsinntekt)
                            put("inntektskilde", it.inntektskilde)
                            if (it is SkjønnsfastsattArbeidsgiver) put("skjønnsfastsatt", it.skjønnsfastsatt)
                        }
                    }
                )
            }
        )
    }

    fun byggGosysOppgaveEndret(person: Person) =
        Testmeldingfabrikk.lagGosysOppgaveEndret(fødselsnummer = person.fødselsnummer)

    fun byggAktivitetsloggNyAktivitetMedVarsler(
        varselkoder: List<String>,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode
    ) =
        Testmeldingfabrikk.lagAktivitetsloggNyAktivitet(
            id = UUID.randomUUID(),
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer,
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            varselkoder = varselkoder
        )

    fun byggUtbetalingEndret(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        forrigeStatus: String,
        gjeldendeStatus: String
    ) =
        Testmeldingfabrikk.lagUtbetalingEndret(
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer,
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            utbetalingId = vedtaksperiode.utbetalingIdForÅByggeMelding("utbetaling_endret"),
            forrigeStatus = enumValueOf(forrigeStatus),
            gjeldendeStatus = enumValueOf(gjeldendeStatus),
            opprettet = LocalDateTime.now(),
            id = UUID.randomUUID(),
        )

    fun byggAvsluttetMedVedtak(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode
    ) = Testmeldingfabrikk.lagAvsluttetMedVedtak(
        aktørId = person.aktørId,
        fødselsnummer = person.fødselsnummer,
        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        spleisBehandlingId = vedtaksperiode.spleisBehandlingIdForÅByggeMelding("avsluttet_med_vedtak"),
        utbetalingId = vedtaksperiode.utbetalingIdForÅByggeMelding("avsluttet_med_vedtak"),
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
        skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
        sykepengegrunnlagsfakta = when (vedtaksperiode.sykepengegrunnlagsfakta.fastsatt) {
            Sykepengegrunnlagsfakta.FastsattType.EtterHovedregel -> Testmeldingfabrikk.avsluttetMedVedtakFastsattEtterHovedregel(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                omregnetÅrsinntektTotalt = 600000.0,
                innrapportertÅrsinntekt = 600000.0,
                avviksprosent = 0.0,
                sykepengegrunnlag = vedtaksperiode.sykepengegrunnlagsfakta.arbeidsgivere.sumOf { it.omregnetÅrsinntekt },
                arbeidsgivere = vedtaksperiode.sykepengegrunnlagsfakta.arbeidsgivere.map {
                    buildMap<String, Any> {
                        put("arbeidsgiver", it.organisasjonsnummer)
                        put("omregnetÅrsinntekt", it.omregnetÅrsinntekt)
                        put("inntektskilde", it.inntektskilde)
                    }
                }
            )
            Sykepengegrunnlagsfakta.FastsattType.EtterSkjønn -> Testmeldingfabrikk.avsluttetMedVedtakFastsattEtterSkjønn(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                omregnetÅrsinntektTotalt = 600000.0,
                innrapportertÅrsinntekt = 600000.0,
                avviksprosent = 0.0,
                skjønnsfastsatt = vedtaksperiode.sykepengegrunnlagsfakta.arbeidsgivere.sumOf { it.omregnetÅrsinntekt },
                arbeidsgivere = vedtaksperiode.sykepengegrunnlagsfakta.arbeidsgivere.map {
                    buildMap {
                        put("arbeidsgiver", it.organisasjonsnummer)
                        put("omregnetÅrsinntekt", it.omregnetÅrsinntekt)
                        put("inntektskilde", it.inntektskilde)
                        if (it is SkjønnsfastsattArbeidsgiver) put("skjønnsfastsatt", it.skjønnsfastsatt)
                    }
                }
            )
        },
        id = UUID.randomUUID(),
    )

    fun byggVarselkodeNyDefinisjon(varselkode: String) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "varselkode_ny_definisjon",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "varselkode" to varselkode,
                "gjeldende_definisjon" to mapOf(
                    "id" to UUID.randomUUID(),
                    "kode" to varselkode,
                    "tittel" to "En tittel for varselkode=$varselkode",
                    "forklaring" to "En forklaring for varselkode=$varselkode",
                    "handling" to "En handling for varselkode=$varselkode",
                    "avviklet" to false,
                    "opprettet" to LocalDateTime.now()
                )
            )
        ).toJson()

    fun byggAdressebeskyttelseEndret(person: Person) =
        Testmeldingfabrikk.lagAdressebeskyttelseEndret(
            aktørId = person.aktørId,
            fødselsnummer = person.fødselsnummer,
        )

    fun byggEndretSkjermetinfo(person: Person, skjermet: Boolean) =
        Testmeldingfabrikk.lagEndretSkjermetinfo(
            fødselsnummer = person.fødselsnummer,
            skjermet = skjermet,
            id = UUID.randomUUID(),
        )
}
