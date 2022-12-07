package no.nav.helse.spesialist.api.graphql.schema

import java.time.format.DateTimeFormatter
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vedtaksperiode.VarselDao

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

interface Overstyring {
    val hendelseId: UUIDString
    val begrunnelse: String
    val timestamp: DateTimeString
    val saksbehandler: Saksbehandler
}

data class Dagoverstyring(
    override val hendelseId: UUIDString,
    override val begrunnelse: String,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    val dager: List<OverstyrtDag>,
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
    override val begrunnelse: String,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    val inntekt: OverstyrtInntekt,
) : Overstyring {
    data class OverstyrtInntekt(
        val forklaring: String,
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

data class Arbeidsforholdoverstyring(
    override val hendelseId: UUIDString,
    override val begrunnelse: String,
    override val timestamp: DateTimeString,
    override val saksbehandler: Saksbehandler,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: DateString,
    val forklaring: String,
) : Overstyring

data class GhostPeriode(
    val id: UUIDString,
    val fom: DateString,
    val tom: DateString,
    val skjaeringstidspunkt: DateString,
    val vilkarsgrunnlagId: UUIDString?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String,
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
    private val varselRepository: ApiVarselRepository,
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
                        orgnummer = organisasjonsnummer,
                        periode = it,
                        risikovurderingApiDao = risikovurderingApiDao,
                        varselDao = varselDao,
                        varselRepository = varselRepository,
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
