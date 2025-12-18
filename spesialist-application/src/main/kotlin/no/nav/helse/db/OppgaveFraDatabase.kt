package no.nav.helse.db

import java.time.LocalDateTime

data class AntallOppgaverFraDatabase(
    val antallMineSaker: Int,
    val antallMineSakerPåVent: Int,
)

data class BehandletOppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val fødselsnummer: String,
    val egenskaper: Set<EgenskapForDatabase>,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val navn: PersonnavnFraDatabase,
    val filtrertAntall: Int,
)

enum class SorteringsnøkkelForDatabase {
    TILDELT_TIL,
    OPPRETTET,
    SØKNAD_MOTTATT,
    TIDSFRIST,
    BEHANDLING_OPPRETTET_TIDSPUNKT,
}

enum class Sorteringsrekkefølge {
    STIGENDE,
    SYNKENDE,
}

enum class EgenskapForDatabase {
    RISK_QA,
    FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_ADRESSE,
    EGEN_ANSATT,
    BESLUTTER,
    SPESIALSAK,
    REVURDERING,
    SØKNAD,
    STIKKPRØVE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON,
    UTBETALING_TIL_ARBEIDSGIVER,
    INGEN_UTBETALING,
    HASTER,
    RETUR,
    VERGEMÅL,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    UTLAND,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    SKJØNNSFASTSETTELSE,
    PÅ_VENT,
    TILBAKEDATERT,
    GOSYS,
    MANGLER_IM,
    MEDLEMSKAP,

    /** Gammel egenskap fra tidligere iterasjon av tilkommen inntekt, skal overses */
    TILKOMMEN,
    GRUNNBELØPSREGULERING,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    ARBEIDSTAKER,
    JORDBRUKER_REINDRIFT,
}
