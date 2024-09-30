package no.nav.helse.db

import java.util.UUID

interface OppgaveRepository {
    fun finnOppgave(id: Long): OppgaveFraDatabase?

    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnVedtaksperiodeId(fødselsnummer: String): UUID

    fun harGyldigOppgave(utbetalingId: UUID): Boolean

    fun finnHendelseId(id: Long): UUID

    fun invaliderOppgaveFor(fødselsnummer: String)
}
