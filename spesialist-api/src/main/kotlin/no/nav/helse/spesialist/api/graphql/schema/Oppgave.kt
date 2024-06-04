package no.nav.helse.spesialist.api.graphql.schema

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Oppgavetype {
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

enum class AntallArbeidsforhold {
    ET_ARBEIDSFORHOLD,
    FLERE_ARBEIDSFORHOLD,
}

enum class Egenskap {
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
}

enum class Kategori {
    Mottaker,
    Inntektskilde,
    Oppgavetype,
    Ukategorisert,
    Periodetype,
    Status,
}

data class Filtrering(
    val egenskaper: List<Oppgaveegenskap> = emptyList(),
    val ekskluderteEgenskaper: List<Oppgaveegenskap>? = emptyList(),
    val ingenUkategoriserteEgenskaper: Boolean = false,
    val tildelt: Boolean? = null,
    val egneSakerPaVent: Boolean = false,
    val egneSaker: Boolean = false,
)

data class Oppgavesortering(val nokkel: Sorteringsnokkel, val stigende: Boolean)

enum class Sorteringsnokkel {
    TILDELT_TIL,
    OPPRETTET,
    SOKNAD_MOTTATT,
    TIDSFRIST,
}

data class BehandledeOppgaver(
    val totaltAntallOppgaver: Int,
    val oppgaver: List<BehandletOppgave>,
)

data class OppgaverTilBehandling(
    val totaltAntallOppgaver: Int,
    val oppgaver: List<OppgaveTilBehandling>,
)

data class AntallOppgaver(
    val antallMineSaker: Int,
    val antallMineSakerPaVent: Int,
)

data class OppgaveTilBehandling(
    val id: String,
    val opprettet: LocalDateTime,
    val opprinneligSoknadsdato: LocalDateTime,
    val tidsfrist: LocalDate?,
    val vedtaksperiodeId: UUID,
    val navn: Personnavn,
    val aktorId: String,
    val tildeling: Tildeling?,
    val egenskaper: List<Oppgaveegenskap>,
    val periodetype: Periodetype,
    val oppgavetype: Oppgavetype,
    val mottaker: Mottaker,
    val antallArbeidsforhold: AntallArbeidsforhold,
)

data class Oppgaveegenskap(
    val egenskap: Egenskap,
    val kategori: Kategori,
)

data class OppgaveForPeriodevisning(
    val id: String,
)

data class Totrinnsvurdering(
    val erRetur: Boolean,
    val saksbehandler: UUID?,
    val beslutter: UUID?,
    val erBeslutteroppgave: Boolean,
)

enum class Mottaker {
    SYKMELDT,
    ARBEIDSGIVER,
    BEGGE,
    INGEN,
}

data class Personnavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)

data class BehandletOppgave(
    val id: String,
    val aktorId: String,
    val oppgavetype: Oppgavetype,
    val periodetype: Periodetype,
    val antallArbeidsforhold: AntallArbeidsforhold,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val personnavn: Personnavn,
)
