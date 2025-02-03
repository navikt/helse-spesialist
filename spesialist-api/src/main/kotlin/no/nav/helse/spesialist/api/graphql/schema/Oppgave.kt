package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
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
    TILKOMMEN,
}

@GraphQLName("Kategori")
enum class ApiKategori {
    Mottaker,
    Inntektskilde,
    Oppgavetype,
    Ukategorisert,
    Periodetype,
    Status,
}

@GraphQLName("Filtrering")
data class ApiFiltrering(
    val egenskaper: List<ApiOppgaveegenskap> = emptyList(),
    val ekskluderteEgenskaper: List<ApiOppgaveegenskap>? = emptyList(),
    val ingenUkategoriserteEgenskaper: Boolean = false,
    val tildelt: Boolean? = null,
    val egneSakerPaVent: Boolean = false,
    val egneSaker: Boolean = false,
)

@GraphQLName("Oppgavesortering")
data class ApiOppgavesortering(
    val nokkel: ApiSorteringsnokkel,
    val stigende: Boolean,
)

@GraphQLName("Sorteringsnokkel")
enum class ApiSorteringsnokkel {
    TILDELT_TIL,
    OPPRETTET,
    SOKNAD_MOTTATT,
    TIDSFRIST,
}

@GraphQLName("BehandledeOppgaver")
data class ApiBehandledeOppgaver(
    val totaltAntallOppgaver: Int,
    val oppgaver: List<ApiBehandletOppgave>,
)

@GraphQLName("OppgaverTilBehandling")
data class ApiOppgaverTilBehandling(
    val totaltAntallOppgaver: Int,
    val oppgaver: List<ApiOppgaveTilBehandling>,
)

@GraphQLName("AntallOppgaver")
data class ApiAntallOppgaver(
    val antallMineSaker: Int,
    val antallMineSakerPaVent: Int,
)

@GraphQLName("PaVentInfo")
data class ApiPaVentInfo(
    val arsaker: List<String>,
    val tekst: String?,
    val dialogRef: Int,
    val saksbehandler: String,
    val opprettet: LocalDateTime,
    val tidsfrist: LocalDate,
    val kommentarer: List<Kommentar>,
)

@GraphQLName("OppgaveTilBehandling")
data class ApiOppgaveTilBehandling(
    val id: String,
    val opprettet: LocalDateTime,
    val opprinneligSoknadsdato: LocalDateTime,
    val tidsfrist: LocalDate?,
    val vedtaksperiodeId: UUID,
    val navn: ApiPersonnavn,
    val aktorId: String,
    val tildeling: ApiTildeling?,
    val egenskaper: List<ApiOppgaveegenskap>,
    val periodetype: Periodetype,
    val oppgavetype: ApiOppgavetype,
    val mottaker: ApiMottaker,
    val antallArbeidsforhold: ApiAntallArbeidsforhold,
    val paVentInfo: ApiPaVentInfo?,
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

@GraphQLName("Mottaker")
enum class ApiMottaker {
    SYKMELDT,
    ARBEIDSGIVER,
    BEGGE,
    INGEN,
}

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
    val oppgavetype: ApiOppgavetype,
    val periodetype: Periodetype,
    val antallArbeidsforhold: ApiAntallArbeidsforhold,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val personnavn: ApiPersonnavn,
)
