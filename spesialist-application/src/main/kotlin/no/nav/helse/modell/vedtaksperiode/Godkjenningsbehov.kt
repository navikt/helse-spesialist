package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDateTime
import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asBigDecimal
import no.nav.helse.mediator.asEnum
import no.nav.helse.mediator.asLocalDate
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.ForberedBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.kommando.OpprettEllerOppdaterArbeidsforhold
import no.nav.helse.modell.kommando.OpprettEllerOppdaterInntektskilder
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
import no.nav.helse.modell.kommando.VurderBehovForAvviksvurdering
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.kommando.VurderVidereBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Godkjenningsbehov(
    override val id: UUID,
    val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    val organisasjonsnummer: String,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    private val vedtaksperiodeId: UUID,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    private val utbetalingId: UUID,
    val spleisBehandlingId: UUID,
    private val vilkårsgrunnlagId: UUID,
    private val tags: List<String>,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val periodetype: Periodetype,
    val førstegangsbehandling: Boolean,
    val utbetalingtype: Utbetalingtype,
    val kanAvvises: Boolean,
    val inntektskilde: Inntektskilde,
    val orgnummereMedRelevanteArbeidsforhold: List<String>,
    val skjæringstidspunkt: LocalDate,
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val foreløpigBeregnetSluttPåSykepenger: LocalDate,
    private val json: String,
) : Vedtaksperiodemelding {
    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun toJson() = json

    override fun behandle(
        person: LegacyPerson,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { godkjenningsbehov(data(), person, sessionContext) }
    }

    internal fun data(): GodkjenningsbehovData =
        GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            yrkesaktivitetstype = yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisVedtaksperioder = spleisVedtaksperioder,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            tags = tags,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            foreløpigBeregnetSluttPåSykepenger = foreløpigBeregnetSluttPåSykepenger,
            json = json,
        )

    companion object {
        fun fraJson(json: String): Godkjenningsbehov {
            val jsonNode = objectMapper.readTree(json)
            val godkjenning = jsonNode["Godkjenning"]
            val yrkesaktivitetstype = jsonNode["yrkesaktivitetstype"].asEnum<Yrkesaktivitetstype>()
            return Godkjenningsbehov(
                id = jsonNode["@id"].asUUID(),
                opprettet = jsonNode["@opprettet"].asLocalDateTime(),
                fødselsnummer = jsonNode["fødselsnummer"].asText(),
                organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
                yrkesaktivitetstype = yrkesaktivitetstype,
                vedtaksperiodeId = jsonNode["vedtaksperiodeId"].asUUID(),
                spleisVedtaksperioder =
                    godkjenning["perioderMedSammeSkjæringstidspunkt"].map { periode ->
                        periode.asSpleisVedtaksperiode(
                            skjæringstidspunkt = godkjenning["skjæringstidspunkt"].asLocalDate(),
                        )
                    },
                utbetalingId = jsonNode["utbetalingId"].asUUID(),
                spleisBehandlingId = godkjenning["behandlingId"].asUUID(),
                vilkårsgrunnlagId = godkjenning["vilkårsgrunnlagId"].asUUID(),
                tags = godkjenning["tags"].map { it.asText() },
                periodeFom = godkjenning["periodeFom"].asLocalDate(),
                periodeTom = godkjenning["periodeTom"].asLocalDate(),
                periodetype = godkjenning["periodetype"].asEnum<Periodetype>(),
                førstegangsbehandling = godkjenning["førstegangsbehandling"].asBoolean(),
                utbetalingtype = godkjenning["utbetalingtype"].asEnum<Utbetalingtype>(),
                kanAvvises = godkjenning["kanAvvises"].asBoolean(),
                inntektskilde = godkjenning["inntektskilde"].asEnum<Inntektskilde>(),
                orgnummereMedRelevanteArbeidsforhold =
                    godkjenning["orgnummereMedRelevanteArbeidsforhold"]
                        ?.map(JsonNode::asText)
                        .orEmpty(),
                skjæringstidspunkt = godkjenning["skjæringstidspunkt"].asLocalDate(),
                sykepengegrunnlagsfakta =
                    godkjenning["sykepengegrunnlagsfakta"].asSykepengegrunnlagsfakta(
                        yrkesaktivitetstype,
                    ),
                foreløpigBeregnetSluttPåSykepenger = godkjenning["foreløpigBeregnetSluttPåSykepenger"].asLocalDate(),
                json = json,
            )
        }

        private fun JsonNode.asSpleisVedtaksperiode(skjæringstidspunkt: LocalDate): SpleisVedtaksperiode =
            SpleisVedtaksperiode(
                vedtaksperiodeId = this["vedtaksperiodeId"].asUUID(),
                spleisBehandlingId = this["behandlingId"].asUUID(),
                fom = this["fom"].asLocalDate(),
                tom = this["tom"].asLocalDate(),
                skjæringstidspunkt = skjæringstidspunkt,
            )

        private fun JsonNode.asSykepengegrunnlagsfakta(yrkesaktivitetstype: Yrkesaktivitetstype): Sykepengegrunnlagsfakta =
            if (yrkesaktivitetstype == Yrkesaktivitetstype.SELVSTENDIG) {
                when (val fastsatt = this["fastsatt"].asText()) {
                    "EtterHovedregel" -> {
                        check(this["arbeidsgivere"] == null || this["arbeidsgivere"].isEmpty) {
                            "Selvstendig næringsdrivende har arbeidsgiver(e)"
                        }
                        Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende(
                            seksG = this["6G"].asDouble(),
                            sykepengegrunnlag = this["sykepengegrunnlag"].asDouble(),
                            selvstendig = this["selvstendig"].tilSelvstendig(),
                        )
                    }

                    else -> error("Ugyldig verdi for fastsatt for selvstendig næringsdrivende: \"$fastsatt\"")
                }
            } else {
                when (val fastsatt = this["fastsatt"].asText()) {
                    "IInfotrygd" ->
                        Sykepengegrunnlagsfakta.Infotrygd

                    "EtterSkjønn" ->
                        Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterSkjønn(
                            seksG = this["6G"].asDouble(),
                            arbeidsgivere =
                                this["arbeidsgivere"].map { arbeidsgiver ->
                                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                        organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText(),
                                        omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                        skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asDouble(),
                                        inntektskilde = arbeidsgiver["inntektskilde"].asInntektskilde(),
                                    )
                                },
                        )

                    "EtterHovedregel" ->
                        Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                            seksG = this["6G"].asDouble(),
                            sykepengegrunnlag = this["sykepengegrunnlag"].asDouble(),
                            arbeidsgivere =
                                this["arbeidsgivere"].map { arbeidsgiver ->
                                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                        organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText(),
                                        omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asDouble(),
                                        inntektskilde = arbeidsgiver["inntektskilde"].asInntektskilde(),
                                    )
                                },
                        )

                    else -> error("Ukjent verdi for fastsatt: \"$fastsatt\"")
                }
            }

        private fun JsonNode.tilSelvstendig(): Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende.Selvstendig =
            Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende.Selvstendig(
                beregningsgrunnlag = this["beregningsgrunnlag"].asBigDecimal(),
                pensjonsgivendeInntekter =
                    this["pensjonsgivendeInntekter"].map { inntekt ->
                        Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende.Selvstendig.PensjonsgivendeInntekt(
                            årstall = inntekt["årstall"].asInt(),
                            beløp = inntekt["beløp"].asBigDecimal(),
                        )
                    },
            )

        private fun JsonNode.asInntektskilde(): Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde =
            when (val inntektskilde = asText()) {
                "Arbeidsgiver" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
                "AOrdningen" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.AOrdningen
                "Saksbehandler" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Saksbehandler
                "Sigrun" -> Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Sigrun
                else -> error("Ukjent verdi for inntektskilde: \"$inntektskilde\"")
            }
    }

    sealed interface Sykepengegrunnlagsfakta {
        data object Infotrygd : Sykepengegrunnlagsfakta

        sealed class Spleis : Sykepengegrunnlagsfakta {
            abstract val seksG: Double

            sealed class Arbeidstaker : Spleis() {
                abstract val arbeidsgivere: List<Arbeidsgiver>

                data class EtterSkjønn(
                    override val seksG: Double,
                    override val arbeidsgivere: List<Arbeidsgiver.EtterSkjønn>,
                ) : Arbeidstaker()

                data class EtterHovedregel(
                    override val seksG: Double,
                    override val arbeidsgivere: List<Arbeidsgiver.EtterHovedregel>,
                    val sykepengegrunnlag: Double,
                ) : Arbeidstaker()
            }

            data class SelvstendigNæringsdrivende(
                val sykepengegrunnlag: Double,
                override val seksG: Double,
                val selvstendig: Selvstendig,
            ) : Spleis() {
                data class Selvstendig(
                    val beregningsgrunnlag: BigDecimal,
                    val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
                ) {
                    data class PensjonsgivendeInntekt(
                        val årstall: Int,
                        val beløp: BigDecimal,
                    )
                }
            }

            sealed interface Arbeidsgiver {
                val organisasjonsnummer: String
                val omregnetÅrsinntekt: Double
                val inntektskilde: Inntektskilde

                data class EtterSkjønn(
                    override val organisasjonsnummer: String,
                    override val omregnetÅrsinntekt: Double,
                    override val inntektskilde: Inntektskilde,
                    val skjønnsfastsatt: Double,
                ) : Arbeidsgiver

                data class EtterHovedregel(
                    override val organisasjonsnummer: String,
                    override val omregnetÅrsinntekt: Double,
                    override val inntektskilde: Inntektskilde,
                ) : Arbeidsgiver

                enum class Inntektskilde {
                    Arbeidsgiver,
                    AOrdningen,
                    Saksbehandler,
                    Sigrun,
                }
            }
        }
    }
}

internal class GodkjenningsbehovCommand(
    behovData: GodkjenningsbehovData,
    utbetaling: Utbetaling,
    førsteKjenteDagFinner: () -> LocalDate?,
    automatisering: Automatisering,
    vedtakDao: VedtakDao,
    meldingDao: MeldingDao,
    commandContextDao: CommandContextDao,
    personDao: PersonDao,
    arbeidsgiverRepository: ArbeidsgiverRepository,
    arbeidsforholdDao: ArbeidsforholdDao,
    egenAnsattDao: EgenAnsattDao,
    utbetalingDao: UtbetalingDao,
    vergemålDao: VergemålDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    risikovurderingDao: RisikovurderingDao,
    påVentDao: PåVentDao,
    automatiseringDao: AutomatiseringDao,
    oppgaveRepository: OppgaveRepository,
    oppgaveDao: OppgaveDao,
    periodehistorikkDao: PeriodehistorikkDao,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
    avviksvurderingRepository: AvviksvurderingRepository,
    opptegnelseDao: OpptegnelseDao,
    tildelingDao: TildelingDao,
    reservasjonDao: ReservasjonDao,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    person: LegacyPerson,
) : MacroCommand() {
    private val sykefraværstilfelle = person.sykefraværstilfelle(behovData.vedtaksperiodeId)
    override val commands: List<Command> =
        listOf(
            ForberedBehandlingAvGodkjenningsbehov(
                commandData = behovData,
                person = person,
            ),
            VurderVidereBehandlingAvGodkjenningsbehov(
                commandData = behovData,
                utbetalingDao = utbetalingDao,
                oppgaveRepository = oppgaveRepository,
                oppgaveDao = oppgaveDao,
                vedtakDao = vedtakDao,
                meldingDao = meldingDao,
                fødselsnummer = behovData.fødselsnummer,
                tildelingDao = tildelingDao,
                reservasjonDao = reservasjonDao,
            ),
            OpprettKoblingTilHendelseCommand(
                commandData = behovData,
                vedtakDao = vedtakDao,
            ),
            AvbrytContextCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                commandContextDao = commandContextDao,
            ),
            VurderBehovForAvviksvurdering(
                fødselsnummer = behovData.fødselsnummer,
                skjæringstidspunkt = behovData.skjæringstidspunkt,
                avviksvurderingRepository = avviksvurderingRepository,
                sykepengegrunnlagsfakta = behovData.sykepengegrunnlagsfakta,
                vilkårsgrunnlagId = behovData.vilkårsgrunnlagId,
                legacyBehandling =
                    person
                        .vedtaksperiode(behovData.vedtaksperiodeId)
                        .finnBehandling(behovData.spleisBehandlingId),
                yrkesaktivitetstype = behovData.yrkesaktivitetstype,
                organisasjonsnummer = behovData.organisasjonsnummer,
            ),
            PersisterVedtaksperiodetypeCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                vedtaksperiodetype = behovData.periodetype,
                inntektskilde = behovData.inntektskilde,
                vedtakDao = vedtakDao,
            ),
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                utbetalingId = behovData.utbetalingId,
                utbetalingDao = utbetalingDao,
            ),
            ForberedVisningCommand(
                fødselsnummer = behovData.fødselsnummer,
                organisasjonsnummer = behovData.organisasjonsnummer,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
                arbeidsgiverIdentifikatorer = (behovData.orgnummereMedRelevanteArbeidsforhold + behovData.organisasjonsnummer).toSet(),
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
                arbeidsforholdDao = arbeidsforholdDao,
            ),
            KontrollerEgenAnsattstatus(
                fødselsnummer = behovData.fødselsnummer,
                egenAnsattDao = egenAnsattDao,
            ),
            VurderVergemålOgFullmakt(
                fødselsnummer = behovData.fødselsnummer,
                vergemålDao = vergemålDao,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderEnhetUtland(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                personDao = personDao,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderÅpenGosysoppgave(
                åpneGosysOppgaverDao = åpneGosysOppgaverDao,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
                harTildeltOppgave = false,
                oppgaveService = oppgaveService,
            ),
            VurderVurderingsmomenter(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                risikovurderingDao = risikovurderingDao,
                organisasjonsnummer = behovData.organisasjonsnummer,
                yrkesaktivitetstype = behovData.yrkesaktivitetstype,
                førstegangsbehandling = behovData.førstegangsbehandling,
                sykefraværstilfelle = sykefraværstilfelle,
                utbetaling = utbetaling,
                sykepengegrunnlagsfakta = behovData.sykepengegrunnlagsfakta,
            ),
            VurderAutomatiskAvvisning(
                personDao = personDao,
                vergemålDao = vergemålDao,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                godkjenningsbehov = behovData,
            ),
            VurderBehovForTotrinnskontroll(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiode = person.vedtaksperiode(behovData.vedtaksperiodeId),
                oppgaveService = oppgaveService,
                periodehistorikkDao = periodehistorikkDao,
                totrinnsvurderingRepository = totrinnsvurderingRepository,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = behovData,
                automatiseringDao = automatiseringDao,
                oppgaveService = oppgaveService,
            ),
            OpprettSaksbehandleroppgave(
                behovData = behovData,
                oppgaveService = oppgaveService,
                automatisering = automatisering,
                personDao = personDao,
                risikovurderingDao = risikovurderingDao,
                egenAnsattDao = egenAnsattDao,
                utbetalingtype = behovData.utbetalingtype,
                sykefraværstilfelle = sykefraværstilfelle,
                utbetaling = utbetaling,
                vergemålDao = vergemålDao,
                påVentDao = påVentDao,
                opptegnelseDao = opptegnelseDao,
            ),
            PersisterInntektCommand(
                fødselsnummer = behovData.fødselsnummer,
                skjæringstidspunkt = behovData.skjæringstidspunkt,
                personDao = personDao,
            ),
        )
}

private class ForberedVisningCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    førsteKjenteDagFinner: () -> LocalDate?,
    personDao: PersonDao,
    arbeidsgiverIdentifikatorer: Set<String>,
    arbeidsgiverRepository: ArbeidsgiverRepository,
    avviksvurderingRepository: AvviksvurderingRepository,
    arbeidsforholdDao: ArbeidsforholdDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersonCommand(
                fødselsnummer = fødselsnummer,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
            ),
            OpprettEllerOppdaterInntektskilder(
                fødselsnummer = fødselsnummer,
                identifikatorer = arbeidsgiverIdentifikatorer,
                arbeidsgiverRepository = arbeidsgiverRepository,
                avviksvurderingRepository = avviksvurderingRepository,
            ),
            OpprettEllerOppdaterArbeidsforhold(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                arbeidsforholdDao = arbeidsforholdDao,
            ),
        )
}
