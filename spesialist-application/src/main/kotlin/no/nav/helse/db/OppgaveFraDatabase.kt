package no.nav.helse.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val navn: PersonnavnFraDatabase,
    val egenskaper: Set<EgenskapForDatabase>,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false,
    val opprettet: LocalDateTime,
    val opprinneligSøknadsdato: LocalDateTime,
    val tidsfrist: LocalDate?,
    val filtrertAntall: Int,
    val paVentInfo: PaVentInfoFraDatabase?,
)

data class PaVentInfoFraDatabase(
    val årsaker: List<String>,
    val tekst: String?,
    val dialogRef: Long,
    val saksbehandler: String,
    val opprettet: LocalDateTime,
    val tidsfrist: LocalDate,
    val kommentarer: List<KommentarFraDatabase>,
)

data class KommentarFraDatabase(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
)

data class AntallOppgaverFraDatabase(
    val antallMineSaker: Int,
    val antallMineSakerPåVent: Int,
)

data class BehandletOppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val egenskaper: Set<EgenskapForDatabase>,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val navn: PersonnavnFraDatabase,
    val filtrertAntall: Int,
)

data class OppgavesorteringForDatabase(
    val nøkkel: SorteringsnøkkelForDatabase,
    val stigende: Boolean,
)

enum class SorteringsnøkkelForDatabase {
    TILDELT_TIL,
    OPPRETTET,
    SØKNAD_MOTTATT,
    TIDSFRIST,
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
}
