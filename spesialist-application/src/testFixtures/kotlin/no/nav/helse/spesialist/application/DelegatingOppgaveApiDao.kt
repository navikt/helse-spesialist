package no.nav.helse.spesialist.application

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import java.util.UUID

class DelegatingOppgaveApiDao : OppgaveApiDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        TODO("Not yet implemented")
    }

    override fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? {
        TODO("Not yet implemented")
    }

}
