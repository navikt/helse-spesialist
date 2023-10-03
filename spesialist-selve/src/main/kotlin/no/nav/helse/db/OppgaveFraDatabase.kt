package no.nav.helse.db

import java.time.LocalDateTime
import java.util.UUID

data class OppgaveFraDatabase(
    val id: Long,
    val egenskap: String,
    val egenskaper: List<String>,
    val status: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val ferdigstiltAvIdent: String? = null,
    val ferdigstiltAvOid: UUID? = null,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false
)

data class OppgaveFraDatabaseForVisning(
    val id: Long,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val navn: PersonnavnFraDatabase,
    val egenskaper: List<String>,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false,
    val opprettet: LocalDateTime,
    val opprinneligSøknadsdato: LocalDateTime,
)