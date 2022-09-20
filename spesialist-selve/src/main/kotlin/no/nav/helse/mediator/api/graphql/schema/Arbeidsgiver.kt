package no.nav.helse.mediator.api.graphql.schema

import java.time.format.DateTimeFormatter
import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

data class Arbeidsforhold(
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class Generasjon(
    val id: UUID,
    val perioder: List<Periode>
)

interface Overstyring {
    val hendelseId: UUID
    val begrunnelse: String
    val timestamp: LocalDateTime
    val saksbehandler: Saksbehandler
}

data class Dagoverstyring(
    override val hendelseId: UUID,
    override val begrunnelse: String,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    val dager: List<OverstyrtDag>,
) : Overstyring {
    data class OverstyrtDag(
        val dato: LocalDate,
        val type: Dagtype,
        val fraType: Dagtype?,
        val grad: Int?,
        val fraGrad: Int?
    )
}

data class Inntektoverstyring(
    override val hendelseId: UUID,
    override val begrunnelse: String,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    val inntekt: OverstyrtInntekt,
) : Overstyring {
    data class OverstyrtInntekt(
        val forklaring: String,
        val manedligInntekt: Double,
        val fraManedligInntekt: Double?,
        val skjaeringstidspunkt: LocalDateTime
    )
}

data class Arbeidsforholdoverstyring(
    override val hendelseId: UUID,
    override val begrunnelse: String,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: LocalDate,
    val forklaring: String
) : Overstyring

data class GhostPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlaghistorikkId: UUID?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String
)

data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>,
    val ghostPerioder: List<GhostPeriode>,
    private val fødselsnummer: String,
    private val generasjoner: List<GraphQLGenerasjon>,
    private val overstyringApiDao: OverstyringApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselDao: VarselDao,
    private val oppgaveDao: OppgaveDao,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
) {
    fun generasjoner(): List<Generasjon> = generasjoner.map { generasjon ->
        Generasjon(
            id = generasjon.id,
            perioder = generasjon.perioder.map {
                when (it) {
                    is GraphQLUberegnetPeriode -> UberegnetPeriode(id = it.id, periode = it)
                    is GraphQLBeregnetPeriode -> BeregnetPeriode(
                        id = it.id,
                        periode = it,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselDao = varselDao,
                        oppgaveDao = oppgaveDao,
                        oppgaveApiDao = oppgaveApiDao,
                        periodehistorikkDao = periodehistorikkDao,
                        notatDao = notatDao
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
            .map { it.tilArbeidsforholdoverstyring() }

    fun arbeidsforhold(): List<Arbeidsforhold> =
        arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
            Arbeidsforhold(
                stillingstittel = it.stillingstittel,
                stillingsprosent = it.stillingsprosent,
                startdato = it.startdato.format(DateTimeFormatter.ISO_DATE),
                sluttdato = it.sluttdato?.format(DateTimeFormatter.ISO_DATE)
            )
        }
}

private fun OverstyringDto.tilDagoverstyring() = Dagoverstyring(
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
    }
)

private fun OverstyringInntektDto.tilInntektoverstyring() = Inntektoverstyring(
    hendelseId = hendelseId.toString(),
    begrunnelse = begrunnelse,
    timestamp = timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandler = Saksbehandler(
        navn = saksbehandlerNavn,
        ident = saksbehandlerIdent
    ),
    inntekt = Inntektoverstyring.OverstyrtInntekt(
        forklaring = forklaring,
        manedligInntekt = månedligInntekt,
        fraManedligInntekt = fraMånedligInntekt,
        skjaeringstidspunkt = skjæringstidspunkt.format(DateTimeFormatter.ISO_DATE)
    )
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
    forklaring = forklaring
)
