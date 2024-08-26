package no.nav.helse.spesialist.api.graphql.schema

import io.ktor.utils.io.core.toByteArray
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Arbeidsforhold(
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
)

data class Generasjon(
    val id: UUID,
    val perioder: List<Periode>,
)

data class ArbeidsgiverInntekterFraAOrdningen(
    val skjaeringstidspunkt: String,
    val inntekter: List<InntektFraAOrdningen>,
)

interface Overstyring {
    val hendelseId: UUID
    val timestamp: LocalDateTime
    val saksbehandler: Saksbehandler
    val ferdigstilt: Boolean
}

data class Dagoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val dager: List<OverstyrtDag>,
    val begrunnelse: String,
) : Overstyring {
    data class OverstyrtDag(
        val dato: LocalDate,
        val type: Dagtype,
        val fraType: Dagtype?,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

data class Inntektoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val inntekt: OverstyrtInntekt,
) : Overstyring {
    data class OverstyrtInntekt(
        val forklaring: String,
        val begrunnelse: String,
        val manedligInntekt: Double,
        val fraManedligInntekt: Double?,
        val skjaeringstidspunkt: LocalDate,
        val refusjonsopplysninger: List<Refusjonsopplysning>?,
        val fraRefusjonsopplysninger: List<Refusjonsopplysning>?,
    )

    data class Refusjonsopplysning(
        val fom: LocalDate,
        val tom: LocalDate?,
        val belop: Double,
    )
}

data class MinimumSykdomsgradOverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val minimumSykdomsgrad: OverstyrtMinimumSykdomsgrad,
) : Overstyring {
    data class OverstyrtMinimumSykdomsgrad(
        val fom: LocalDate,
        val tom: LocalDate,
        val vurdering: Boolean,
        val begrunnelse: String,
        val initierendeVedtaksperiodeId: UUID,
    )
}

data class Sykepengegrunnlagskjonnsfastsetting(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
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
        val skjaeringstidspunkt: LocalDate,
    )
}

data class Arbeidsforholdoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: LocalDate,
    val forklaring: String,
    val begrunnelse: String,
) : Overstyring

data class GhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String,
) {
    val id = UUID.nameUUIDFromBytes(fom.toString().toByteArray() + organisasjonsnummer.toByteArray()).toString()
}

data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>,
    val ghostPerioder: List<GhostPeriode>,
    private val fødselsnummer: String,
    private val generasjoner: List<GraphQLGenerasjon>,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val tilganger: SaksbehandlerTilganger,
    private val overstyringer: List<Overstyring>,
) {
    fun generasjoner(): List<Generasjon> =
        generasjoner.mapIndexed { index, generasjon ->
            val oppgaveId = oppgaveApiDao.finnOppgaveId(fødselsnummer)
            val perioderSomSkalViseAktiveVarsler = varselRepository.perioderSomSkalViseVarsler(oppgaveId)
            Generasjon(
                id = generasjon.id,
                perioder =
                    generasjon.perioder.map {
                        when (it) {
                            is GraphQLUberegnetPeriode ->
                                UberegnetPeriode(
                                    varselRepository = varselRepository,
                                    periode = it,
                                    skalViseAktiveVarsler = index == 0 && perioderSomSkalViseAktiveVarsler.contains(it.vedtaksperiodeId),
                                    notatDao = notatDao,
                                    index = index,
                                )

                            is GraphQLBeregnetPeriode ->
                                BeregnetPeriode(
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
                                    saksbehandlerhåndterer = saksbehandlerhåndterer,
                                )
                            else -> throw Exception("Ukjent tidslinjeperiode")
                        }
                    },
            )
        }

    fun overstyringer(): List<Overstyring> = overstyringer

    fun arbeidsforhold(): List<Arbeidsforhold> =
        arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
            Arbeidsforhold(
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent,
                startdato = it.startdato,
                sluttdato = it.sluttdato,
            )
        }

    @Suppress("unused")
    fun inntekterFraAordningen(): List<ArbeidsgiverInntekterFraAOrdningen> =
        arbeidsgiverApiDao.finnArbeidsgiverInntekterFraAordningen(
            fødselsnummer,
            organisasjonsnummer,
        )
}
