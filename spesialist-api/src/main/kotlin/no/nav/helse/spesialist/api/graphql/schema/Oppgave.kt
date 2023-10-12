package no.nav.helse.spesialist.api.graphql.schema

enum class Oppgavetype {
    SOKNAD,
    STIKKPROVE,
    RISK_QA,
    REVURDERING,
    FORTROLIG_ADRESSE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING
}

enum class AntallArbeidsforhold {
    ET_ARBEIDSFORHOLD,
    FLERE_ARBEIDSFORHOLD
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
    FULLMAKT,
    VERGEMAL
}

enum class Kategori {
    Mottaker,
    Inntektskilde,
    Oppgavetype,
    Ukategorisert,
    Periodetype
}

data class Oppgavesortering(val nokkel: Sorteringsnokkel, val stigende: Boolean)

enum class Sorteringsnokkel {
    TILDELT_TIL,
    OPPRETTET,
    SOKNAD_MOTTATT
}

data class OppgaveTilBehandling(
    val id: String,
    val opprettet: DateTimeString,
    val opprinneligSoknadsdato: DateTimeString,
    val vedtaksperiodeId: UUIDString,
    val navn: Personnavn,
    val aktorId: String,
    val tildeling: Tildeling?,
    val egenskaper: List<Oppgaveegenskap>,
    val oppgaveegenskaper: Oppgaveegenskaper,
    val periodetype: Periodetype,
    val oppgavetype: Oppgavetype,
    val mottaker: Mottaker,
    val antallArbeidsforhold: AntallArbeidsforhold,
)

data class Oppgaveegenskaper(
    val egenskaper: List<Oppgaveegenskap>
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
    val saksbehandler: UUIDString?,
    val beslutter: UUIDString?,
    val erBeslutteroppgave: Boolean
)

enum class Mottaker {
    SYKMELDT,
    ARBEIDSGIVER,
    BEGGE,
    INGEN
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
    val ferdigstiltTidspunkt: DateTimeString,
    val ferdigstiltAv: String?,
    val personnavn: Personnavn,
)
