package no.nav.helse.db

import java.util.UUID

interface OppgaveDao {
    fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long

    fun finnBehandlingId(oppgaveId: Long): UUID

    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnOppgaveId(utbetalingId: UUID): Long?

    fun finnVedtaksperiodeId(oppgaveId: Long): UUID

    fun reserverNesteId(): Long

    fun finnSpleisBehandlingId(oppgaveId: Long): UUID

    fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>?

    fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long?

    fun finnFødselsnummer(oppgaveId: Long): String

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean
}
