package no.nav.helse.spesialist.api.graphql.schema

import io.ktor.utils.io.core.toByteArray
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetVilkarsprovdPeriode

data class Arbeidsforhold(
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: DateString,
    val sluttdato: DateString?,
)

data class Generasjon(
    val id: UUIDString,
    val perioder: List<Periode>,
)

data class ArbeidsgiverInntekterFraAOrdningen(
    val skjaeringstidspunkt: String,
    val inntekter: List<InntektFraAOrdningen>,
)

interface Overstyring {
    val hendelseId: UUIDString
    val timestamp: DateTimeString
    val saksbehandler: Saksbehandler
    val ferdigstilt: Boolean
}

data class Dagoverstyring(
    override val hendelseId: UUIDString,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val dager: List<OverstyrtDag>,
    val begrunnelse: String,
) : Overstyring {
    data class OverstyrtDag(
        val dato: DateString,
        val type: Dagtype,
        val fraType: Dagtype?,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

data class Inntektoverstyring(
    override val hendelseId: UUIDString,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val inntekt: OverstyrtInntekt,
) : Overstyring {
    data class OverstyrtInntekt(
        val forklaring: String,
        val begrunnelse: String,
        val manedligInntekt: Double,
        val fraManedligInntekt: Double?,
        val skjaeringstidspunkt: DateTimeString,
        val refusjonsopplysninger: List<Refusjonsopplysning>?,
        val fraRefusjonsopplysninger: List<Refusjonsopplysning>?,
    )

    data class Refusjonsopplysning(
        val fom: DateString,
        val tom: DateString?,
        val belop: Double,
    )
}

data class Sykepengegrunnlagskjonnsfastsetting(
    override val hendelseId: UUIDString,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val skjonnsfastsatt: SkjonnsfastsattSykepengegrunnlag,
) : Overstyring {
    data class SkjonnsfastsattSykepengegrunnlag(
        val arsak: String,
        val type: Skjonnsfastsettingstype?,
        val begrunnelse: String?,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val arlig: Double,
        val fraArlig: Double?,
        val skjaeringstidspunkt: DateTimeString,
    )
}

data class Arbeidsforholdoverstyring(
    override val hendelseId: UUIDString,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: DateString,
    val forklaring: String,
    val begrunnelse: String,
) : Overstyring

data class GhostPeriode(
    val fom: DateString,
    val tom: DateString,
    val skjaeringstidspunkt: DateString,
    val vilkarsgrunnlagId: UUIDString?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String,
) {
    val id = UUID.nameUUIDFromBytes(fom.toByteArray() + organisasjonsnummer.toByteArray()).toString()
}

data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>,
    val ghostPerioder: List<GhostPeriode>,
    private val fødselsnummer: String,
    private val generasjoner: List<GraphQLGenerasjon>,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val overstyringApiDao: OverstyringApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val tilganger: SaksbehandlerTilganger,
) {
    fun generasjoner(): List<Generasjon> = generasjoner.mapIndexed { index, generasjon ->
        val oppgaveId = oppgaveApiDao.finnOppgaveId(fødselsnummer)
        val perioderSomSkalViseAktiveVarsler = varselRepository.perioderSomSkalViseVarsler(oppgaveId)
        Generasjon(
            id = generasjon.id,
            perioder = generasjon.perioder.map {
                when (it) {
                    is GraphQLUberegnetPeriode -> UberegnetPeriode(
                        varselRepository = varselRepository,
                        periode = it,
                        skalViseAktiveVarsler = index == 0 && perioderSomSkalViseAktiveVarsler.contains(UUID.fromString(it.vedtaksperiodeId)),
                        notatDao = notatDao,
                        index = index,
                    )

                    is GraphQLBeregnetPeriode -> BeregnetPeriode(
                        orgnummer = organisasjonsnummer,
                        periode = it,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselRepository = varselRepository,
                        oppgaveApiDao = oppgaveApiDao,
                        periodehistorikkDao = periodehistorikkDao,
                        notatDao = notatDao,
                        totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                        påVentApiDao = påVentApiDao,
                        tilganger = tilganger,
                        erSisteGenerasjon = index == 0,
                        index = index,
                        oppgavehåndterer = oppgavehåndterer,
                    )

                    is GraphQLUberegnetVilkarsprovdPeriode -> UberegnetVilkarsprovdPeriode(
                        vilkarsgrunnlagId = it.vilkarsgrunnlagId,
                        varselRepository = varselRepository,
                        periode = it,
                        // Antakelse om at hvis det ikke finnes oppgave er det en periode som skal skjønnsfastsettes. Da vil vi vise varsler.
                        // Dette kans fjernes når skjønnsfastsettingperioder også har oppgave.
                        skalViseAktiveVarsler = if (oppgaveId == null) true else index == 0 && perioderSomSkalViseAktiveVarsler.contains(UUID.fromString(it.vedtaksperiodeId)),
                        notatDao = notatDao,
                        index = index,
                    )

                    else -> throw Exception("Ukjent tidslinjeperiode")
                }
            }
        )
    }

    fun overstyringer(): List<Overstyring> =
        overstyringApiDao.finnOverstyringerAvTidslinjer(fødselsnummer, organisasjonsnummer)
            .map { it.tilDagoverstyring() } +
        overstyringApiDao.finnOverstyringerAvInntekt(fødselsnummer, organisasjonsnummer)
            .map { it.tilInntektoverstyring() } +
        overstyringApiDao.finnOverstyringerAvArbeidsforhold(fødselsnummer, organisasjonsnummer)
            .map { it.tilArbeidsforholdoverstyring() } +
        overstyringApiDao.finnSkjønnsfastsettingSykepengegrunnlag(fødselsnummer, organisasjonsnummer)
            .map { it.tilSykepengegrunnlagSkjønnsfastsetting() }

    fun arbeidsforhold(): List<Arbeidsforhold> =
        arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
            Arbeidsforhold(
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent,
                startdato = it.startdato.format(DateTimeFormatter.ISO_DATE),
                sluttdato = it.sluttdato?.format(DateTimeFormatter.ISO_DATE)
            )
        }

    fun inntekterFraAordningen(): List<ArbeidsgiverInntekterFraAOrdningen> = arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(fødselsnummer, organisasjonsnummer)
}

private fun OverstyringTidslinjeDto.tilDagoverstyring() = Dagoverstyring(
    hendelseId = hendelseId.toString(),
    begrunnelse = begrunnelse,
    timestamp = timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandler = Saksbehandler(
        navn = saksbehandlerNavn,
        ident = saksbehandlerIdent
    ),
    dager = overstyrteDager.map { dag ->
        Dagoverstyring.OverstyrtDag(
            dato = dag.dato.format(DateTimeFormatter.ISO_DATE),
            type = dag.type,
            fraType = dag.fraType,
            grad = dag.grad,
            fraGrad = dag.fraGrad
        )
    },
    ferdigstilt = ferdigstilt,
)

private fun OverstyringInntektDto.tilInntektoverstyring() = Inntektoverstyring(
    hendelseId = hendelseId.toString(),
    timestamp = timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandler = Saksbehandler(
        navn = saksbehandlerNavn,
        ident = saksbehandlerIdent
    ),
    inntekt = Inntektoverstyring.OverstyrtInntekt(
        forklaring = forklaring,
        begrunnelse = begrunnelse,
        manedligInntekt = månedligInntekt,
        fraManedligInntekt = fraMånedligInntekt,
        skjaeringstidspunkt = skjæringstidspunkt.format(DateTimeFormatter.ISO_DATE),
        refusjonsopplysninger = refusjonsopplysninger?.map {
            Inntektoverstyring.Refusjonsopplysning(
                fom = it.fom.toString(),
                tom = it.tom?.toString(),
                belop = it.beløp
            )
        } ?: emptyList(),
        fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map {
            Inntektoverstyring.Refusjonsopplysning(
                fom = it.fom.toString(),
                tom = it.tom?.toString(),
                belop = it.beløp
            )
        } ?: emptyList(),
    ),
    ferdigstilt = ferdigstilt,
)

private fun OverstyringArbeidsforholdDto.tilArbeidsforholdoverstyring() = Arbeidsforholdoverstyring(
    hendelseId = hendelseId.toString(),
    begrunnelse = begrunnelse,
    timestamp = timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandler = Saksbehandler(
        navn = saksbehandlerNavn,
        ident = saksbehandlerIdent
    ),
    deaktivert = deaktivert,
    skjaeringstidspunkt = skjæringstidspunkt.format(DateTimeFormatter.ISO_DATE),
    forklaring = forklaring,
    ferdigstilt = ferdigstilt,
)

private fun SkjønnsfastsettingSykepengegrunnlagDto.tilSykepengegrunnlagSkjønnsfastsetting() = Sykepengegrunnlagskjonnsfastsetting(
    hendelseId = hendelseId.toString(),
    timestamp = timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandler = Saksbehandler(
        navn = saksbehandlerNavn,
        ident = saksbehandlerIdent
    ),
    skjonnsfastsatt = Sykepengegrunnlagskjonnsfastsetting.SkjonnsfastsattSykepengegrunnlag(
        arsak = årsak,
        type = type,
        begrunnelse = begrunnelse,
        begrunnelseMal = begrunnelseMal,
        begrunnelseFritekst = begrunnelseFritekst,
        begrunnelseKonklusjon = begrunnelseKonklusjon,
        arlig = årlig,
        fraArlig = fraÅrlig,
        skjaeringstidspunkt = skjæringstidspunkt.format(DateTimeFormatter.ISO_DATE),
    ),
    ferdigstilt = ferdigstilt,
)

