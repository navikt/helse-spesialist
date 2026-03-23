package no.nav.helse.db.api

import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import java.util.UUID

interface OppgaveApiDao {
    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto?
}
