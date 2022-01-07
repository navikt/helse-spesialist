package no.nav.helse.mediator.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.enums.GraphQLBehandlingstype
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.hentsnapshot.*

enum class Behandlingstype { BEHANDLET, UBEREGNET, VENTER }

enum class Inntektstype { ENARBEIDSGIVER, FLEREARBEIDSGIVERE }

enum class Periodetype {
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT
}

data class Vurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: LocalDateTime
)

data class Utbetaling(
    val arbeidsgiverFagsystemId: String,
    val arbeidsgiverNettoBelop: Int,
    val personFagsystemId: String,
    val personNettoBelop: Int,
    val status: String,
    val type: String,
    val vurdering: Vurdering?
)

data class Periodevilkar(
    val alder: Alder,
    val soknadsfrist: Soknadsfrist?,
    val sykepengedager: Sykepengedager
)

data class Aktivitet(
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String,
    val vedtaksperiodeId: UUID
)

interface Periode {
    fun behandlingstype(): Behandlingstype
    fun erForkastet(): Boolean
    fun fom(): LocalDate
    fun tom(): LocalDate
    fun inntektstype(): Inntektstype
    fun opprettet(): LocalDateTime
    fun periodetype(): Periodetype
    fun tidslinje(): List<String>

    @GraphQLIgnore
    fun behandlingstype(periode: GraphQLTidslinjeperiode): Behandlingstype = when (periode.behandlingstype) {
        GraphQLBehandlingstype.BEHANDLET -> Behandlingstype.BEHANDLET
        GraphQLBehandlingstype.UBEREGNET -> Behandlingstype.UBEREGNET
        GraphQLBehandlingstype.VENTER -> Behandlingstype.VENTER
        else -> throw Exception("Ukjent behandlingstype ${periode.behandlingstype}")
    }

    @GraphQLIgnore
    fun erForkastet(periode: GraphQLTidslinjeperiode): Boolean = periode.erForkastet

    @GraphQLIgnore
    fun fom(periode: GraphQLTidslinjeperiode): LocalDate = periode.fom

    @GraphQLIgnore
    fun tom(periode: GraphQLTidslinjeperiode): LocalDate = periode.tom

    @GraphQLIgnore
    fun inntektstype(periode: GraphQLTidslinjeperiode): Inntektstype = when (periode.inntektstype) {
        GraphQLInntektstype.ENARBEIDSGIVER -> Inntektstype.ENARBEIDSGIVER
        GraphQLInntektstype.FLEREARBEIDSGIVERE -> Inntektstype.FLEREARBEIDSGIVERE
        else -> throw Exception("Ukjent inntektstype ${periode.inntektstype}")
    }

    @GraphQLIgnore
    fun opprettet(periode: GraphQLTidslinjeperiode): LocalDateTime = periode.opprettet

    @GraphQLIgnore
    fun periodetype(periode: GraphQLTidslinjeperiode): Periodetype = when (periode.periodetype) {
        GraphQLPeriodetype.FORLENGELSE -> Periodetype.FORLENGELSE
        GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> Periodetype.FORSTEGANGSBEHANDLING
        GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> Periodetype.INFOTRYGDFORLENGELSE
        GraphQLPeriodetype.OVERGANGFRAIT -> Periodetype.OVERGANG_FRA_IT
        else -> throw Exception("Ukjent periodetype ${periode.periodetype}")
    }

    @GraphQLIgnore
    fun tidslinje(periode: GraphQLTidslinjeperiode): List<String> = emptyList()
}

data class UberegnetPeriode(
    val id: UUID,
    private val periode: GraphQLTidslinjeperiode
) : Periode {
    override fun behandlingstype(): Behandlingstype = behandlingstype(periode)
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): LocalDate = fom(periode)
    override fun tom(): LocalDate = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): LocalDateTime = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<String> = tidslinje(periode)
}


data class BeregnetPeriode(
    val id: UUID,
    private val periode: GraphQLBeregnetPeriode
) : Periode {
    override fun behandlingstype(): Behandlingstype = behandlingstype(periode)
    override fun erForkastet(): Boolean = erForkastet(periode)
    override fun fom(): LocalDate = fom(periode)
    override fun tom(): LocalDate = tom(periode)
    override fun inntektstype(): Inntektstype = inntektstype(periode)
    override fun opprettet(): LocalDateTime = opprettet(periode)
    override fun periodetype(): Periodetype = periodetype(periode)
    override fun tidslinje(): List<String> = tidslinje(periode)

    fun aktivitetslogg(): List<Aktivitet> = periode.aktivitetslogg.map {
        Aktivitet(
            alvorlighetsgrad = it.alvorlighetsgrad,
            melding = it.melding,
            tidsstempel = it.tidsstempel,
            vedtaksperiodeId = it.vedtaksperiodeId
        )
    }

    fun beregningId(): UUID = periode.beregningId

    fun forbrukteSykedager(): Int? = periode.forbrukteSykedager

    fun gjenstaendeSykedager(): Int? = periode.gjenstaendeSykedager

    fun hendelser(): List<Hendelse> = periode.hendelser.map { it.tilHendelse() }

    fun maksdato(): LocalDate = periode.maksdato

    fun periodevilkar(): Periodevilkar = Periodevilkar(
        alder = periode.periodevilkar.alder,
        soknadsfrist = periode.periodevilkar.soknadsfrist,
        sykepengedager = periode.periodevilkar.sykepengedager
    )

    fun skjaeringstidspunkt(): LocalDate = periode.skjaeringstidspunkt

    fun utbetaling(): Utbetaling = periode.utbetaling.let {
        Utbetaling(
            arbeidsgiverFagsystemId = it.arbeidsgiverFagsystemId,
            arbeidsgiverNettoBelop = it.arbeidsgiverNettoBelop,
            personFagsystemId = it.personFagsystemId,
            personNettoBelop = it.personNettoBelop,
            status = it.status,
            type = it.type,
            vurdering = it.vurdering?.let { vurdering ->
                Vurdering(
                    automatisk = vurdering.automatisk,
                    godkjent = vurdering.godkjent,
                    ident = vurdering.ident,
                    tidsstempel = vurdering.tidsstempel
                )
            }
        )
    }

    fun vilkarsgrunnlaghistorikkId(): UUID = periode.vilkarsgrunnlaghistorikkId
}
