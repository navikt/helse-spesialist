package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringDto
import no.nav.helse.overstyring.OverstyringInntektDto
import java.time.format.DateTimeFormatter

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
    val dager: List<Dag>,
) : Overstyring {
    data class Dag(
        val dato: LocalDate,
        val type: Dagtype,
        val grad: Int?
    )
}

data class Inntektoverstyring(
    override val hendelseId: UUID,
    override val begrunnelse: String,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    val inntekt: Inntekt,
) : Overstyring {
    data class Inntekt(
        val forklaring: String,
        val manedligInntekt: Double,
        val skjaeringstidspunkt: LocalDateTime
    )
}

data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>,
    private val fødselsnummer: String,
    private val generasjoner: List<GraphQLGenerasjon>,
    private val overstyringApiDao: OverstyringApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao
) {
    fun generasjoner(): List<Generasjon> = generasjoner.map { generasjon ->
        Generasjon(
            id = generasjon.id,
            perioder = generasjon.perioder.map {
                when (it) {
                    is GraphQLUberegnetPeriode -> UberegnetPeriode(id = it.id, periode = it)
                    is GraphQLBeregnetPeriode -> BeregnetPeriode(id = it.id, periode = it)
                    else -> throw Exception("Ukjent tidslinjeperiode")
                }
            }
        )
    }

    fun overstyringer(): List<Overstyring> =
        overstyringApiDao.finnOverstyringerAvTidslinjer(fødselsnummer, organisasjonsnummer)
            .map { it.tilDagoverstyring() } +
            overstyringApiDao.finnOverstyringerAvInntekt(fødselsnummer, organisasjonsnummer)
                .map { it.tilInntektoverstyring() }

    fun arbeidsforhold(): List<Arbeidsforhold> = arbeidsgiverApiDao.finnArbeidsforhold(fødselsnummer, organisasjonsnummer).map {
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
        Dagoverstyring.Dag(
            dato = dag.dato.format(DateTimeFormatter.ISO_DATE),
            type = dag.type,
            grad = dag.grad
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
    inntekt = Inntektoverstyring.Inntekt(
        forklaring = forklaring,
        manedligInntekt = månedligInntekt,
        skjaeringstidspunkt = skjæringstidspunkt.format(DateTimeFormatter.ISO_DATE_TIME)
    )
)
