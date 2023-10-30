package no.nav.helse.db

import java.time.LocalDateTime
import java.util.UUID

data class OppgaveFraDatabase(
    val id: Long,
    val egenskap: String,
    val egenskaper: List<EgenskapForDatabase>,
    val status: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val kanAvvises: Boolean,
    val ferdigstiltAvIdent: String? = null,
    val ferdigstiltAvOid: UUID? = null,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false,
)

data class OppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val navn: PersonnavnFraDatabase,
    val egenskaper: List<EgenskapForDatabase>,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false,
    val opprettet: LocalDateTime,
    val opprinneligSøknadsdato: LocalDateTime,
    val filtrertAntall: Int,
)

data class AntallOppgaverFraDatabase(
    val antallMineSaker: Int,
    val antallMineSakerPåVent: Int,
)

data class BehandletOppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val egenskaper: List<EgenskapForDatabase>,
    val ferdigstiltTidspunkt: LocalDateTime,
    val ferdigstiltAv: String?,
    val navn: PersonnavnFraDatabase,
)

data class OppgavesorteringForDatabase(val nøkkel: SorteringsnøkkelForDatabase, val stigende: Boolean)

enum class SorteringsnøkkelForDatabase {
    TILDELT_TIL,
    OPPRETTET,
    SØKNAD_MOTTATT
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
    FULLMAKT,
    VERGEMÅL,
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    UTLAND,
    FORLENGELSE,
    FORSTEGANGSBEHANDLING,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    SKJØNNSFASTSETTELSE
}