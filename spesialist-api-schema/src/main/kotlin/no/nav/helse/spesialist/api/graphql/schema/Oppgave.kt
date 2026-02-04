@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Oppgavetype")
enum class ApiOppgavetype {
    SOKNAD,
    STIKKPROVE,
    RISK_QA,
    REVURDERING,
    FORTROLIG_ADRESSE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
}

@GraphQLName("AntallArbeidsforhold")
enum class ApiAntallArbeidsforhold {
    ET_ARBEIDSFORHOLD,
    FLERE_ARBEIDSFORHOLD,
}

@GraphQLName("Egenskap")
enum class ApiEgenskap {
    RISK_QA,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER,
    SPESIALSAK,
    REVURDERING,
    SOKNAD,
    STIKKPROVE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    UTLAND,
    HASTER,
    RETUR,
    VERGEMAL,
    SKJONNSFASTSETTELSE,
    PA_VENT,
    TILBAKEDATERT,
    GOSYS,
    MANGLER_IM,
    MEDLEMSKAP,
    GRUNNBELOPSREGULERING,
    SELVSTENDIG_NAERINGSDRIVENDE,
    ARBEIDSTAKER,
    JORDBRUKER_REINDRIFT,
}

@GraphQLName("Kategori")
enum class ApiKategori {
    Mottaker,
    Inntektskilde,
    Inntektsforhold,
    Arbeidssituasjon,
    Oppgavetype,
    Ukategorisert,
    Periodetype,
    Status,
}

@GraphQLName("OppgaveSorteringsfelt")
@Suppress("ktlint:standard:enum-entry-name-case")
enum class ApiOppgaveSorteringsfelt {
    tildeling,
    opprettetTidspunkt,
    paVentInfo_tidsfrist,
    behandlingOpprettetTidspunkt,
}

@GraphQLName("Sorteringsrekkefolge")
enum class ApiSorteringsrekkefølge {
    STIGENDE,
    SYNKENDE,
}

@GraphQLName("BehandledeOppgaver")
data class ApiBehandledeOppgaver(
    val totaltAntallOppgaver: Int,
    val oppgaver: List<ApiBehandletOppgave>,
)

@GraphQLName("AntallOppgaver")
data class ApiAntallOppgaver(
    val antallMineSaker: Int,
    val antallMineSakerPaVent: Int,
)

@GraphQLName("Oppgaveegenskap")
data class ApiOppgaveegenskap(
    val egenskap: ApiEgenskap,
    val kategori: ApiKategori,
)

@GraphQLName("OppgaveForPeriodevisning")
data class ApiOppgaveForPeriodevisning(
    val id: String,
)

@GraphQLName("Totrinnsvurdering")
data class ApiTotrinnsvurdering(
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val erBeslutteroppgave: Boolean,
)

@Serializable
@GraphQLName("Personnavn")
data class ApiPersonnavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)

@GraphQLName("BehandletOppgave")
data class ApiBehandletOppgave(
    val id: String,
    val aktorId: String,
    val personPseudoId: UUID,
    val oppgavetype: ApiOppgavetype,
    val periodetype: ApiPeriodetype,
    val antallArbeidsforhold: ApiAntallArbeidsforhold,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val beslutter: String?,
    val saksbehandler: String?,
    val personnavn: ApiPersonnavn,
)
